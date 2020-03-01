package gust.backend;

import com.google.common.collect.ImmutableMap;
import io.micronaut.views.soy.SoyContext;
import io.micronaut.views.soy.SoyNamingMapProvider;
import tools.elide.page.Context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;


/**
 * Supplies page context to a Micronaut/Soy render routine, based on the context proto provided/filled out by a given
 * server-side controller.
 *
 * <p>Because this flow occurs in two stages (i.e. building or calculating context, then, subsequently, rendering
 * context), the logic here is implemented to be entirely immutable, and so this object should be used in a transitory
 * way to mediate between a single Soy routine and the context attached to it.</p>
 */
@Immutable
@SuppressWarnings("unused")
public final class PageContext implements PageRender {
  /** Name at which proto-context is injected. */
  private static final String CONTEXT_PROPERTY_NAME = "context";

  /** Shared singleton instance of an empty page context. */
  private static final PageContext _EMPTY = new PageContext(
    Context.getDefaultInstance(),
    null,
    null,
    null);

  /** Raw context. */
  private final @Nonnull SoyContext rawContext;

  /** Proto-based context. */
  private final @Nonnull Context protoContext;

  /**
   * Private constructor for proto-based page context with an option for additional regular <pre>@param</pre> props, or
   * additional <pre>@inject</pre> values, and/or an override naming map provider.
   *
   * @param proto Context proto to inject with this render operation.
   * @param context Map of page context information.
   * @param injected Additional injected properties to apply.
   * @param namingMapProvider Style rewrite naming provider to apply/override (if enabled).
   */
  private PageContext(@Nullable Context proto,
                      @Nullable Map<String, Object> context,
                      @Nullable Map<String, Object> injected,
                      @Nullable SoyNamingMapProvider namingMapProvider) {
    this.protoContext = proto != null ? proto : Context.getDefaultInstance();
    this.rawContext = SoyContext.fromMap(
      context != null ? context : Collections.emptyMap(),
      injected != null ? Optional.of(injected) : Optional.empty(),
      namingMapProvider != null ? Optional.of(namingMapProvider) : Optional.empty());
  }

  // -- Factories: Maps -- //

  /**
   * Factory to create an empty page context. Under the hood, this uses a static singleton representing an empty context
   * to avoid re-creating the object repeatedly.
   *
   * @return Pre-fabricated empty page context.
   */
  public static PageContext empty() {
    return PageContext._EMPTY;
  }

  /**
   * Factory to create a page context object from a regular Java map, of string context properties to values of any
   * object type. Under the hood, this is processed and converted/wrapped into Soy values.
   *
   * @param context Context with which to render a Soy template.
   * @return Instance of page context, enclosing the provided context.
   */
  public static PageContext fromMap(@Nonnull Map<String, Object> context) {
    return new PageContext(null, context, null, null);
  }

  /**
   * Factory to create a page context object from a regular Java map, of string context properties to values of any
   * object type. Additionally, this interface allows specification of properties declared via <pre>@inject</pre>. Under
   * the hood, all context is processed and converted/wrapped into Soy values.
   *
   * @param context Context with which to render a Soy template - i.e. regular <pre>@param</pre> declarations.
   * @param injected Injected parameters to provide to the render operation - available via <pre>@inject</pre>.
   * @return Fabricated page context object.
   */
  public static PageContext fromMap(@Nonnull Map<String, Object> context,
                                    @Nonnull Map<String, Object> injected) {
    return new PageContext(null, context, injected, null);
  }

  /**
   * Factory to create a page context object from a regular Java map, of string context properties to values of any
   * object type. Additionally, this interface allows specification of properties declared via <pre>@inject</pre>, and
   * also a {@link SoyNamingMapProvider} to override any globally-installed map. Under the hood, all context is
   * processed and converted/wrapped into Soy values.
   *
   * <p>Note that style rewriting must be enabled for the <pre>namingMapProvider</pre> override to take effect.</p>
   *
   * @param context Context with which to render a Soy template - i.e. regular <pre>@param</pre> declarations.
   * @param injected Injected parameters to provide to the render operation - available via <pre>@inject</pre>.
   * @param namingMapProvider Override any globally-installed naming map provider.
   * @return Fabricated page context object.
   */
  public static PageContext fromMap(@Nonnull Map<String, Object> context,
                                    @Nonnull Map<String, Object> injected,
                                    @Nullable SoyNamingMapProvider namingMapProvider) {
    return new PageContext(null, context, injected, namingMapProvider);
  }

  // -- Factories: Protos -- //

  /**
   * Factory to create a page context object from a proto message containing structured data, which is injected into the
   * render flow at `context`. Templates may opt-in to receive this value via a parameter declaration such as
   * <pre>@inject context: gust.page.Context</pre>.
   *
   * @param pageContext Protobuf page context.
   * @return Page context object.
   */
  public static @Nonnull PageContext fromProto(@Nonnull Context pageContext) {
    return new PageContext(pageContext, null, null, null);
  }

  /**
   * Factory to create a page context object from a proto message containing structured data, which is injected into the
   * render flow at `context`. Templates may opt-in to receive this value via a parameter declaration such as
   * <pre>@inject context: gust.page.Context</pre>.
   *
   * <p>This method offers the additional ability to specify <pre>props</pre>, which should correspond with any
   * <pre>@param</pre> declarations for the subject template to be rendered.</p>
   *
   * @param pageContext Protobuf page context.
   * @param props Parameters to render the template with.
   * @return Page context object.
   */
  public static @Nonnull PageContext fromProto(@Nonnull Context pageContext,
                                               @Nonnull Map<String, Object> props) {
    return new PageContext(pageContext, props, null, null);
  }

  /**
   * Factory to create a page context object from a proto message containing structured data, which is injected into the
   * render flow at `context`. Templates may opt-in to receive this value via a parameter declaration such as
   * <pre>@inject context: gust.page.Context</pre>.
   *
   * <p>This method offers the additional ability to specify <pre>props</pre> and <pre>injected</pre> values. Props
   * should correspond with any <pre>@param</pre> declarations for the subject template to be rendered. Injected values
   * are opted-into with <pre>@inject</pre>, and are overlaid on any existing injected values (may not override
   * <pre>context</pre>).</p>
   *
   * @param pageContext Protobuf page context.
   * @param props Parameters to render the template with.
   * @param injected Additional injected values (may not contain a value at key <pre>context</pre>).
   * @return Page context object.
   */
  public static @Nonnull PageContext fromProto(@Nonnull Context pageContext,
                                               @Nonnull Map<String, Object> props,
                                               @Nonnull Map<String, Object> injected) {
    return new PageContext(pageContext, props, injected, null);
  }

  /**
   * Factory to create a page context object from a proto message containing structured data, which is injected into the
   * render flow at `context`. Templates may opt-in to receive this value via a parameter declaration such as
   * <pre>@inject context: gust.page.Context</pre>.
   *
   * <p>This method offers the additional ability to specify <pre>props</pre> and <pre>injected</pre> values. Props
   * should correspond with any <pre>@param</pre> declarations for the subject template to be rendered. Injected values
   * are opted-into with <pre>@inject</pre>, and are overlaid on any existing injected values (may not override
   * <pre>context</pre>).</p>
   *
   * <p>If desired, an invoking developer may wish to specify a <pre>namingMapProvider</pre>. To have any effect, style
   * renaming must be active in application config. The naming map provider passed here overrides any globally-installed
   * style renaming map provider.</p>
   *
   * @param pageContext Protobuf page context.
   * @param props Parameters to render the template with.
   * @param injected Additional injected values (may not contain a value at key <pre>context</pre>).
   * @param namingMapProvider Naming map provider to override any globally-installed provider with, if enabled.
   * @return Page context object.
   */
  public static @Nonnull PageContext fromProto(@Nonnull Context pageContext,
                                               @Nonnull Map<String, Object> props,
                                               @Nonnull Map<String, Object> injected,
                                               @Nullable SoyNamingMapProvider namingMapProvider) {
    return new PageContext(pageContext, props, injected, namingMapProvider);
  }

  // -- Interface: Soy Proto Context -- //

  /**
   * Retrieve serializable server-side-rendered page context, which should be assigned to the render flow bound to this
   * context mediator.
   *
   * @return Server-side rendered page context.
   */
  @Nonnull @Override
  public Context getPageContext() {
    return this.protoContext;
  }

  // -- Interface: Soy Context Mediation -- //

  /**
   * Retrieve properties which should be made available via regular, declared `@param` statements.
   *
   * @return Map of regular template properties.
   */
  @Nonnull @Override
  public Map<String, Object> getProperties() {
    return rawContext.getProperties();
  }

  /**
   * Retrieve properties and values that should be made available via `@inject`.
   *
   * @return Map of injected properties and their values.
   */
  @Nonnull @Override
  public Map<String, Object> getInjectedProperties() {
    return ImmutableMap
      .<String, Object>builder()
      .put(CONTEXT_PROPERTY_NAME, protoContext)
      .putAll(rawContext.getInjectedProperties())
      .build();
  }

  /**
   * Specify a Soy renaming map which overrides the globally-installed map, if any. Renaming must still be activated via
   * config, or manually, for the return value of this method to have any effect.
   *
   * @return {@link SoyNamingMapProvider} that should be used for this render routine.
   */
  @Nonnull @Override
  public Optional<SoyNamingMapProvider> overrideNamingMap() {
    return rawContext.overrideNamingMap();
  }
}
