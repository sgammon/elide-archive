package javatests.gust.backend;

import com.google.common.collect.ImmutableMap;
import com.google.template.soy.shared.SoyCssRenamingMap;
import com.google.template.soy.shared.SoyIdRenamingMap;
import gust.backend.PageContext;
import gust.backend.SoyProtoContextMediator;
import io.micronaut.views.soy.SoyNamingMapProvider;
import org.junit.Test;
import tools.elide.page.Context;

import javax.annotation.Nullable;
import java.util.Collections;

import static org.junit.Assert.*;


/** Tests the {@link PageContext} class for various behavioral contracts. */
public class PageContextTest {
  /** Test that a simple map context works fine with {@link PageContext}. */
  @Test public void testMapContext() {
    String hiprop = "hi";
    PageContext ctx = PageContext.fromMap(Collections.singletonMap(hiprop, 5));
    assertEquals("basic context property should be fetchable",
      5, ctx.getProperties().get(hiprop));

    PageContext ctx2 = PageContext.fromMap(
      ImmutableMap.of("hi", 10, "hey", 15));
    assertEquals("properties should not blend between instances",
      10, ctx2.getProperties().get("hi"));
    assertEquals("property map should work fine with multiple entries",
      15, ctx2.getProperties().get("hey"));
  }

  /** Test that a simple map, and injected props, work fine with {@link PageContext}. */
  @Test public void testMapWithInjectedContext() {
    PageContext ctx = PageContext.fromMap(
      Collections.singletonMap("hi", 5),
      Collections.singletonMap("yo", 10));

    assertEquals("properties should be available through regular props, when injection is present",
      5, ctx.getProperties().get("hi"));
    assertEquals("inject properties should be made available",
      10, ctx.getInjectedProperties().get("yo"));
  }

  /** Test behavior with a context map and renaming map override. */
  @Test public void testContextMapWithRenamingOverride() {
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

    assertEquals("properties should be available through regular props, when injection is present",
      5, ctx.getProperties().get("hi"));
    assertEquals("inject properties should be made available",
      10, ctx.getInjectedProperties().get("yo"));
    assertTrue("renaming map should be present when we provide it",
      ctx.overrideNamingMap().isPresent());
    assertSame("should get back same soy renaming map override that we provide",
      overrideMap,
      ctx.overrideNamingMap().get());
  }

  /** Test basic proto-driven page context with {@link PageContext}. */
  @Test public void testSimpleProtoContext() {
    PageContext ctx = PageContext.fromProto(Context.newBuilder()
      .setMeta(Context.Metadata.newBuilder()
        .setTitle("Page Title Here"))
      .build());

    assertEquals("simple proto context should pass properties through",
      "Page Title Here",
      ctx.getPageContext().getMeta().getTitle());

    assertEquals("simple proto context should be exposed through `@inject context`",
      "Page Title Here",
      ((Context)ctx.getInjectedProperties().get("context")).getMeta().getTitle());

    assertEquals("proto context should be valid through interface or object",
      ((SoyProtoContextMediator)ctx).getPageContext(),
      ctx.getPageContext());
  }

  /** Test the default value for renaming map overrides. */
  @Test public void testRenamingMapOverrideDefault() {
    PageContext ctx = PageContext.empty();
    assertFalse("empty page context should default to a null renaming map override",
      ctx.overrideNamingMap().isPresent());
  }

  /** Test default context maps for an empty page context. */
  @Test public void testContextMapDefaults() {
    PageContext ctx = PageContext.empty();
    assertTrue("empty prop context should default to an empty map",
      ctx.getProperties().isEmpty());
    assertFalse("empty inject context should still specify a context proto",
      ctx.getInjectedProperties().isEmpty());
  }

  /** Test default proto context for an empty page context. */
  @Test public void testContextProtoDefault() {
    PageContext ctx = PageContext.empty();
    assertSame("should get back the default context instance for an empty page context",
      Context.getDefaultInstance(),
      ctx.getPageContext());
  }

  /** Test proto-based context factory methods. */
  @Test public void testContextProtoFactoryMethods() {
    PageContext protoWithProps = PageContext.fromProto(Context.newBuilder()
      .setMeta(Context.Metadata.newBuilder()
        .setTitle("Page Title Here"))
      .build(),
      ImmutableMap.of("hi", 5));

    assertNotNull("should get a valid context for a proto with props",
      protoWithProps);
    assertEquals("page title should properly pass through with props present",
      "Page Title Here",
      protoWithProps.getPageContext().getMeta().getTitle());
    assertEquals("page title should properly pass through as injected value with props present",
      "Page Title Here",
      ((Context)protoWithProps.getInjectedProperties().get("context")).getMeta().getTitle());
    assertEquals("regular context props should properly pass through with proto-based factory",
      5,
        protoWithProps.getProperties().get("hi"));

    PageContext protoWithPropsAndInjectedValues = PageContext.fromProto(Context.newBuilder()
        .setMeta(Context.Metadata.newBuilder()
          .setTitle("Page Title Here"))
        .build(),
      ImmutableMap.of("hi", 5),
      ImmutableMap.of("yo", 10, "hi", 15));

    assertNotNull("should get a valid context for a proto with props (+injected)",
      protoWithPropsAndInjectedValues);
    assertEquals("page title should properly pass through with props present (+injected)",
      "Page Title Here",
      protoWithPropsAndInjectedValues.getPageContext().getMeta().getTitle());
    assertEquals("page title should properly pass through as injected value with props present (+injected)",
      "Page Title Here",
      ((Context)protoWithPropsAndInjectedValues.getInjectedProperties().get("context")).getMeta().getTitle());
    assertEquals("regular context props should properly pass through with proto-based factory (+injected)",
      5,
      protoWithPropsAndInjectedValues.getProperties().get("hi"));
    assertEquals("injected context props should properly pass through with proto-based factory",
      10,
      protoWithPropsAndInjectedValues.getInjectedProperties().get("yo"));
    assertEquals("injected context props should not blend with regular context properties",
      15,
      protoWithPropsAndInjectedValues.getInjectedProperties().get("hi"));

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

    assertNotNull("should get a valid context for a proto with props (+injected and map)",
      protoWithPropsAndInjectedValuesAndMap);
    assertEquals("page title should properly pass through with props present (+injected and map)",
      "Page Title Here",
      protoWithPropsAndInjectedValuesAndMap.getPageContext().getMeta().getTitle());
    assertEquals("page title should properly pass through as injected value with props present (+injected and map)",
      "Page Title Here",
      ((Context)protoWithPropsAndInjectedValuesAndMap.getInjectedProperties().get("context")).getMeta().getTitle());
    assertEquals("regular context props should properly pass through with proto-based factory (+injected and map)",
      5,
      protoWithPropsAndInjectedValuesAndMap.getProperties().get("hi"));
    assertEquals("injected context props should properly pass through with proto-based factory (+map)",
      10,
      protoWithPropsAndInjectedValuesAndMap.getInjectedProperties().get("yo"));
    assertEquals("injected context props should not blend with regular context properties (+map)",
      15,
      protoWithPropsAndInjectedValuesAndMap.getInjectedProperties().get("hi"));
    assertTrue("rewrite map should show as present if provided",
      protoWithPropsAndInjectedValuesAndMap.overrideNamingMap().isPresent());
    assertSame("providing rewrite map override through proto context factories should pass-through",
      overrideMap,
      protoWithPropsAndInjectedValuesAndMap.overrideNamingMap().get());
  }
}
