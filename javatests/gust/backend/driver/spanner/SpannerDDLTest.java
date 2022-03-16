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

import gust.backend.model.PersonRecord;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static gust.backend.driver.spanner.SpannerGeneratedDDL.*;
import static com.google.common.truth.Truth.assertWithMessage;


/** Tests for DDL utilities related to Spanner. */
public final class SpannerDDLTest {
    private void generatorAssertions(@Nonnull SpannerGeneratedDDL generator,
                                     @Nonnull String tableName) {
        assertNotNull(generator,
                "should be able to resolve a schema generator for an arbitrary model interleaved in a parent");
        assertEquals(tableName, generator.getTableName(),
                "generated DDL statement table name should match expected value");
        assertNotNull(generator.getModel(), "should be able to get the model matching the generator");
        assertNotNull(generator.toString(), "`toString()` on generator should not return `null`");
        assertFalse(generator.getColumns().isEmpty(),
                "generated DDL statement column set should not be empty");
    }

    @Test public void testGenerateDDL() {
        var generator = SpannerGeneratedDDL.generateTableDDL(
            PersonRecord.Person.getDefaultInstance(),
            Optional.empty()
        );

        assertNotNull(generator, "should be able to resolve a schema generator for an arbitrary model");
    }

    @Test public void testGeneratePersonDDL() {
        var generator = SpannerGeneratedDDL.generateTableDDL(
            PersonRecord.Person.getDefaultInstance(),
            Optional.empty()
        ).build();

        var expectedBasicCreate = (
            "CREATE TABLE People (" +
                "ID STRING(240) NOT NULL, " +
                "Name STRING(1024), " +
                "ContactInfo STRING(2048)" +
            ") PRIMARY KEY (ID ASC)"
        );

        generatorAssertions(generator, "People");
        assertWithMessage("generated DDL statement should match expected output")
                .that(generator.getGeneratedStatement().toString())
                .isEqualTo(expectedBasicCreate);
    }

    @Test public void testGeneratePersonDDLInterleaved() {
        var generator = SpannerGeneratedDDL.generateTableDDL(
                PersonRecord.Person.getDefaultInstance(),
                Optional.empty())
                .setInterleaveTarget(Optional.of(InterleaveTarget
                    .forParent("ContactList")))
                .build();

        var expectedInterleavedCreate = (
            "CREATE TABLE People (" +
                "ID STRING(240) NOT NULL, " +
                "Name STRING(1024), " +
                "ContactInfo STRING(2048)" +
            ") " +
                "PRIMARY KEY (ID ASC), " +
                "INTERLEAVE IN PARENT ContactList"
        );

        generatorAssertions(generator, "People");
        assertWithMessage("generated DDL statement should match expected output")
                .that(generator.getGeneratedStatement().toString())
                .isEqualTo(expectedInterleavedCreate);
    }

    @Test public void testGeneratePersonDescendingKey() {
        var generator = SpannerGeneratedDDL.generateTableDDL(
                PersonRecord.Person.getDefaultInstance(),
                Optional.empty())
                .setKeySortDirection(SortDirection.DESC)
                .build();

        var expectedInterleavedCreate = (
            "CREATE TABLE People (" +
                "ID STRING(240) NOT NULL, " +
                "Name STRING(1024), " +
                "ContactInfo STRING(2048)" +
            ") " +
                "PRIMARY KEY (ID DESC)"
        );

        generatorAssertions(generator, "People");
        assertWithMessage("generated DDL statement should match expected output")
                .that(generator.getGeneratedStatement().toString())
                .isEqualTo(expectedInterleavedCreate);
    }

    @Test public void testGenerateTypeBuffet() {
        var generator = SpannerGeneratedDDL.generateTableDDL(
                PersonRecord.TypeBuffet.getDefaultInstance(),
                Optional.empty()
        ).build();

        var expectedBuffetTable = (
            "CREATE TABLE TypeExamples (" +
                "ID INT64 NOT NULL, " +
                "IntNormal INT64, " +
                "IntDouble INT64, " +
                "UintNormal INT64, " +
                "UintDouble INT64, " +
                "SintNormal INT64, " +
                "SintDouble INT64, " +
                "FixedNormal INT64, " +
                "FixedDouble INT64, " +
                "SfixedNormal INT64, " +
                "SfixedDouble INT64, " +
                "StringField STRING(2048), " +
                "BoolField BOOL, " +
                "BytesField BYTES(2048), " +
                "FloatField FLOAT64, " +
                "DoubleField FLOAT64, " +
                "EnumField STRING(32), " +
                "Labels ARRAY<STRING(240)>, " +
                "SpannerNumericField NUMERIC, " +
                "Timestamp TIMESTAMP, " +
                "Date DATE" +
            ") PRIMARY KEY (ID ASC)"
        );

        generatorAssertions(generator, "TypeExamples");
        assertWithMessage("generated DDL statement should match expected output")
                .that(generator.getGeneratedStatement().toString())
                .isEqualTo(expectedBuffetTable);
    }
}
