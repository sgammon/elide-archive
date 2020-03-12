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
package gust.backend;

import com.google.common.collect.ImmutableMap;
import com.google.template.soy.shared.SoyCssRenamingMap;
import com.google.template.soy.shared.SoyIdRenamingMap;
import io.micronaut.views.soy.SoyNamingMapProvider;
import org.junit.jupiter.api.Test;
import tools.elide.page.Context;

import javax.annotation.Nullable;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;


/** Tests the {@link PageContext} class for various behavioral contracts. */
public class PageContextTest {
  /** Test that a simple map context works fine with {@link PageContext}. */
  @Test void testMapContext() {
    String hiprop = "hi";
    PageContext ctx = PageContext.fromMap(Collections.singletonMap(hiprop, 5));
    assertEquals(5, ctx.getProperties().get(hiprop),
      "basic context property should be fetchable");

    PageContext ctx2 = PageContext.fromMap(
      ImmutableMap.of("hi", 10, "hey", 15));
    assertEquals(10, ctx2.getProperties().get("hi"),
      "properties should not blend between instances");
    assertEquals(15, ctx2.getProperties().get("hey"),
      "property map should work fine with multiple entries");
  }

  /** Test that a simple map, and injected props, work fine with {@link PageContext}. */
  @Test void testMapWithInjectedContext() {
    PageContext ctx = PageContext.fromMap(
      Collections.singletonMap("hi", 5),
      Collections.singletonMap("yo", 10));

    assertEquals(5, ctx.getProperties().get("hi"),
      "properties should be available through regular props, when injection is present");
    assertEquals(10, ctx.getInjectedProperties(Collections.emptyMap()).get("yo"),
      "inject properties should be made available");
  }

  /** Test behavior with a context map and renaming map override. */
  @Test void testContextMapWithRenamingOverride() {
    SoyNamingMapProvider overrideMap = new SoyNamingMapProvider() {
      @Nullable @Override
      public SoyCssRenamingMap cssRenamingMap() {
        return null;
      }

      @Nullable @Override
      public SoyIdRenamingMap idRenamingMap() {
        return null;
      }
    };

    PageContext ctx = PageContext.fromMap(
      Collections.singletonMap("hi", 5),
      Collections.singletonMap("yo", 10),
      overrideMap);

    assertEquals(5, ctx.getProperties().get("hi"),
      "properties should be available through regular props, when injection is present");
    assertEquals(10, ctx.getInjectedProperties(Collections.emptyMap()).get("yo"),
      "inject properties should be made available");
    assertTrue(ctx.overrideNamingMap().isPresent(),
      "renaming map should be present when we provide it");
    assertSame(overrideMap, ctx.overrideNamingMap().get(),
      "should get back same soy renaming map override that we provide");
  }

  /** Test basic proto-driven page context with {@link PageContext}. */
  @Test void testSimpleProtoContext() {
    PageContext ctx = PageContext.fromProto(Context.newBuilder()
      .setMeta(Context.Metadata.newBuilder()
        .setTitle("Page Title Here"))
      .build());

    assertEquals("Page Title Here", ctx.getPageContext().getMeta().getTitle(),
      "simple proto context should pass properties through");

    assertEquals("Page Title Here", ((Context)ctx.getInjectedProperties(Collections.emptyMap())
        .get("page")).getMeta().getTitle(),
      "simple proto context should be exposed through `@inject context`");

    assertEquals(((PageRender)ctx).getPageContext(), ctx.getPageContext(),
      "proto context should be valid through interface or object");
  }

  /** Test the default value for renaming map overrides. */
  @Test void testRenamingMapOverrideDefault() {
    PageContext ctx = PageContext.empty();
    assertFalse(ctx.overrideNamingMap().isPresent(),
      "empty page context should default to a null renaming map override");
  }

  /** Test default context maps for an empty page context. */
  @Test void testContextMapDefaults() {
    PageContext ctx = PageContext.empty();
    assertTrue(ctx.getProperties().isEmpty(),
      "empty prop context should default to an empty map");
    assertFalse(ctx.getInjectedProperties(Collections.emptyMap()).isEmpty(),
      "empty inject context should still specify a context proto");
  }

  /** Test default proto context for an empty page context. */
  @Test void testContextProtoDefault() {
    PageContext ctx = PageContext.empty();
    assertSame(Context.getDefaultInstance(), ctx.getPageContext(),
      "should get back the default context instance for an empty page context");
  }

  /** Test proto-based context factory methods. */
  @Test void testContextProtoFactoryMethods() {
    PageContext protoWithProps = PageContext.fromProto(Context.newBuilder()
      .setMeta(Context.Metadata.newBuilder()
        .setTitle("Page Title Here"))
      .build(),
      ImmutableMap.of("hi", 5));

    assertNotNull(protoWithProps,
      "should get a valid context for a proto with props");
    assertEquals("Page Title Here", protoWithProps.getPageContext().getMeta().getTitle(),
      "page title should properly pass through with props present");
    assertEquals("Page Title Here",
      ((Context)protoWithProps.getInjectedProperties(Collections.emptyMap())
        .get("page")).getMeta().getTitle(),
      "page title should properly pass through as injected value with props present");
    assertEquals(5, protoWithProps.getProperties().get("hi"),
      "regular context props should properly pass through with proto-based factory");

    PageContext protoWithPropsAndInjectedValues = PageContext.fromProto(Context.newBuilder()
        .setMeta(Context.Metadata.newBuilder()
          .setTitle("Page Title Here"))
        .build(),
      ImmutableMap.of("hi", 5),
      ImmutableMap.of("yo", 10, "hi", 15));

    assertNotNull(protoWithPropsAndInjectedValues,
      "should get a valid context for a proto with props (+injected)");
    assertEquals("Page Title Here", protoWithPropsAndInjectedValues.getPageContext().getMeta().getTitle(),
      "page title should properly pass through with props present (+injected)");
    assertEquals("Page Title Here",
      ((Context)protoWithPropsAndInjectedValues.getInjectedProperties(Collections.emptyMap())
        .get("page")).getMeta().getTitle(),
      "page title should properly pass through as injected value with props present (+injected)");
    assertEquals(5, protoWithPropsAndInjectedValues.getProperties().get("hi"),
      "regular context props should properly pass through with proto-based factory (+injected)");
    assertEquals(10, protoWithPropsAndInjectedValues
        .getInjectedProperties(Collections.emptyMap()).get("yo"),
      "injected context props should properly pass through with proto-based factory");
    assertEquals(15, protoWithPropsAndInjectedValues
        .getInjectedProperties(Collections.emptyMap()).get("hi"),
      "injected context props should not blend with regular context properties");

    SoyNamingMapProvider overrideMap = new SoyNamingMapProvider() {
      @Nullable @Override
      public SoyCssRenamingMap cssRenamingMap() {
        return null;
      }

      @Nullable @Override
      public SoyIdRenamingMap idRenamingMap() {
        return null;
      }
    };

    PageContext protoWithPropsAndInjectedValuesAndMap = PageContext.fromProto(Context.newBuilder()
        .setMeta(Context.Metadata.newBuilder()
          .setTitle("Page Title Here"))
        .build(),
      ImmutableMap.of("hi", 5),
      ImmutableMap.of("yo", 10, "hi", 15),
      overrideMap);

    assertNotNull(protoWithPropsAndInjectedValuesAndMap,
      "should get a valid context for a proto with props (+injected and map)");
    assertEquals("Page Title Here",
      protoWithPropsAndInjectedValuesAndMap.getPageContext().getMeta().getTitle(),
      "page title should properly pass through with props present (+injected and map)");
    assertEquals("Page Title Here",
      ((Context)protoWithPropsAndInjectedValuesAndMap
        .getInjectedProperties(Collections.emptyMap()).get("page")).getMeta().getTitle(),
      "page title should properly pass through as injected value with props present (+injected and map)");
    assertEquals(5, protoWithPropsAndInjectedValuesAndMap.getProperties().get("hi"),
      "regular context props should properly pass through with proto-based factory (+injected and map)");
    assertEquals(10, protoWithPropsAndInjectedValuesAndMap
        .getInjectedProperties(Collections.emptyMap()).get("yo"),
      "injected context props should properly pass through with proto-based factory (+map)");
    assertEquals(15, protoWithPropsAndInjectedValuesAndMap
        .getInjectedProperties(Collections.emptyMap()).get("hi"),
      "injected context props should not blend with regular context properties (+map)");
    assertTrue(protoWithPropsAndInjectedValuesAndMap.overrideNamingMap().isPresent(),
      "rewrite map should show as present if provided");
    assertSame(overrideMap, protoWithPropsAndInjectedValuesAndMap.overrideNamingMap().get(),
      "providing rewrite map override through proto context factories should pass-through");
  }
}
