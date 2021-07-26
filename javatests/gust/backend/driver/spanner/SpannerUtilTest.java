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

import com.google.cloud.spanner.Type;
import com.google.protobuf.Descriptors;
import gust.backend.model.ModelMetadata;
import gust.backend.model.PersonRecord;
import org.junit.jupiter.api.Test;
import tools.elide.core.SpannerFieldOptions;
import tools.elide.core.SpannerOptions;
import tools.elide.core.TableFieldOptions;

import javax.annotation.Nonnull;

import java.util.Optional;

import static gust.backend.model.ModelMetadata.*;
import static gust.backend.driver.spanner.SpannerUtil.*;
import static org.junit.jupiter.api.Assertions.*;


/** Spanner utility unit testing. */
public final class SpannerUtilTest {
    private void assertResolvedType(@Nonnull ModelMetadata.FieldPointer pointer,
                                    @Nonnull Type.Code typeCode,
                                    @Nonnull SpannerOptions.SpannerType spannerType,
                                    boolean repeated) {
        if (repeated) {
            assertSame(
                Type.Code.ARRAY,
                resolveType(pointer, spannerType).getCode(),
                "repeated type should present as ARRAY type in Spanner"
            );
            assertSame(
                typeCode,
                resolveType(pointer, spannerType).getArrayElementType().getCode(),
                String.format("repeated type should present as %s type in Spanner", spannerType.name())
            );
        } else {
            assertEquals(
                typeCode,
                resolveType(pointer, spannerType).getCode(),
                String.format("%s resolved type should match %s type", typeCode.name(), typeCode.name())
            );
        }
    }

    @Test public void testResolveDefaultGroupTypeFail() {
        var nameField = ModelMetadata.FieldPointer.fieldAtName(
            PersonRecord.Person.getDescriptor(),
            "name"
        );

        assertThrows(IllegalArgumentException.class, () ->
            resolveDefaultType(nameField, Descriptors.FieldDescriptor.Type.GROUP, SpannerDriverSettings.DEFAULTS)
        );
    }

    @Test public void testResolveEnumsAsNumbers() {
        var nameField = ModelMetadata.FieldPointer.fieldAtName(
            PersonRecord.Person.getDescriptor(),
            "name"
        );

        var enumNumbers = resolveDefaultType(
                nameField,
                Descriptors.FieldDescriptor.Type.ENUM,
                new SpannerDriverSettings() {
                    @Nonnull
                    @Override
                    public Boolean enumsAsNumbers() {
                        return true;
                    }
                }
        );

        assertEquals(
            Type.Code.INT64,
            enumNumbers.getCode(),
            "INT64 should be type of integer enums"
        );
    }

    @Test public void testResolveEnumsAsStrings() {
        var nameField = ModelMetadata.FieldPointer.fieldAtName(
            PersonRecord.Person.getDescriptor(),
            "name"
        );

        var enumStrings = resolveDefaultType(
                nameField,
                Descriptors.FieldDescriptor.Type.ENUM,
                new SpannerDriverSettings() {
                    @Nonnull
                    @Override
                    public Boolean enumsAsNumbers() {
                        return false;
                    }
                }
        );

        assertEquals(
            Type.Code.STRING,
            enumStrings.getCode(),
            "STRING should be type of string enums"
        );
    }

    @Test public void testSingularFieldWrap() {
        var singularField = ModelMetadata.FieldPointer.fieldAtName(
            PersonRecord.Person.getDescriptor(),
            "name"
        );

        assertResolvedType(singularField, Type.Code.STRING, SpannerOptions.SpannerType.STRING, false);
        assertResolvedType(singularField, Type.Code.NUMERIC, SpannerOptions.SpannerType.NUMERIC, false);
        assertResolvedType(singularField, Type.Code.INT64, SpannerOptions.SpannerType.INT64, false);
        assertResolvedType(singularField, Type.Code.FLOAT64, SpannerOptions.SpannerType.FLOAT64, false);
        assertResolvedType(singularField, Type.Code.BYTES, SpannerOptions.SpannerType.BYTES, false);
        assertResolvedType(singularField, Type.Code.BOOL, SpannerOptions.SpannerType.BOOL, false);
        assertResolvedType(singularField, Type.Code.DATE, SpannerOptions.SpannerType.DATE, false);
        assertResolvedType(singularField, Type.Code.TIMESTAMP, SpannerOptions.SpannerType.TIMESTAMP, false);
    }

    @Test public void testFailStructTypeAsColumn() {
        var singularField = ModelMetadata.FieldPointer.fieldAtName(
            PersonRecord.Person.getDescriptor(),
            "name"
        );

        assertThrows(IllegalArgumentException.class, () ->
            assertResolvedType(
                singularField,
                Type.Code.STRUCT,
                SpannerOptions.SpannerType.UNRECOGNIZED,
                false
            )
        );
    }

    @Test public void testMaybeWrapArray() {
        var repeatedField = ModelMetadata.FieldPointer.fieldAtName(
            PersonRecord.TypeBuffet.getDescriptor(),
            "labels"
        );

        assertResolvedType(
            repeatedField,
            Type.Code.STRING,
            SpannerOptions.SpannerType.STRING,
            true
        );
    }

    @Test public void testSchemaRequiresTableName() {
        assertThrows(IllegalArgumentException.class, () -> resolveTableName(
            PersonRecord.EnrollEvent.getDescriptor()
        ));
    }

    @Test public void testRestrictedKeyTypes() {
        // strings can be keys
        assertDoesNotThrow(() -> resolveKeyType(pluck(
            PersonRecord.TypeBuffet.getDefaultInstance(),
            "string_field"
        ).getField()));

        // any 64-bit unsigned integer can be a key
        assertDoesNotThrow(() -> resolveKeyType(pluck(
            PersonRecord.TypeBuffet.getDefaultInstance(),
            "uint_double"
        ).getField()));
        assertDoesNotThrow(() -> resolveKeyType(pluck(
            PersonRecord.TypeBuffet.getDefaultInstance(),
            "fixed_double"
        ).getField()));

        // no other type can be a key
        assertThrows(IllegalStateException.class, () -> resolveKeyType(pluck(
            PersonRecord.TypeBuffet.getDefaultInstance(),
            "int_normal"
        ).getField()));
        assertThrows(IllegalStateException.class, () -> resolveKeyType(pluck(
            PersonRecord.TypeBuffet.getDefaultInstance(),
            "uint_normal"
        ).getField()));
        assertThrows(IllegalStateException.class, () -> resolveKeyType(pluck(
            PersonRecord.TypeBuffet.getDefaultInstance(),
            "sint_normal"
        ).getField()));
        assertThrows(IllegalStateException.class, () -> resolveKeyType(pluck(
            PersonRecord.TypeBuffet.getDefaultInstance(),
            "sint_double"
        ).getField()));
        assertThrows(IllegalStateException.class, () -> resolveKeyType(pluck(
            PersonRecord.TypeBuffet.getDefaultInstance(),
            "fixed_normal"
        ).getField()));
        assertThrows(IllegalStateException.class, () -> resolveKeyType(pluck(
            PersonRecord.TypeBuffet.getDefaultInstance(),
            "sfixed_normal"
        ).getField()));
        assertThrows(IllegalStateException.class, () -> resolveKeyType(pluck(
            PersonRecord.TypeBuffet.getDefaultInstance(),
            "sfixed_double"
        ).getField()));
        assertThrows(IllegalStateException.class, () -> resolveKeyType(pluck(
            PersonRecord.TypeBuffet.getDefaultInstance(),
        "bool_field"
        ).getField()));
        assertThrows(IllegalStateException.class, () -> resolveKeyType(pluck(
            PersonRecord.TypeBuffet.getDefaultInstance(),
            "bytes_field"
        ).getField()));
        assertThrows(IllegalStateException.class, () -> resolveKeyType(pluck(
            PersonRecord.TypeBuffet.getDefaultInstance(),
            "float_field"
        ).getField()));
        assertThrows(IllegalStateException.class, () -> resolveKeyType(pluck(
            PersonRecord.TypeBuffet.getDefaultInstance(),
            "double_field"
        ).getField()));
        assertThrows(IllegalStateException.class, () -> resolveKeyType(pluck(
            PersonRecord.TypeBuffet.getDefaultInstance(),
            "enum_field"
        ).getField()));

        // repeated types cannot be keys, even if they are typed eligibly otherwise
        assertThrows(IllegalStateException.class, () -> resolveKeyType(pluck(
            PersonRecord.TypeBuffet.getDefaultInstance(),
            "labels"
        ).getField()));
    }

    @Test public void testColumnNamingBehavior() {
        var singularField = ModelMetadata.FieldPointer.fieldAtName(
            PersonRecord.TypeBuffet.getDescriptor(),
            "double_field"
        );

        // any name specified in the `SpannerFieldOptions` should prevail
        assertEquals(
            "SomeOtherName",
            resolveColumnName(
                singularField,
                Optional.of(SpannerFieldOptions.newBuilder()
                    .setColumn("SomeOtherName")
                    .build()),
                Optional.of(TableFieldOptions.newBuilder()
                    .setName("NotThisName")
                    .build()),
                SpannerDriverSettings.DEFAULTS
            ),
            "`SpannerFieldOptions` should always prevail"
        );
        assertEquals(
            "SomeOtherName",
            resolveColumnName(
                singularField,
                Optional.of(SpannerFieldOptions.newBuilder()
                        .setColumn("SomeOtherName")
                        .build()),
                Optional.empty(),
                SpannerDriverSettings.DEFAULTS
            ),
            "`SpannerFieldOptions` should always prevail"
        );

        // any name specified in the `TableFieldOptions` should prevail over the calculated name
        assertEquals(
            "GenericTableFieldName",
            resolveColumnName(
                singularField,
                Optional.empty(),
                Optional.of(TableFieldOptions.newBuilder()
                    .setName("GenericTableFieldName")
                    .build()),
                SpannerDriverSettings.DEFAULTS
            ),
            "`TableFieldOptions` should always prevail over calculated defaults"
        );

        // "preserve field names" should prevail over any calculated names
        assertEquals(
            "double_field",
            resolveColumnName(
                singularField,
                Optional.empty(),
                Optional.empty(),
                new SpannerDriverSettings() {
                    @Nonnull
                    @Override
                    public Boolean preserveFieldNames() {
                        return true;
                    }
                }
            ),
            "setting `preserveFieldNames` should prevail over any calculated names"
        );

        // calculated names should use capitalized JSON names by default (i.e. "Spanner-style names")
        assertEquals(
            "DoubleField",
            resolveColumnName(
                singularField,
                Optional.empty(),
                Optional.empty(),
                SpannerDriverSettings.DEFAULTS
            ),
            "calculated names should use capitalized JSON names by default (i.e. 'Spanner-style names')"
        );

        // calculated names should be identical to ProtoJSON fields when capitalized naming is turned off
        assertEquals(
            "doubleField",
            resolveColumnName(
                singularField,
                Optional.empty(),
                Optional.empty(),
                new SpannerDriverSettings() {
                    @Nonnull
                    @Override
                    public Boolean defaultCapitalizedNames() {
                        return false;
                    }
                }
            ),
            "turning off `defaultCapitalizedNames` should result in standard ProtoJSON field naming"
        );
    }
}
