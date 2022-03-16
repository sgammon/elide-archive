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

import com.google.cloud.spanner.DatabaseId;
import gust.backend.model.PersonRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/** Tests for the {@link SpannerManager}. */
public class SpannerManagerTest {
    @AfterAll
    public static void shutDownAllManagers() {
        SpannerManager.acquire().close();
    }

    @Test public void testAcquireSpannerManager() {
        assertNotNull(SpannerManager.acquire(),
                "should be able to acquire a `SpannerManager` with no args");
        assertSame(SpannerManager.acquire(), SpannerManager.acquire(),
                "should get singleton on multiple calls to `acquire`");
    }

    @Test public void testConfigureSpannerManager() {
        var manager = SpannerManager
            .acquire()
            .configureForDatabase(DatabaseId.of(
                "sample-project",
                "instance",
                "database"
            )).build();

        assertNotNull(manager, "should not get `null` for configured manager");
        // note: intentionally not cleaning up so it gets caught by `AfterAll`
    }

    @Test public void testGetAdapterFromManager() {
        var manager = SpannerManager
                .acquire()
                .configureForDatabase(DatabaseId.of(
                        "sample-project",
                        "instance",
                        "database"
                )).build();

        assertNotNull(manager, "should not get `null` for configured manager");
        var adapter = manager.adapter(
            PersonRecord.PersonKey.getDefaultInstance(),
            PersonRecord.Person.getDefaultInstance()
        );

        assertNotNull(adapter, "should be able to acquire adapter from configured manager");
        assertDoesNotThrow(manager::close);
    }

    @Test public void testGetCachedAdapterFromManager() {
        var manager = SpannerManager
                .acquire()
                .configureForDatabase(DatabaseId.of(
                        "sample-project",
                        "instance",
                        "database"
                )).build();

        assertNotNull(manager, "should not get `null` for configured manager");
        var adapter = manager.adapter(
            PersonRecord.PersonKey.getDefaultInstance(),
            PersonRecord.Person.getDefaultInstance()
        );

        assertNotNull(adapter, "should be able to acquire adapter from configured manager");

        var identical = manager.adapter(
            PersonRecord.PersonKey.getDefaultInstance(),
            PersonRecord.Person.getDefaultInstance()
        );

        assertNotNull(identical, "should be able to acquire adapter from configured manager");
        assertSame(adapter, identical, "cached adapters should be identical");
        assertDoesNotThrow(manager::close);
    }

    @Test public void testNoCacheBleed() {
        var manager1 = SpannerManager
                .acquire()
                .configureForDatabase(DatabaseId.of(
                        "sample-project",
                        "instance",
                        "database"
                )).build();

        var manager2 = SpannerManager
                .acquire()
                .configureForDatabase(DatabaseId.of(
                        "sample-project",
                        "instance",
                        "database"
                )).build();

        assertNotNull(manager1, "should not get `null` for configured manager");
        assertNotNull(manager2, "should not get `null` for configured manager");
        var adapter1 = manager1.adapter(
                PersonRecord.PersonKey.getDefaultInstance(),
                PersonRecord.Person.getDefaultInstance()
        );
        var adapter2 = manager2.adapter(
                PersonRecord.PersonKey.getDefaultInstance(),
                PersonRecord.Person.getDefaultInstance()
        );

        assertNotNull(adapter1, "should be able to acquire adapter from configured manager");
        assertNotNull(adapter2, "should be able to acquire adapter from configured manager");

        var identical1 = manager1.adapter(
                PersonRecord.PersonKey.getDefaultInstance(),
                PersonRecord.Person.getDefaultInstance()
        );
        var identical2 = manager2.adapter(
                PersonRecord.PersonKey.getDefaultInstance(),
                PersonRecord.Person.getDefaultInstance()
        );

        assertNotNull(identical1, "should be able to acquire adapter from configured manager");
        assertNotNull(identical2, "should be able to acquire adapter from configured manager");
        assertSame(adapter2, identical2, "cached adapters should be identical");

        assertNotSame(adapter1, identical2, "cross-manager adapters should never be cached together");
        assertNotSame(adapter2, identical1, "cross-manager adapters should never be cached together");
        assertDoesNotThrow(manager1::close);
        assertDoesNotThrow(manager2::close);
    }

    @Test public void testManagerRepeatedlyCallClose() {
        var manager = SpannerManager
                .acquire()
                .configureForDatabase(DatabaseId.of(
                        "sample-project",
                        "instance",
                        "database"
                )).build();
        assertDoesNotThrow(manager::close);
        assertDoesNotThrow(manager::close);
        assertDoesNotThrow(manager::close);
        assertDoesNotThrow(manager::close);
    }

    @Test public void testClosedManagersCannotSpawn() {
        var manager = SpannerManager
                .acquire()
                .configureForDatabase(DatabaseId.of(
                        "sample-project",
                        "instance",
                        "database"
                )).build();

        var adapter = manager.adapter(
            PersonRecord.PersonKey.getDefaultInstance(),
            PersonRecord.Person.getDefaultInstance()
        );

        assertNotNull(adapter, "should be able to spawn regular adapter before close");
        assertDoesNotThrow(manager::close);
        assertThrows(IllegalStateException.class, () -> manager.adapter(
            PersonRecord.PersonKey.getDefaultInstance(),
            PersonRecord.Person.getDefaultInstance()
        ), "should not be able to spawn adapters from a closed manager");
    }

    @Test public void testAutoCloseManager() {
        try (var manager = SpannerManager
                .acquire()
                .configureForDatabase(DatabaseId.of(
                        "sample-project",
                        "instance",
                        "database"
                )).build()) {
            assertNotNull(manager, "should be able to acquire auto-closeable manager");
            assertFalse(manager.getClosed(), "manager should not be closed while in auto-close");
            assertEquals("database", manager.getDatabase().getDatabase(),
                    "database should be expected value for bound manager");
        }

        var manager = SpannerManager
                .acquire()
                .configureForDatabase(DatabaseId.of(
                        "sample-project",
                        "instance",
                        "database"
                )).build();
        try (manager) {
            assertNotNull(manager, "should be able to acquire auto-closeable manager");
            assertFalse(manager.getClosed(), "manager should not be closed while in auto-close");
        } finally {
            assertTrue(manager.getClosed(), "manager should auto-close after main block");
        }
    }

    @Test public void testAllManagersShouldBeAccurate() {
        var manager = SpannerManager
                .acquire()
                .configureForDatabase(DatabaseId.of(
                        "sample-project",
                        "instance",
                        "database"
                )).build();
        assertFalse(SpannerManager.allManagers().isEmpty(),
                "spawning a manager should be sufficient to show up in `allManagers`");
        manager.close();
    }
}
