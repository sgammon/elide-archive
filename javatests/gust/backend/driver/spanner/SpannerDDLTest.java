package gust.backend.driver.spanner;

import gust.backend.model.PersonRecord;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static gust.backend.driver.spanner.SpannerGeneratedDDL.*;


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
                "Key STRING(240) NOT NULL, " +
                "Name STRING(1024), " +
                "ContactInfo STRING(2048)" +
            ") PRIMARY KEY (Key ASC)"
        );

        generatorAssertions(generator, "People");
        assertEquals(expectedBasicCreate, generator.getGeneratedStatement().toString(),
                "generated DDL statement should match expected output");
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
                "Key STRING(240) NOT NULL, " +
                "Name STRING(1024), " +
                "ContactInfo STRING(2048)" +
            ") " +
                "PRIMARY KEY (Key ASC), " +
                "INTERLEAVE IN PARENT ContactList"
        );

        generatorAssertions(generator, "People");
        assertEquals(expectedInterleavedCreate, generator.getGeneratedStatement().toString(),
                "generated DDL statement should match expected output");
    }

    @Test public void testGeneratePersonDescendingKey() {
        var generator = SpannerGeneratedDDL.generateTableDDL(
                PersonRecord.Person.getDefaultInstance(),
                Optional.empty())
                .setKeySortDirection(SortDirection.DESC)
                .build();

        var expectedInterleavedCreate = (
            "CREATE TABLE People (" +
                "Key STRING(240) NOT NULL, " +
                "Name STRING(1024), " +
                "ContactInfo STRING(2048)" +
            ") " +
                "PRIMARY KEY (Key DESC)"
        );

        generatorAssertions(generator, "People");
        assertEquals(expectedInterleavedCreate, generator.getGeneratedStatement().toString(),
                "generated DDL statement should match expected output");
    }

    @Test public void testGenerateTypeBuffet() {
        var generator = SpannerGeneratedDDL.generateTableDDL(
                PersonRecord.TypeBuffet.getDefaultInstance(),
                Optional.empty()
        ).build();

        var expectedBuffetTable = (
            "CREATE TABLE TypeExample (" +
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
                "BytesField BYTES, " +
                "FloatField FLOAT64, " +
                "DoubleField FLOAT64, " +
                "EnumField STRING(2048), " +
                "Labels ARRAY" +
            ") PRIMARY KEY (ID ASC)"
        );

        generatorAssertions(generator, "TypeExample");
        assertEquals(expectedBuffetTable, generator.getGeneratedStatement().toString(),
                "generated DDL statement should match expected output");
    }
}
