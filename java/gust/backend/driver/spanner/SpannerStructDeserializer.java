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
import com.google.protobuf.util.JsonFormat;
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
import java.util.*;
import java.util.stream.Collectors;

import static gust.backend.driver.spanner.SpannerUtil.*;
import static gust.backend.model.ModelMetadata.*;


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
    static <M extends Message> SpannerStructDeserializer<M> forModel(
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
     * @param fieldPointer Resolved field pointer.
     * @param target Target message builder.
     * @param value Resolved value for the ID.
     */
    private void inflateRowKey(@Nonnull ModelMetadata.FieldPointer fieldPointer,
                               @Nonnull Message.Builder target,
                               @Nonnull Value value) {
        Objects.requireNonNull(fieldPointer, "cannot inflate Spanner key with no field pointer");
        Objects.requireNonNull(value, "cannot inflate Spanner key from NULL value");
        var keyBuilder = target.newBuilderForField(fieldPointer.getField());
        var keyType = resolveKeyType(idField(keyBuilder.getDefaultInstanceForType()).orElseThrow());

        // splice the ID into the key
        if (keyType.getCode() == Type.Code.STRING) {
            spliceIdBuilder(keyBuilder, Optional.of(value.getString()));
        } else if (keyType.getCode() == Type.Code.INT64) {
            spliceIdBuilder(keyBuilder, Optional.of(value.getInt64()));
        } else {
            throw new IllegalStateException(String.format("Unsupported key type: '%s'.", keyType.getCode().name()));
        }

        // splice the key into the model
        target.setField(fieldPointer.getField(), keyBuilder.build());
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

        // resolve the expected column index
        var columnValue = resolveColumnValue(
            source,
            fieldPointer,
            spannerOpts,
            columnOpts,
            driverSettings
        );

        if (columnValue.isNull()) {
            if (logging.isTraceEnabled())
                logging.trace("Resolved column value for field '{}' was NULL. Skipping.",
                    fieldPointer.getName());
        } else {
            // first up, check to see if this is a key or ID field, and decode it properly if so
            var field = fieldPointer.getField();
            if (matchFieldAnnotation(field, FieldType.KEY) ||
                    matchFieldAnnotation(field, FieldType.ID)) {
                this.inflateRowKey(fieldPointer, target, columnValue);
                return;
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
                        columnType.toString());

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

            // if we make it this far, the following conditions are true, and we are ready to copy a value from
            // the source struct into the proto:
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
                        if (repeated) {
                            // decode as a list of JSON-encoded model instances.
                            var encodedModels = columnValue.getStringArray();
                            var modelResults = new ArrayList<>(encodedModels.size());
                            var modelIndex = 0;
                            for (var encodedModel : encodedModels) {
                                try {
                                    var subBuilder = target.newBuilderForField(fieldPointer.getField());
                                    defaultJsonParser.merge(encodedModel, subBuilder);
                                    modelResults.add(subBuilder.build());
                                    modelIndex++;

                                } catch (InvalidProtocolBufferException invalidProtoException) {
                                    logging.error(String.format(
                                        "Failed to deserialize JSON model at path '%s', at index %s.",
                                        fieldPointer.getField().getFullName(),
                                        modelIndex
                                    ), invalidProtoException);

                                    throw new RuntimeException(invalidProtoException);
                                }
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
                            try {
                                var subBuilder = target.newBuilderForField(fieldPointer.getField());
                                defaultJsonParser.merge(encodedModel, subBuilder);
                                spliceBuilder(
                                    target,
                                    fieldPointer,
                                    Optional.of(subBuilder.build())
                                );
                            } catch (InvalidProtocolBufferException invalidProtoException) {
                                logging.error(String.format(
                                    "Failed to deserialize JSON model at path '%s'.",
                                    fieldPointer.getField().getFullName()
                                ), invalidProtoException);

                                throw new RuntimeException(invalidProtoException);
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
                    // @TODO(sgammon): implement NUMERIC support
                    throw new IllegalStateException("NUMERIC fields are not supported yet.");

                case TIMESTAMP:
                    // @TODO(sgammon): implement TIMESTAMP support
                    throw new IllegalStateException("TIMESTAMP fields are not supported yet.");

                case DATE:
                    // @TODO(sgammon): implement DATE support
                    throw new IllegalStateException("DATE fields are not supported yet.");

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
            Optional.of(onlySpannerEligibleFields(eligibleFields, driverSettings))
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
