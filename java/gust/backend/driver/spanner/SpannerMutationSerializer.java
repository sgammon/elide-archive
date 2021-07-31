/*
 * Copyright © 2020, The Gust Framework Authors. All rights reserved.
 *
 * The Gust/Elide framework and tools, and all associated source or object computer code, except where otherwise noted,
 * are licensed under the Zero Prosperity license, which is enclosed in this repository, in the file LICENSE.txt. Use of
 * this code in object or source form requires and implies consent and agreement to that license in principle and
 * practice. Source or object code not listing this header, or unless specified otherwise, remain the property of
 * Elide LLC and its suppliers, if any. The intellectual and technical concepts contained herein are proprietary to
 * Elide LLC and its suppliers and may be covered by U.S. and Foreign Patents, or patents in process, and are protected
 * by trade secret and copyright law. Dissemination of this information, or reproduction of this material, in any form,
 * is strictly forbidden except in adherence with assigned license requirements.
 */
package gust.backend.driver.spanner;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import gust.backend.model.ModelDeflateException;
import gust.backend.model.ModelSerializer;
import gust.backend.runtime.Logging;
import org.slf4j.Logger;
import tools.elide.core.FieldType;
import tools.elide.core.SpannerFieldOptions;
import tools.elide.core.SpannerOptions;
import tools.elide.core.TableFieldOptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static gust.backend.driver.spanner.SpannerUtil.*;
import static gust.backend.model.ModelMetadata.*;


/**
 * Implements a specialized serializer, capable of converting generated {@link Message}-derived objects into Spanner
 * {@link Mutation} records during write operations.
 *
 * @see SpannerStructDeserializer For an equivalent specialized de-serializer, working atop Spanner {@link Struct}s.
 * @param <Model> Typed {@link Message} which implements a concrete model object structure, as defined and annotated by
 *                the core Gust annotations.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class SpannerMutationSerializer<Model extends Message> implements ModelSerializer<Model, Mutation> {
    private static final Logger logging = Logging.logger(SpannerMutationSerializer.class);
    private static final JsonFormat.Printer defaultJsonPrinter = JsonFormat
            .printer()
            .includingDefaultValueFields()
            .omittingInsignificantWhitespace()
            .sortingMapKeys();

    /** Model structure for the backing instance. */
    private final @Nonnull Descriptors.Descriptor model;

    /** Settings for the Spanner driver itself. */
    private final @Nonnull SpannerDriverSettings driverSettings;

    /** Target mutation we intend to initialize. */
    private volatile @Nullable Mutation.WriteBuilder target = null;

    /**
     * Private constructor.
     *
     * @param instance Default instance for the model we intend to serialize.
     * @param driverSettings Settings for the Spanner driver itself.
     */
    SpannerMutationSerializer(@Nonnull Model instance,
                              @Nonnull SpannerDriverSettings driverSettings) {
        this.driverSettings = driverSettings;
        this.model = instance.getDescriptorForType();
    }

    /**
     * Initialize a new mutation cycle for the provided mutation builder object. We hold this object and fill it in when
     * the next serialization call occurs. After serialization, the value is cleared for later use.
     *
     * @param initial In-progress mutation to initialize.
     */
    @Nonnull SpannerMutationSerializer<Model> initializeMutation(@Nonnull Mutation.WriteBuilder initial) {
        target = initial;
        return this;
    }

    /**
     * Inject a model instance's key ID at the expected place in a given Spanner {@link Mutation} target, given any
     * annotations present on the key model field.
     *
     * <p>Keys in Gust are implemented as objects. Typically these keys have a property present on them which is itself
     * annotated as an ID property. This method makes sure that we collapse that ID into the key's column in the table,
     * which should store a native value (i.e. an ID string or number) rather than an encoded message or sub-collection
     * entry, which is the norm for sub-messages outside of special cases like keys, timestamps, and dates.</p>
     *
     * @param keyField Pointer to the key field on the model.
     * @param instance Message instance where we should pluck the key/ID from.
     * @param target Mutation target where we should write the resulting ID value.
     * @throws IllegalStateException For invalid key types. Only `STRING` and `INT64` are supported as column types in
     *         Spanner for primary keys.
     */
    @VisibleForTesting
    void collapseRowKey(@Nonnull FieldPointer keyField,
                        @Nonnull Message instance,
                        @Nonnull Mutation.WriteBuilder target) {
        var idField = idField(instance).orElseThrow();
        var id = id(instance).orElseThrow();
        var column = resolveKeyColumn(idField, driverSettings);
        var valueBinder = target.set(column);
        var type = resolveKeyType(idField);

        if (type.getCode() == Type.Code.STRING) {
            valueBinder.to((String)id);
        } else if (type.getCode() == Type.Code.INT64) {
            valueBinder.to((Long)id);
        } else {
            throw new IllegalStateException(
                String.format("Unsupported key field type: '%s'.", keyField.getField().getType().name()));
        }
    }

    @SuppressWarnings("unchecked")
    <Primitive> void bindValueTyped(@Nonnull Descriptors.FieldDescriptor field,
                                    @Nonnull ValueBinder<Primitive> valueBinder,
                                    @Nonnull Type columnType,
                                    @Nonnull Object rawValue,
                                    @Nonnull Optional<SpannerFieldOptions> spannerOpts,
                                    @Nonnull Optional<TableFieldOptions> columnOpts) {
        boolean repeated = columnType.getCode() == Type.Code.ARRAY;
        Objects.requireNonNull(rawValue, "should never get `NULL` for present protocol buffer value");

        Type innerType = repeated ?
                columnType.getArrayElementType() :
                columnType;

        switch (innerType.getCode()) {
            case BOOL:
                if (repeated) {
                    valueBinder.toBoolArray((Iterable<Boolean>)rawValue);
                } else {
                    valueBinder.to((Boolean)rawValue);
                }
                break;

            case INT64:
                // we need to check if it's an ENUM, and if ENUMs-as-strings is off. if both of these conditions
                // match, we need to resolve the enumerated instance and assign that instead.
                if (field.getType() == Descriptors.FieldDescriptor.Type.ENUM &&
                    driverSettings.enumsAsNumbers()) {
                    var descriptor = (Descriptors.EnumValueDescriptor)rawValue;
                    valueBinder.to(descriptor.getNumber());

                } else {
                    // otherwise, we should treat it as a native numeric type, repeated or singular.
                    if (repeated) {
                        valueBinder.toInt64Array((Iterable<Long>) rawValue);
                    } else {
                        if (rawValue instanceof Long) {
                            valueBinder.to((Long) rawValue);
                        } else {
                            valueBinder.to((Integer) rawValue);
                        }
                    }
                }
                break;

            case FLOAT64:
                if (repeated) {
                    valueBinder.toFloat64Array((Iterable<Double>)rawValue);
                } else {
                    valueBinder.to((Double)rawValue);
                }
                break;

            case STRING:
                // special case: JSON fields
                if (field.getType() == Descriptors.FieldDescriptor.Type.MESSAGE &&
                    (spannerOpts.isPresent() && spannerOpts.get().getType() == SpannerOptions.SpannerType.JSON) ||
                    (columnOpts.isPresent() && columnOpts.get().getSptype() == SpannerOptions.SpannerType.JSON)) {
                    // it's a repeated JSON field, so serialize it as an array of sub-messages instead of strings.
                    try {
                        if (repeated) {
                            var arr = new LinkedList<String>();
                            for (var encodableModel : (Iterable<Message>) rawValue) {
                                arr.add(defaultJsonPrinter.print(encodableModel));
                            }
                            valueBinder.toStringArray(arr);
                        } else {
                            // it's a singular JSON field, so serialize it as a sub-message instead of a string.
                            valueBinder.to(defaultJsonPrinter.print((Message)rawValue));
                        }
                    } catch (InvalidProtocolBufferException ipbe) {
                        logging.error("!! Invalid protocol buffer for JSON encoding.", ipbe);
                        throw new IllegalStateException(ipbe);
                    }
                } else {
                    // we need to check if it's an ENUM, and if ENUMs-as-strings is on. if both of these conditions
                    // match, we need to resolve the enumerated instance and assign that instead.
                    if (field.getType() == Descriptors.FieldDescriptor.Type.ENUM &&
                        !driverSettings.enumsAsNumbers()) {
                        var descriptor = (Descriptors.EnumValueDescriptor)rawValue;
                        valueBinder.to(descriptor.getName());

                    } else {
                        // it's not a JSON field or an enum, or enum serialization as strings isn't on, so serialize it
                        // as a regular string value (either singular or repeated).
                        if (repeated) {
                            valueBinder.toStringArray((Iterable<String>)rawValue);
                        } else {
                            valueBinder.to((String)rawValue);
                        }
                    }
                }
                break;

            case BYTES:
                if (repeated) {
                    var arr = new LinkedList<ByteArray>();
                    for (var bytes : (Iterable<ByteString>) rawValue) {
                        arr.add(ByteArray.copyFrom(bytes.asReadOnlyByteBuffer()));
                    }
                    valueBinder.toBytesArray(arr);
                } else {
                    valueBinder.to(ByteArray.copyFrom(((ByteString)rawValue).asReadOnlyByteBuffer()));
                }
                break;

            case STRUCT:
                throw new IllegalArgumentException(String.format(
                    "STRUCT types are expressions and are not valid for storage. Please use either a `JSON` field or " +
                    "valid sub-collection binding, at field path '%s'.",
                    field.getFullName()
                ));

            case TIMESTAMP:
                if (com.google.protobuf.Timestamp.getDescriptor()
                        .getFullName()
                        .equals(field.getMessageType().getFullName())) {
                    if (repeated) {
                        var arr = new LinkedList<Timestamp>();
                        for (var ts : (Iterable<com.google.protobuf.Timestamp>) rawValue) {
                            arr.add(SpannerTemporalConverter.cloudTimestampFromProto(ts));
                        }
                        valueBinder.toTimestampArray(arr);
                    } else {
                        valueBinder.to(SpannerTemporalConverter.cloudTimestampFromProto(
                                ((com.google.protobuf.Timestamp)rawValue)));
                    }
                    break;
                }

                // any sub-message type other than `Timestamp` is invalid.
                throw new IllegalStateException(
                        "Type 'TIMESTAMP' is not yet supported for use with record " +
                        "'" + field.getMessageType().getFullName() + "'.");

            case DATE:
                if (com.google.type.Date.getDescriptor()
                    .getFullName()
                    .equals(field.getMessageType().getFullName())) {
                    if (repeated) {
                        var arr = new LinkedList<Date>();
                        for (var date : (Iterable<com.google.type.Date>) rawValue) {
                            arr.add(SpannerTemporalConverter.cloudDateFromProto(date));
                        }
                        valueBinder.toDateArray(arr);
                    } else {
                        valueBinder.to(SpannerTemporalConverter.cloudDateFromProto(
                                ((com.google.type.Date)rawValue)));
                    }
                    break;
                }

                // any sub-message type other than `Date` is invalid.
                throw new IllegalStateException(
                        "Type 'DATE' is not yet supported for use with record " +
                        "'" + field.getMessageType().getFullName() + "'.");

            case NUMERIC:
                // @TODO(sgammon): support for 'NUMERIC' complex types
                throw new IllegalStateException("Type 'NUMERIC' is not yet supported for use with Spanner.");

            case ARRAY: throw new IllegalStateException("Illegal array received for flattened serialization.");
        }
    }

    /**
     * Collapse an individual {@link Message} field into the expected column slow against a given Spanner
     * {@link Mutation} record, which is in the process of being assembled.
     *
     * <p>Each individual model field which is eligible for storage in Spanner must be resolvable to a valid column name
     * and type. This process is usually conducted via model annotations, with sensible defaults built in. Before a
     * value can be duly bound, this method resolves such ancillary values and feeds them into
     * {@link #bindValueTyped(Descriptors.FieldDescriptor, ValueBinder, Type, Object, Optional, Optional)}, where the
     * binding itself takes place.</p>
     *
     * @see #bindValueTyped(Descriptors.FieldDescriptor, ValueBinder, Type, Object, Optional, Optional) Inner post-check
     *      typed value injection.
     * @param instance Model instance we should pluck the field value from.
     * @param fieldPointer Resolved pointer to the model field we are collapsing into a column value.
     * @param target Mutation target we should write the resulting value to, as applicable.
     */
    @VisibleForTesting
    void collapseColumnField(@Nonnull Model instance,
                             @Nonnull FieldPointer fieldPointer,
                             @Nonnull Mutation.WriteBuilder target) {
        var field = fieldPointer.getField();
        var fieldValue = pluck(instance, fieldPointer.getName());

        if (!field.isRepeated() && !instance.hasField(field) || fieldValue.getValue().isEmpty() ||
            field.isRepeated() && instance.getRepeatedFieldCount(field) < 1) {
            // field has no value. skip, but log about it.
            if (logging.isTraceEnabled())
                logging.trace(
                    "Field '{}' on model '{}' had no value. Skipping.",
                    field,
                    model.getFullName()
                );
            return;
        }

        // virtualize the key property, when encountered
        if (!field.isRepeated() && matchFieldAnnotation(field, FieldType.KEY)) {
            this.collapseRowKey(fieldPointer, instance, target);
            return;
        } else if (field.isRepeated() && matchFieldAnnotation(field, FieldType.KEY)) {
            throw new IllegalStateException(
                "Cannot make `KEY` field repeated (on model '" + field.getMessageType().getFullName() + "'."
            );
        }

        // resolve any column or spanner options
        var columnOpts = columnOpts(fieldPointer);
        var spannerOpts = spannerOpts(fieldPointer);

        // resolve column name...
        var columnName = resolveColumnName(
            fieldPointer,
            spannerOpts,
            columnOpts,
            driverSettings
        );

        // then type...
        var columnType = resolveColumnType(
            fieldPointer,
            spannerOpts,
            columnOpts,
            driverSettings
        );

        // then raw value...
        var valueBinder = target.set(columnName);
        var rawValue = fieldValue.getValue().orElseThrow();

        bindValueTyped(
            field,
            valueBinder,
            columnType,
            rawValue,
            spannerOpts,
            columnOpts
        );
    }

    /**
     * Construct a {@link Mutation} serializer for the provided <b>instance</b>.
     *
     * @param instance Model instance to acquire a mutation serializer for.
     * @param driverSettings Settings for the Spanner driver itself.
     * @param <M> Model type to deserialize.
     * @return Snapshot deserializer instance.
     */
    @SuppressWarnings("SameParameterValue")
    static <M extends Message> SpannerMutationSerializer<M> forModel(@Nonnull M instance,
                                                                     @Nonnull SpannerDriverSettings driverSettings) {
        return new SpannerMutationSerializer<>(
            instance,
            driverSettings
        );
    }

    /** @inheritDoc */
    @Override
    public @Nonnull Mutation deflate(@Nonnull Model input) throws ModelDeflateException {
        // `initializeMutation` must be called before `deflate`. it is force-emptied each cycle to prevent
        var writeBuilder = this.target;
        Objects.requireNonNull(input, "cannot deflate `null` input for Spanner mutation");
        Objects.requireNonNull(writeBuilder, "cannot deflate model with no initialized write target.");

        // stream all non-recursive model fields, filtering down only to fields which are eligible for storage in
        // Spanner. for each field, invoke `collapseColumnField`, which mutates the held builder in-place.
        forEachField(
          model,
          Optional.of(onlySpannerEligibleFields(driverSettings))
        ).forEach((field) ->
            this.collapseColumnField(input, field, writeBuilder)
        );

        final Mutation mutation = writeBuilder.build();
        this.target = null;
        return mutation;
    }
}
