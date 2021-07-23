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

import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Type;
import com.google.cloud.spanner.Value;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import gust.backend.runtime.Logging;
import org.slf4j.Logger;
import tools.elide.core.*;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static gust.backend.model.ModelMetadata.*;


/**
 * Provides utilities related to operations with Spanner, including tools for resolving column names and types from
 * models and annotations, and producing default sets of columns for DDL statements.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class SpannerUtil {
    private static final Logger logging = Logging.logger(SpannerUtil.class);

    private SpannerUtil() { /* disallow construction */ }

    /** Default predicate to use when filtering for eligible Spanner fields. */
    private static final @Nonnull Predicate<FieldPointer> defaultFieldPredicate;

    static {
        defaultFieldPredicate = (fieldPointer) -> {
            var field = fieldPointer.getField();
            var fieldOpts = fieldAnnotation(field, Datamodel.field);
            var columnOpts = fieldAnnotation(field, Datamodel.column);
            var spannerOpts = fieldAnnotation(field, Datamodel.spanner);

            return !(
                // field cannot be present if skipped via `column.ignore`
                (columnOpts.isPresent() && columnOpts.get().getIgnore()) ||

                // field cannot be present if skipped via `spanner.ignore`
                (spannerOpts.isPresent() && spannerOpts.get().getIgnore()) ||

                // properties marked as `INTERNAL` should always be withheld
                (fieldOpts.isPresent() && fieldOpts.get().getVisibility() == FieldVisibility.INTERNAL)
            );
        };
    }

    /**
     * For a given key field pointer, resolve the column name which should be used for the primary key in Spanner,
     * according to the annotation structure present on the key.
     *
     * @see #resolveKeyType(FieldPointer) To resolve the primary key column type.
     * @param keyField Resolved field pointer to a given model's key field.
     * @param driverSettings Settings for the Spanner driver.
     * @return Name of the column we should use for the primary key.
     */
    public static @Nonnull String resolveKeyColumn(@Nonnull FieldPointer keyField,
                                                   @Nonnull SpannerDriverSettings driverSettings) {
        // resolve key field and column name corresponding to that key field
        return resolveColumnName(
            keyField,
            spannerOpts(keyField),
            columnOpts(keyField),
            driverSettings
        );
    }

    /**
     * For a given key field pointer, resolve the column type which should be used for the primary key in Spanner,
     * according to the annotation structure present on the key.
     *
     * @see #resolveKeyColumn(FieldPointer, SpannerDriverSettings) To resolve the primary key column name.
     * @param idField Resolved field pointer to a given model key's ID field.
     * @return Spanner column type to use for this model's ID.
     */
    public static @Nonnull Type resolveKeyType(@Nonnull FieldPointer idField) {
        // resolve the expected key column type. validate it on the way.
        if (idField.getField().getType() == Descriptors.FieldDescriptor.Type.STRING) {
            return Type.string();
        } else if (idField.getField().getType() == Descriptors.FieldDescriptor.Type.INT64) {
            return Type.int64();
        } else {
            throw new IllegalStateException(
                String.format("Unsupported key field type: '%s'.", idField.getField().getType().name()));
        }
    }

    /**
     * Given a resolved field pointer and set of annotations, resolve the expected/configured column name in Spanner for
     * a given typed model field. If no specialized Spanner or table column annotations are present, fallback to a
     * calculated default name.
     *
     * @see #resolveColumnName(Descriptors.FieldDescriptor, Optional, Optional, SpannerDriverSettings) For the full
     *      un-sugared version of this method.
     * @param fieldPointer Pre-resolved model field pointer.
     * @param spannerOpts Spanner-specific options on the field.
     * @param columnOpts Column-generic options on the field.
     * @param settings Settings for the Spanner driver.
     * @return Expected name of the field when expressed as a column in Spanner.
     */
    public static @Nonnull String resolveColumnName(@Nonnull FieldPointer fieldPointer,
                                                    @Nonnull Optional<SpannerFieldOptions> spannerOpts,
                                                    @Nonnull Optional<TableFieldOptions> columnOpts,
                                                    @Nonnull SpannerDriverSettings settings) {
        return resolveColumnName(
            fieldPointer.getField(),
            spannerOpts,
            columnOpts,
            settings
        );
    }

    /**
     * Given a resolved Protocol Buffer field descriptor and set of annotations, resolve the expected/configured column
     * name in Spanner for a given typed model field. If no specialized Spanner or table column annotations are present,
     * fallback to a calculated default name.
     *
     * <p>{@link SpannerFieldOptions} always outweigh {@link TableFieldOptions}. If two similar or congruent properties
     * are set between options, generic options are applied first, and then specialized options override.</p>
     *
     * <p>If {@link SpannerDriverSettings#preserveFieldNames()} is activated when this method is called, default names
     * will use the literal field name from the Protocol Buffer definition. Otherwise, JSON-style names are calculated
     * and used as default names.</p>
     *
     * <p>Similarly, if {@link SpannerDriverSettings#defaultCapitalizedNames()} is activated when this method is called,
     * default names will use JSON-style naming but with initial capitals. For example, `name` turns into `Name` and
     * `contact_info` turns into `ContactInfo`. In all cases, explicit property names from specialized or generic
     * annotations prevail, then {@link SpannerDriverSettings#preserveFieldNames()} prevails, then the default form of
     * naming with Spanner capitalized names active.</p>
     *
     * @see #resolveColumnName(FieldPointer, Optional, Optional, SpannerDriverSettings) For a version of this method
     *      which operates on {@link FieldPointer} objects.
     * @param field Protocol Buffer field descriptor for which we should resolve a Spanner column name.
     * @param spannerOpts Spanner options applied to this field as annotations.
     * @param columnOpts Generic table column settings applied to this field as annotations.
     * @param settings Settings for the Spanner driver.
     * @return Resolved column name, from explicit annotations, or by way of default calculation, as described above.
     */
    public static @Nonnull String resolveColumnName(@Nonnull Descriptors.FieldDescriptor field,
                                                    @Nonnull Optional<SpannerFieldOptions> spannerOpts,
                                                    @Nonnull Optional<TableFieldOptions> columnOpts,
                                                    @Nonnull SpannerDriverSettings settings) {
        // resolve the expected column name in Spanner.
        var columnName = spannerOpts.isPresent() && !spannerOpts.get().getColumn().isBlank() ?
                 spannerOpts.get().getColumn() :
               columnOpts.isPresent() && !columnOpts.get().getName().isBlank() ?
                 columnOpts.get().getName() :
               settings.preserveFieldNames() ?
                 field.getName() :
               field.getJsonName();

        if (logging.isTraceEnabled())
            logging.trace("Resolved column name for field '{}': '{}'",
                field.getName(),
                columnName);
        return columnName;
    }

    /**
     * Given a Spanner row result expressed as a {@link Struct} and a {@link FieldPointer} which is expected to be
     * present, with a pre-resolved column name, return the numeric column index.
     *
     * @param source Row result from Spanner which we should resolve the column index from.
     * @param fieldPointer Pointer to the field for which we are resolving an index. Pre-resolved.
     * @param name Translated name of the column for which we are resolving an index. Pre-resolved.
     * @return Integer index for the column in the provided row result.
     */
    public static int resolveColumnIndex(@Nonnull Struct source,
                                         @Nonnull FieldPointer fieldPointer,
                                         @Nonnull String name) {
        var columnIndex = source.getColumnIndex(name);
        if (logging.isTraceEnabled())
            logging.trace("Resolved column index for field '{}': '{}'",
                fieldPointer.getName(),
                columnIndex);
        return columnIndex;
    }

    /**
     * Given a Spanner row result expressed as a {@link Struct} and a {@link FieldPointer} which is expected to be
     * present, resolve any present {@link Value}.
     *
     * <p>This method additionally resolves the expected column name for the provided field.</p>
     *
     * @see #resolveColumnName(FieldPointer, Optional, Optional, SpannerDriverSettings) For an explanation of model
     *      field column name calculations and annotation behavior.
     * @param source Row result from Spanner from which we should resolve any present value.
     * @param fieldPointer Pointer to the model field for which we are resolving a value.
     * @param spannerOpts Spanner-specific options and annotations present on the field.
     * @param columnOpts Column-generic options and annotations present on the field.
     * @param driverSettings Settings for the Spanner driver.
     * @return Resolved Spanner value, as applicable.
     */
    public static @Nonnull Value resolveColumnValue(@Nonnull Struct source,
                                                    @Nonnull FieldPointer fieldPointer,
                                                    @Nonnull Optional<SpannerFieldOptions> spannerOpts,
                                                    @Nonnull Optional<TableFieldOptions> columnOpts,
                                                    @Nonnull SpannerDriverSettings driverSettings) {
        var columnValue = source.getValue(resolveColumnIndex(
            source,
            fieldPointer,
            resolveColumnName(
                fieldPointer,
                spannerOpts,
                columnOpts,
                driverSettings
            )
        ));
        if (logging.isTraceEnabled())
            logging.trace("Resolved column value for field '{}': '{}'",
                    fieldPointer.getName(),
                    columnValue.toString());
        return columnValue;
    }

    /**
     * Given a resolved and eligible {@link FieldPointer} for a model field which should interact with Spanner, resolve
     * an expected Spanner {@link Type}, including any nested structure or complex objects, as mediated and regulated by
     * annotations on the model field.
     *
     * <p>If {@link SpannerFieldOptions#getType()} returns a non-default value, it prevails first, with
     * {@link TableFieldOptions#getSptype()} after that. If no explicit type is resolvable from the field definition,
     * a default type is generated (see method references for more information).</p>
     *
     * @see #resolveDefaultType(FieldPointer, SpannerDriverSettings) Fallback behavior if no explicit type is specified.
     * @param fieldPointer Pointer to the model field for which we should resolve a Spanner column type.
     * @param spannerOpts Spanner-specific options or annotations present on the field definition, as applicable.
     * @param columnOpts Column-generic options or annotations present on the field definition, as applicable.
     * @param settings Active settings for the Spanner driver.
     * @return Expected Spanner column type corresponding to the provided model field, considering all annotations.
     */
    public static @Nonnull Type resolveColumnType(@Nonnull FieldPointer fieldPointer,
                                                  @Nonnull Optional<SpannerFieldOptions> spannerOpts,
                                                  @Nonnull Optional<TableFieldOptions> columnOpts,
                                                  @Nonnull SpannerDriverSettings settings) {
        //noinspection deprecation
        return spannerOpts.isPresent() && spannerOpts.get().getType() != SpannerOptions.SpannerType.UNSPECIFIED_TYPE ?
                 resolveType(fieldPointer, spannerOpts.get().getType()) :
               columnOpts.isPresent() && columnOpts.get().getSptype() != SpannerOptions.SpannerType.UNSPECIFIED_TYPE ?
                 resolveType(fieldPointer, columnOpts.get().getSptype()) :
               resolveDefaultType(fieldPointer, settings);
    }

    /**
     * Calculate a default projection of Spanner columns, as configured on the provided default model instance.
     *
     * @param descriptor Default model schema to generate a default set of Spanner columns from.
     * @param driverSettings Settings for the Spanner driver.
     * @return Default list of Spanner columns.
     */
    public static @Nonnull List<String> calculateDefaultFields(@Nonnull Descriptors.Descriptor descriptor,
                                                               @Nonnull SpannerDriverSettings driverSettings) {
        return forEachField(
                descriptor,
                Optional.of(onlySpannerEligibleFields(driverSettings))
        ).map((fieldPointer) -> resolveColumnName(fieldPointer,
            fieldAnnotation(fieldPointer.getField(), Datamodel.spanner),
            fieldAnnotation(fieldPointer.getField(), Datamodel.column),
            driverSettings
        )).collect(Collectors.toUnmodifiableList());
    }

    /**
     * Calculate a default projection of Spanner columns, as configured on the provided default model instance. The
     * default set of columns includes any columns considered "eligible" for storage in Spanner, each pre-resolved with
     * a column name and type.
     *
     * <p>This method is designed to operate deterministically, by visiting each eligible model field in a predictable
     * order and expressing that same order in the output collection.</p>
     *
     * @see #onlySpannerEligibleFields(SpannerDriverSettings) For an explanation of predicate behavior with regard to
     *      eligibility for interaction with Spanner.
     * @see #resolveColumnName(FieldPointer, Optional, Optional, SpannerDriverSettings) For an explanation of Spanner
     *      column name resolution and default calculation behavior.
     * @see #resolveColumnType(FieldPointer, Optional, Optional, SpannerDriverSettings) For an explanation of Spanner
     *      column type resolution and default decision behavior.
     * @param descriptor Default model schema to generate a default set of Spanner columns from.
     * @param driverSettings Settings for the Spanner driver.
     * @return Default list of Spanner columns.
     */
    public static @Nonnull Collection<Type.StructField> generateStruct(@Nonnull Descriptors.Descriptor descriptor,
                                                                       @Nonnull SpannerDriverSettings driverSettings) {
        return forEachField(
            descriptor,
            Optional.of(onlySpannerEligibleFields(driverSettings))
        ).map((fieldPointer) -> {
            var spannerOpts = fieldAnnotation(fieldPointer.getField(), Datamodel.spanner);
            var columnOpts = fieldAnnotation(fieldPointer.getField(), Datamodel.column);
            var name = resolveColumnName(
                fieldPointer,
                spannerOpts,
                columnOpts,
                driverSettings
            );
            var type = resolveColumnType(
                fieldPointer,
                spannerOpts,
                columnOpts,
                driverSettings
            );
            return Type.StructField.of(name, type);
        }).collect(Collectors.toUnmodifiableList());
    }

    /**
     * Return a {@link Predicate} implementation which determines field eligibility with regard to interaction with
     * Cloud Spanner, optionally considering the provided set of circumstantial higher-order eligible fields (for
     * instance, in the case of a known property projection).
     *
     * <p>Field eligibility is determined by the following criteria:
     * <ul>
     *     <li>Model fields <b>MUST</b> be present on the {@link Message} schema to interact with Spanner.</li>
     *     <li>Model fields <b>MUST NOT</b> be annotated with {@link TableFieldOptions#getIgnore()}.</li>
     *     <li>Model fields <b>MUST NOT</b> be annotated with {@link SpannerFieldOptions#getIgnore()}.</li>
     *     <li>Model fields <b>MUST NOT</b> be annotated with {@link FieldVisibility#INTERNAL}.</li>
     *     <li>If provided and non-empty, model fields <b>MUST</b> be present in the set of <pre>eligibleFields</pre>
     *     provided to this method.</li>
     * </ul></p>
     *
     * @see #onlySpannerEligibleFields(SpannerDriverSettings) For circumstances with no known eligible fields.
     * @param eligibleFields Set of higher-order eligible fields, if applicable. Only considered if non-empty.
     * @param settings Settings for the Spanner driver.
     * @return Predicate which filters {@link FieldPointer} objects according to the provided settings.
     */
    public static @Nonnull Predicate<FieldPointer> onlySpannerEligibleFields(@Nonnull SortedSet<String> eligibleFields,
                                                                             @Nonnull SpannerDriverSettings settings) {
        if (eligibleFields.isEmpty()) {
            return defaultFieldPredicate;
        }
        return defaultFieldPredicate.and((fieldPointer) -> {
            var field = fieldPointer.getField();
            var columnOpts = fieldAnnotation(field, Datamodel.column);
            var spannerOpts = fieldAnnotation(field, Datamodel.spanner);

            return !(
                // field should be omitted if not present in list of row fields, if we have any
                !eligibleFields.isEmpty() && !eligibleFields.contains(resolveColumnName(
                    fieldPointer,
                    spannerOpts,
                    columnOpts,
                    settings
                ))
            );
        });
    }

    /**
     * Return a {@link Predicate} implementation which operates on {@link FieldPointer} objects to determine eligibility
     * for interaction with Spanner.
     *
     * <p>This method variant provides no opportunity to filter by higher-order circumstantial fields. Should invoking
     * code have an opportunity to do so, it may dispatch the more customizable form of this method (see below).
     * Additionally, that method may be referenced for a detailed explanation of field eligibility behavior.</p>
     *
     * @see #onlySpannerEligibleFields(SortedSet, SpannerDriverSettings) For the fully-controllable form of this method,
     *      which can combine an additional {@link Predicate} to filter by a set of fields known at invocation time.
     * @param settings Settings for the Spanner driver.
     * @return Predicate to determine {@link FieldPointer} eligibility for interaction with Spanner.
     */
    public static @Nonnull Predicate<FieldPointer> onlySpannerEligibleFields(@Nonnull SpannerDriverSettings settings) {
        return onlySpannerEligibleFields(Collections.emptySortedSet(), settings);
    }

    /**
     * If a concrete type is marked as repeated, wrap it in an array type. Otherwise, just return the type.
     *
     * @param field Field pointer for the model field.
     * @param inner Inner type for the maybe-repeated field.
     * @return Either the array-wrapped type or the concrete individual type.
     */
    public static @Nonnull Type maybeWrapType(@Nonnull FieldPointer field,
                                              @Nonnull Type inner) {
        if (field.getField().isRepeated()) {
            return Type.array(inner);
        }
        return inner;
    }

    /**
     * Resolve a normalized Spanner type for the provided field `pointer`, with the explicit provided `spannerType`.
     * With an explicit type, our job is just to make sure the model configuration is cohesive.
     *
     * @param pointer Resolved field pointer.
     * @param spannerType Explicit Spanner type.
     * @return Resolved normalized Spanner type.
     */
    public static @Nonnull Type resolveType(@Nonnull FieldPointer pointer,
                                            @Nonnull tools.elide.core.SpannerOptions.SpannerType spannerType) {
        switch (spannerType) {
            case STRING:
            case JSON: return maybeWrapType(pointer, Type.string());
            case NUMERIC: return maybeWrapType(pointer, Type.numeric());
            case FLOAT64: return maybeWrapType(pointer, Type.float64());
            case INT64: return maybeWrapType(pointer, Type.int64());
            case BYTES: return maybeWrapType(pointer, Type.bytes());
            case BOOL: return maybeWrapType(pointer, Type.bool());
            case DATE: return maybeWrapType(pointer, Type.date());
            case TIMESTAMP: return maybeWrapType(pointer, Type.timestamp());
            default: throw new IllegalArgumentException("Unrecognized Spanner type.");
        }
    }

    /**
     * Resolve a default Spanner type for the provided field `pointer`. This selects a sensible default when no
     * explicit type annotations are present for a Spanner column's type.
     *
     * @param pointer Field pointer to resolve a Spanner type for.
     * @param settings Settings for the Spanner driver.
     * @return Resolved normalized Spanner type.
     */
    public static @Nonnull Type resolveDefaultType(@Nonnull FieldPointer pointer,
                                                   @Nonnull SpannerDriverSettings settings) {
        switch (pointer.getField().getType()) {
            case DOUBLE:
            case FLOAT:
                return maybeWrapType(pointer, Type.float64());

            case INT32:
            case INT64:
            case UINT32:
            case UINT64:
            case FIXED32:
            case FIXED64:
            case SFIXED32:
            case SFIXED64:
            case SINT32:
            case SINT64:
                return maybeWrapType(pointer, Type.int64());

            case BOOL: return maybeWrapType(pointer, Type.bool());
            case STRING: return maybeWrapType(pointer, Type.string());

            case BYTES: return maybeWrapType(pointer, Type.bytes());
            case ENUM:
                return settings.enumsAsNumbers() ?
                    maybeWrapType(pointer, Type.numeric()) :
                    maybeWrapType(pointer, Type.string());

            case MESSAGE:
                return maybeWrapType(pointer, Type.struct(generateStruct(
                    pointer.getField().getMessageType(),
                    settings
                )));

            case GROUP:
            default:
                throw new IllegalArgumentException(String.format(
                    "Unrecognized Protocol Buffer type: %s.",
                    pointer.getField().getType().name()
                ));
        }
    }

    /**
     * Given a pre-resolved {@link FieldPointer}, resolve any present {@link TableFieldOptions}.
     *
     * @see #columnOpts(Descriptors.FieldDescriptor) For the low-level version of this method, which includes a more
     *      detailed explanation of {@link TableFieldOptions} with regard to Spanner.
     * @param fieldPointer Field pointer for which we should resolve any present column-generic options.
     * @return Set of specified table field options, or {@link Optional#empty()}.
     */
    public static @Nonnull Optional<TableFieldOptions> columnOpts(@Nonnull FieldPointer fieldPointer) {
        return columnOpts(fieldPointer.getField());
    }

    /**
     * Given a resolved Protocol Buffer {@link Descriptors.FieldDescriptor} which is considered eligible for interaction
     * with Spanner, resolve any present column-generic options and annotations via {@link TableFieldOptions}.
     *
     * <p>Column-generic options apply to engines which operate in a columnar manner. This includes Spanner, but also
     * includes engines like BigQuery and SQL-based systems. To allow adaptation to those systems without curtailing
     * control of table and field naming, the Spanner driver respects {@link TableFieldOptions} but defers to any
     * present {@link SpannerFieldOptions}.</p>
     *
     * @see #columnOpts(FieldPointer) For a version of this method which operates on {@link FieldPointer}.
     * @see #spannerOpts(Descriptors.FieldDescriptor) For the equivalent version of this method that returns Spanner-
     *      specific field options.
     * @param field Field descriptor for which we should resolve any present {@link TableFieldOptions}.
     * @return Any present column-generic field options or annotations, or {@link Optional#empty()}.
     */
    public static @Nonnull Optional<TableFieldOptions> columnOpts(@Nonnull Descriptors.FieldDescriptor field) {
        // resolve any generic column options...
        var columnOpts = fieldAnnotation(
            field,
            Datamodel.column
        );

        if (columnOpts.isPresent() && logging.isDebugEnabled())
            logging.debug("Found column options for field '{}': \n{}",
                field.getName(),
                columnOpts.toString());
        else if (columnOpts.isEmpty() && logging.isDebugEnabled()) {
            logging.debug("No column opts for field '{}'. Using defaults.",
                field.getName());
        }
        return columnOpts;
    }

    /**
     * Given a pre-resolved {@link FieldPointer}, resolve any present {@link SpannerFieldOptions}.
     *
     * @see #spannerOpts(Descriptors.FieldDescriptor) For the low-level version of this method, which includes a more
     *      detailed explanation of {@link SpannerFieldOptions}.
     * @param fieldPointer Field pointer for which we should resolve any present column-generic options.
     * @return Set of specified table field options, or {@link Optional#empty()}.
     */
    public static @Nonnull Optional<SpannerFieldOptions> spannerOpts(@Nonnull FieldPointer fieldPointer) {
        return spannerOpts(fieldPointer.getField());
    }

    /**
     * Given a resolved Protocol Buffer {@link Descriptors.FieldDescriptor} which is considered eligible for interaction
     * with Spanner, resolve any present Spanner-specific options and annotations via {@link SpannerFieldOptions}.
     *
     * <p>To adapt to other columnar-style engines, model fields may be annotated with {@link TableFieldOptions}. In all
     * cases, present {@link SpannerFieldOptions} override with regard to Spanner Driver behavior.</p>
     *
     * @see #spannerOpts(FieldPointer) For a version of this method which operates on {@link FieldPointer}.
     * @see #columnOpts(Descriptors.FieldDescriptor) For the equivalent version of this method that returns column-
     *      generic field options.
     * @param field Field descriptor for which we should resolve any present {@link SpannerFieldOptions}.
     * @return Any present column-generic field options or annotations, or {@link Optional#empty()}.
     */
    public static @Nonnull Optional<SpannerFieldOptions> spannerOpts(@Nonnull Descriptors.FieldDescriptor field) {
        // resolve spanner options next, which override any default table options.
        var spannerOpts = fieldAnnotation(
            field,
            Datamodel.spanner
        );

        if (spannerOpts.isPresent() && logging.isDebugEnabled())
            logging.debug("Found Spanner options for field '{}': \n{}",
                    field.getName(),
                    spannerOpts.toString());
        else if (spannerOpts.isEmpty() && logging.isDebugEnabled()) {
            logging.debug("No Spanner opts for field '{}'. Using defaults.",
                field.getName());
        }
        return spannerOpts;
    }
}
