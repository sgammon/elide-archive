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

import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Type;
import com.google.cloud.spanner.Value;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.google.type.Date;
import gust.backend.model.ModelDeserializer;
import gust.backend.model.ModelInflateException;
import gust.backend.model.ModelMetadata;
import gust.backend.runtime.Logging;
import org.slf4j.Logger;
import tools.elide.core.FieldType;
import tools.elide.core.SpannerOptions;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static gust.backend.driver.spanner.SpannerTemporalConverter.*;
import static gust.backend.driver.spanner.SpannerUtil.*;
import static gust.backend.model.ModelMetadata.*;

import static java.lang.String.format;


/**
 * Implements a specialized de-serializer, capable of converting runtime-inhabited Spanner {@link Struct}-records into
 * typed {@link Message}-derived objects.
 *
 * @see SpannerMutationSerializer For an equivalent specialized serializer, working atop Spanner {@link Mutation}s.
 * @param <Model> Typed {@link Message} which implements a concrete model object structure, as defined and annotated by
 *                the core Gust annotations.
 */
@Immutable
@ThreadSafe
public final class SpannerStructDeserializer<Model extends Message> implements ModelDeserializer<Struct, Model> {
    private static final Logger logging = Logging.logger(SpannerStructDeserializer.class);
    private static final @Nonnull JsonFormat.Parser defaultJsonParser = JsonFormat.parser()
            .ignoringUnknownFields();

    /** Encapsulated object deserializer. */
    private final Model defaultInstance;

    /** Descriptor for the root model we're decoding. */
    private final Descriptors.Descriptor modelDescriptor;

    /** Settings for the Spanner driver. */
    private final @Nonnull SpannerDriverSettings driverSettings;

    /**
     * Private constructor.
     *
     * @param instance Model instance to deserialize.
     * @param driverSettings Settings for the Spanner driver.
     */
    private SpannerStructDeserializer(@Nonnull Model instance,
                                      @Nonnull SpannerDriverSettings driverSettings) {
        this.defaultInstance = instance;
        this.driverSettings = driverSettings;
        this.modelDescriptor = instance.getDescriptorForType();
    }

    /**
     * Construct a {@link Struct} deserializer for the provided <b>instance</b>.
     *
     * @param instance Model instance to acquire a data deserializer for.
     * @param driverSettings Settings for the Spanner driver itself.
     * @param <M> Model type to deserialize.
     * @return Snapshot deserializer instance.
     */
    @SuppressWarnings("SameParameterValue")
    public static <M extends Message> SpannerStructDeserializer<M> forModel(
            @Nonnull M instance,
            @Nonnull SpannerDriverSettings driverSettings) {
        return new SpannerStructDeserializer<>(
            instance,
            driverSettings
        );
    }

    /**
     * Inflate a field from a Spanner row ID into a message/model Key instance, with the provided value object.
     *
     * <p>The provided field pointer is used to fill in the primary key ID pulled from a given Spanner row result. That
     * ID is spliced into the key record, which is then spliced into the target builder.</p>
     *
     * @param target Target message builder.
     * @param value Resolved value for the ID.
     */
    private void inflateRowKey(@Nonnull Message.Builder target,
                               @Nonnull Value value) {
        Objects.requireNonNull(value, "cannot inflate Spanner key from NULL value");
        var baseInstance = target.getDefaultInstanceForType();
        var keyField = keyField(baseInstance).orElseThrow();
        var idField = idField(baseInstance).orElseThrow();

        var keyBuilder = target.newBuilderForField(keyField.getField());
        var keyType = resolveKeyType(idField);

        // splice the ID into the key
        if (keyType.getCode() == Type.Code.STRING) {
            spliceIdBuilder(keyBuilder, Optional.of(value.getString()));
        } else if (keyType.getCode() == Type.Code.INT64) {
            // special case: stringify if so instructed
            if (idField.getField().getType().equals(Descriptors.FieldDescriptor.Type.STRING)) {
                spliceIdBuilder(keyBuilder, Optional.of(String.valueOf(value.getInt64())));
            } else {
                spliceIdBuilder(keyBuilder, Optional.of(value.getInt64()));
            }
        } else {
            throw new IllegalStateException(format("Unsupported key type: '%s'.", keyType.getCode().name()));
        }

        // splice the key into the model
        target.setField(keyField.getField(), keyBuilder.build());
    }

    /**
     * Resolve a Cloud Spanner column and value for the provided model field, from the source row structure, if one
     * is present.
     *
     * <p>If the column has no value, it is skipped. If the column has a value present, it is decoded according to the
     * Cloud Spanner {@link Type} specified for the column, each of which are mapped to primitive Protocol Buffer
     * object field types.</p>
     *
     * @param target Target protocol buffer builder to fill in.
     * @param source Row result from Spanner to fill from.
     * @param fieldPointer Field we are resolving from the proto.
     */
    private void convergeColumnField(@Nonnull Message.Builder target,
                                     @Nonnull Struct source,
                                     @Nonnull ModelMetadata.FieldPointer fieldPointer) {
        // resolve any generic column options and spanner extension options
        var columnOpts = columnOpts(fieldPointer);
        var spannerOpts = spannerOpts(fieldPointer);
        var columnName = resolveColumnName(fieldPointer.getField(), driverSettings);

        if (source.isNull(columnName)) {
            if (logging.isTraceEnabled())
                logging.trace("Resolved column value for field '{}' was NULL. Skipping.",
                    fieldPointer.getName());
        } else {
            // resolve the expected column index
            var columnValue = resolveColumnValue(
                source,
                fieldPointer,
                spannerOpts,
                columnOpts,
                driverSettings
            );

            // first up, check to see if this is a key or ID field, and decode it properly if so
            var field = fieldPointer.getField();
            if (matchFieldAnnotation(field, FieldType.ID)) {
                this.inflateRowKey(target, columnValue);
                return;
            } else if (matchFieldAnnotation(field, FieldType.KEY)) {
                throw new IllegalStateException("Should not get KEY-type fields in convergence loop.");
            }

            // if so directed, check the expected type against the real type indicated by Spanner. if this
            // feature is turned off, soft logging errors turn into value exceptions.
            if (driverSettings.checkExpectedTypes()) {
                // resolve the expected column type
                var columnType = resolveColumnType(
                    fieldPointer,
                    spannerOpts,
                    columnOpts,
                    driverSettings
                );

                if (logging.isTraceEnabled())
                    logging.trace("Resolved Spanner type for field '{}': '{}'",
                        fieldPointer.getName(),
                        columnType);

                if (!columnType.getCode().equals(columnValue.getType().getCode())) {
                    logging.error(
                            "Type mismatch: field '{}' expected type {}, but got {}.",
                        fieldPointer.getField().getFullName(),
                        columnType.getCode().name(),
                        columnValue.getType().getCode().name()
                    );
                    return;
                }
            }

            // if we make it this far, the following conditions are true, and we are ready to copy a value from the
            // source struct into the proto:
            //
            // 1) we have a non-NULL value in Spanner for a given column, with a resolved name and type.
            // 2) the name and type match the proto model, where we also have a resolved proto native type.
            // 3) we are certainly operating on a leaf field.
            boolean repeated = columnValue.getType().getCode() == Type.Code.ARRAY;
            Type.Code innerType = repeated ?
                columnValue.getType().getArrayElementType().getCode() :
                columnValue.getType().getCode();

            switch (innerType) {
                case BOOL:
                    spliceBuilder(
                        target,
                        fieldPointer,
                        Optional.of(repeated ? columnValue.getBoolArray() : columnValue.getBool())
                    );
                    break;
                case INT64:
                    spliceBuilder(
                        target,
                        fieldPointer,
                        Optional.of(repeated ? columnValue.getInt64Array() : columnValue.getInt64())
                    );
                    break;

                case FLOAT64:
                    spliceBuilder(
                        target,
                        fieldPointer,
                        Optional.of(repeated ? columnValue.getFloat64Array() : columnValue.getFloat64())
                    );
                    break;

                case STRING:
                    // special case: string fields containing model-compliant JSON
                    if (fieldPointer.getField().getType() == Descriptors.FieldDescriptor.Type.MESSAGE &&
                        (spannerOpts.isPresent() && spannerOpts.get().getType() == SpannerOptions.SpannerType.JSON ||
                         columnOpts.isPresent() && columnOpts.get().getSptype() == SpannerOptions.SpannerType.JSON)) {
                        int modelIndex = 0;
                        try {
                            if (repeated) {
                                // decode as a list of JSON-encoded model instances.
                                var encodedModels = columnValue.getStringArray();
                                var modelResults = new ArrayList<>(encodedModels.size());
                                for (var encodedModel : encodedModels) {
                                    var subBuilder = target.newBuilderForField(fieldPointer.getField());
                                    defaultJsonParser.merge(encodedModel, subBuilder);
                                    modelResults.add(subBuilder.build());
                                    modelIndex++;
                                }

                                // mount set of models on the target proto
                                spliceBuilder(
                                        target,
                                        fieldPointer,
                                        Optional.of(modelResults)
                                );

                            } else {
                                // decode as a singular JSON-encoded model instance.
                                var encodedModel = columnValue.getString();
                                var subBuilder = target.newBuilderForField(fieldPointer.getField());
                                defaultJsonParser.merge(encodedModel, subBuilder);
                                spliceBuilder(
                                        target,
                                        fieldPointer,
                                        Optional.of(subBuilder.build())
                                );
                            }

                        } catch (InvalidProtocolBufferException invalidProtoException) {
                            if (repeated) {
                                logging.error(format(
                                        "Failed to deserialize JSON model at path '%s', at index %s.",
                                        fieldPointer.getField().getFullName(),
                                        modelIndex
                                ), invalidProtoException);
                            } else {
                                logging.error(format(
                                        "Failed to deserialize JSON model at path '%s'.",
                                        fieldPointer.getField().getFullName()
                                ), invalidProtoException);
                            }

                            throw new IllegalStateException(invalidProtoException);
                        }
                    } else if (fieldPointer.getField().getType() == Descriptors.FieldDescriptor.Type.ENUM) {
                        // special case: if this string column is mapped to an `enum` field, we are being asked to
                        // decode it from the enumeration. in this case, protobuf will be expecting an instance of
                        // `EnumValueDescriptor`, not `String`.
                        if (repeated) {
                            var stringList = columnValue.getStringArray();
                            if (!stringList.isEmpty()) {
                                var enumValues = stringList.stream().map((symbol) -> {
                                    try {
                                        return Optional.ofNullable(fieldPointer
                                            .getField()
                                            .getEnumType()
                                            .findValueByName(symbol));
                                    } catch (IllegalArgumentException iae) {
                                        if (logging.isWarnEnabled())
                                            logging.warn(format(
                                                "Failed to decode repeated string enum value '%s' at path '%s'.",
                                                columnValue.getString(),
                                                fieldPointer.getField().getFullName()));
                                        return Optional.<Descriptors.EnumValueDescriptor>empty();
                                    }
                                }).filter(Optional::isPresent)
                                        .map(Optional::get)
                                        .collect(Collectors.toUnmodifiableList());

                                if (!enumValues.isEmpty()) {
                                    spliceBuilder(
                                        target,
                                        fieldPointer,
                                        Optional.of(enumValues)
                                    );
                                }
                            }
                        } else {
                            try {
                                var enumValue = fieldPointer
                                    .getField()
                                    .getEnumType()
                                    .findValueByName(columnValue.getString());

                                if (enumValue != null) {
                                    spliceBuilder(
                                            target,
                                            fieldPointer,
                                            Optional.of(enumValue)
                                    );
                                }
                            } catch (IllegalArgumentException iae) {
                                if (logging.isWarnEnabled())
                                    logging.warn(format(
                                        "Failed to decode singular string enum value '%s' at path '%s'.",
                                        columnValue.getString(),
                                        fieldPointer.getField().getFullName()));
                            }
                        }
                    } else {
                        spliceBuilder(
                            target,
                            fieldPointer,
                            Optional.of(repeated ? columnValue.getStringArray() : columnValue.getString())
                        );
                    }
                    break;

                case BYTES:
                    spliceBuilder(
                        target,
                        fieldPointer,
                        Optional.of(repeated ? columnValue.getBytesArray() : columnValue.getBytes())
                    );
                    break;

                case NUMERIC:
                    if (field.getType() == Descriptors.FieldDescriptor.Type.STRING) {
                        // grab the string value and splice
                        spliceBuilder(
                            target,
                            fieldPointer,
                            Optional.of(repeated ? columnValue.getStringArray() : columnValue.getString())
                        );
                        break;
                    }

                    // throw illegal state
                    throw new IllegalStateException(
                        "NUMERIC fields must be expressed as proto-strings, in exponent notation if necessary. For " +
                        "more information, please see the Cloud Spanner documentation regarding NUMERIC types: " +
                        "https://cloud.google.com/spanner/docs/working-with-numerics");

                case TIMESTAMP:
                    // extract timestamp value
                    var timestampValue = columnValue.getTimestamp();

                    switch (field.getType()) {
                        case UINT64:
                        case FIXED64:
                            // we're being asked to put a Google Cloud timestamp record into an unsigned integer field,
                            // with a `long`-style size. this is the only possible safe native conversion.
                            spliceBuilder(
                                target,
                                fieldPointer,
                                Optional.of((timestampValue.getSeconds() * 1000) + timestampValue.getNanos())
                            );
                            break;

                        case STRING:
                            // we're being asked to put a Google Cloud timestamp record into a string field. in this
                            // case, the adapter leverages any date options or otherwise defaults to ISO8601.
                            spliceBuilder(
                                target,
                                fieldPointer,
                                Optional.of(timestampValue.toString())
                            );
                            break;

                        case MESSAGE:
                            // if we have a sub-message in the same spot as a timestamp, it's worth checking to see if
                            // it's a native Google Cloud timestamp, in which case we can just use it. otherwise, if we
                            // encounter a standard PB timestamp, we need to convert.
                            if (Timestamp.getDescriptor().getFullName().equals(field.getMessageType().getFullName())) {
                                spliceBuilder(
                                    target,
                                    fieldPointer,
                                    Optional.of(protoTimestampFromCloud(timestampValue))
                                );
                                break;
                            }

                            // any other sub-message type represents an illegal state.
                            throw new IllegalStateException(
                                "Cannot convert Spanner TIMESTAMP value to unsupported sub-message type " +
                                "'" + field.getMessageType().getFullName() + "'."
                            );

                        default:
                            // any other expressed field represents an illegal state.
                            throw new IllegalStateException(
                                "Cannot convert Spanner TIMESTAMP value to proto-type '" + field.getType().name() + "'."
                            );
                    }

                case DATE:
                    // extract date value
                    com.google.cloud.Date dateValue;
                    try {
                        dateValue = columnValue.getDate();
                    } catch (IllegalStateException ise) {
                        // try to fall back to timestamp types. for unclear reasons, the driver will report `DATE`
                        // fields as `TIMESTAMP` types in some cases.
                        var ts = columnValue.getTimestamp();
                        dateValue = com.google.cloud.Date.fromJavaUtilDate(ts.toDate());
                    }
                    if (field.getType() == Descriptors.FieldDescriptor.Type.STRING) {
                        // we're being asked to put a Google Cloud structured date record into a string field. in
                        // this case, the adapter leverages any date options or otherwise defaults to ISO8601.
                        spliceBuilder(
                            target,
                            fieldPointer,
                            Optional.of(format(
                                "%s/%s/%s",
                                dateValue.getYear(),
                                dateValue.getMonth(),
                                dateValue.getDayOfMonth()
                            ))
                        );
                        break;
                    } else if (field.getType() == Descriptors.FieldDescriptor.Type.MESSAGE &&
                            Date.getDescriptor().getFullName().equals(field.getMessageType().getFullName())) {
                        // if we have a `Date` sub-message in the same spot as a date, we need to convert to a standard
                        // proto date, which is the only supported target here besides a standard `Timestamp` type.
                        spliceBuilder(
                            target,
                            fieldPointer,
                            Optional.of(protoDateFromCloud(dateValue))
                        );
                        break;
                    } else if (field.getType() == Descriptors.FieldDescriptor.Type.MESSAGE &&
                               Timestamp.getDescriptor().getFullName().equals(field.getMessageType().getFullName())) {
                        // if we have a sub-message in the same spot as a date that is a standard proto `Timestamp`, we
                        // need to convert to it.
                        var dateInstant = com.google.cloud.Date.toJavaUtilDate(dateValue).toInstant();

                        spliceBuilder(
                            target,
                            fieldPointer,
                            Optional.of(Timestamp.newBuilder()
                                .setSeconds(dateInstant.getEpochSecond())
                                .setNanos(dateInstant.getNano())
                                .build())
                        );
                        break;
                    } else if (field.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
                        throw new IllegalStateException(
                            "Cannot convert Spanner DATE value to unsupported sub-message type " +
                            "'" + field.getMessageType().getFullName() + "'."
                        );
                    } else  {
                        // any other expressed field represents an illegal state.
                        throw new IllegalStateException(
                            "Cannot convert Spanner DATE value to proto-type '" + field.getType().name() + "'."
                        );
                    }

                case ARRAY:
                    throw new IllegalStateException(
                        "Should not receive `ARRAY` field types for concrete decoding."
                    );

                case STRUCT:
                    convergeFields(
                        target.newBuilderForField(fieldPointer.getField()),
                        fieldPointer.getField().getMessageType(),
                        columnValue.getStruct(),
                        columnValue.getStruct().getType().getStructFields()
                    );
            }
        }
    }

    /**
     * Resolve all fields for the provided `target` `model`, from the provided row struct `source`. If a value is
     * present, decode it according to the assigned Spanner {@link Type} and any present or implied model
     * annotations.
     *
     * <p>This method performs recursion for nested `STRUCT` objects inside the row. Such structures are interpreted
     * according to model annotations present or implied on the target builder. In such cases, `base` is set to the
     * root builder being filled in. For the initial case, `base` is always {@link Optional#empty()}.</p>
     *
     * @param target Target builder which we intend to fill in with values.
     * @param model Model descriptor for the object we are building.
     * @param source Source row structure to pull data from.
     * @param fields List of fields present in the row, for efficient model filtering.
     */
    private void convergeFields(@Nonnull Message.Builder target,
                                @Nonnull Descriptors.Descriptor model,
                                @Nonnull Struct source,
                                @Nonnull List<Type.StructField> fields) {
        if (logging.isDebugEnabled()) logging.trace(
                "More than one column value present in row. Decoding as {}...",
                modelDescriptor.getFullName());

        // compute a set of projection fields
        SortedSet<String> eligibleFields = fields.isEmpty() ? new TreeSet<>() : fields
                .stream()
                .map(Type.StructField::getName)
                .collect(Collectors.toCollection(TreeSet::new));

        forEachField(
            model,
            Optional.of(onlySpannerEligibleFields(eligibleFields, driverSettings)),
            (pointer) -> {
                // decide if we should recurse. we should never recurse for `JSON` fields, or for fields marked with
                // `ignore` on the column or spanner options.
                var spannerOpts = spannerOpts(pointer);
                var columnOpts = columnOpts(pointer);
                return !(
                    (columnOpts.isPresent() && columnOpts.get().getIgnore()) ||
                    (spannerOpts.isPresent() && spannerOpts.get().getIgnore()) ||
                    (spannerOpts.isPresent() && spannerOpts.get().getType().equals(SpannerOptions.SpannerType.JSON))
                );
            }
        ).forEach((fieldPointer) -> {
            if (logging.isDebugEnabled()) logging.trace(
                    "Converging eligible column field {}...",
                    fieldPointer.getField().getFullName());

            convergeColumnField(
                target,
                source,
                fieldPointer
            );
        });
    }

    /** @inheritDoc */
    @Override
    public @Nonnull Model inflate(@Nonnull Struct rowStruct) throws ModelInflateException {
        Objects.requireNonNull(rowStruct, "cannot inflate null row struct from spanner");

        // grab field count and begin iterating over fields, and assigning by type
        var fieldCount = rowStruct.getColumnCount();
        if (fieldCount > 0 && logging.isDebugEnabled()) logging.debug(
                "Inflating {} fields from Spanner row struct.",
                fieldCount);

        if (fieldCount < 1) {
            logging.warn("Empty rowStruct. Discarding.");
            //noinspection unchecked
            return (Model)defaultInstance.newBuilderForType().build();
        } else {
            if (logging.isTraceEnabled()) logging.trace(
                    "More than one column value present in row. Decoding as {}...",
                    modelDescriptor.getFullName());

            // create a new builder, converge against it from the source row structure.
            var builder = defaultInstance.newBuilderForType();
            convergeFields(
                builder,
                modelDescriptor,
                rowStruct,
                rowStruct.getType().getStructFields()
            );

            //noinspection unchecked
            return (Model)builder.build();
        }
    }
}
