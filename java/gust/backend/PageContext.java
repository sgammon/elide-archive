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
import com.google.template.soy.msgs.SoyMsgBundle;
import io.micronaut.views.soy.SoyContext;
import io.micronaut.views.soy.SoyNamingMapProvider;
import tools.elide.page.Context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;


/**
 * Supplies page context to a Micronaut/Soy render routine, based on the context proto provided/filled out by a given
 * server-side controller.
 *
 * <p>Because this flow occurs in two stages (i.e. building or calculating context, then, subsequently, rendering
 * context), the logic here is implemented to be entirely immutable, and so this object should be used in a transitory
 * way to mediate between a single Soy routine and the context attached to it.</p>
 */
@Immutable
@SuppressWarnings({"unused", "WeakerAccess"})
public final class PageContext implements PageRender {
  /** Shared singleton instance of an empty page context. */
  private static final PageContext _EMPTY = new PageContext(
    Context.getDefaultInstance(),
    null,
    null,
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
   * @param i18n Internationalization context to apply.
   * @param delegatePredicate Predicate for selecting a Soy delegate package, if applicable.
   */
  private PageContext(@Nullable Context proto,
                      @Nullable Map<String, Object> context,
                      @Nullable Map<String, Object> injected,
                      @Nullable SoyNamingMapProvider namingMapProvider,
                      @Nullable SoyContext.SoyI18NContext i18n,
                      @Nullable Predicate<String> delegatePredicate) {
    this.protoContext = proto != null ? proto : Context.getDefaultInstance();
    this.rawContext = SoyContext.fromMap(
      context != null ? context : Collections.emptyMap(),
      Optional.ofNullable(injected),
      Optional.ofNullable(namingMapProvider),
      Optional.ofNullable(i18n),
      Optional.ofNullable(delegatePredicate));
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
    return new PageContext(null, context, null, null, null, null);
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
    return new PageContext(null, context, injected, null, null, null);
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
    return new PageContext(null, context, injected, namingMapProvider, null, null);
  }

  /**
   * Factory to create a page context object from a regular Java map, of string context properties to values of any
   * object type. Additionally, this interface allows specification of properties declared via <pre>@inject</pre>, and
   * also a {@link SoyNamingMapProvider} to override any globally-installed map. Under the hood, all context is
   * processed and converted/wrapped into Soy values.
   *
   * <p>Note that style rewriting must be enabled for the <pre>namingMapProvider</pre> override to take effect.</p>
   *
   * <p>If the invoking developer wishes to apply internationalization via an XLIFF message bundle or pre-constructed
   * Soy Messages bundle, they may do so via `i18n`.</p>
   *
   * @param context Context with which to render a Soy template - i.e. regular <pre>@param</pre> declarations.
   * @param injected Injected parameters to provide to the render operation - available via <pre>@inject</pre>.
   * @param namingMapProvider Override any globally-installed naming map provider.
   * @param i18n Internationalization context to apply.
   * @param delegatePredicate Predicate for selecting a delegate package.
   * @return Fabricated page context object.
   */
  public static PageContext fromMap(@Nonnull Map<String, Object> context,
                                    @Nonnull Map<String, Object> injected,
                                    @Nullable SoyNamingMapProvider namingMapProvider,
                                    @Nullable SoyContext.SoyI18NContext i18n,
                                    @Nullable Predicate<String> delegatePredicate) {
    return new PageContext(null, context, injected, namingMapProvider, i18n, delegatePredicate);
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
    return new PageContext(
      pageContext,
      null,
      null,
      null,
      null,
      null
    );
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
    return new PageContext(
      pageContext,
      props,
      null,
      null,
      null,
      null
    );
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
    return new PageContext(
      pageContext,
      props,
      injected,
      null,
      null,
      null
    );
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
    return new PageContext(
      pageContext,
      props,
      injected,
      namingMapProvider,
      null,
      null
    );
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
   * <p>In addition to the other method variant above, this method allows internationalization via the `i18n` parameter,
   * which accepts a {@link SoyContext.SoyI18NContext} instance. A messages file, resource, or pre-constructed Soy
   * Message Bundle may be passed for translation during render.</p>
   *
   * @param pageContext Protobuf page context.
   * @param props Parameters to render the template with.
   * @param injected Additional injected values (may not contain a value at key <pre>context</pre>).
   * @param namingMapProvider Naming map provider to override any globally-installed provider with, if enabled.
   * @param i18n Internationalization context to apply.
   * @param delegatePredicate Predicate for selecting a Soy delegate package, as applicable.
   * @return Page context object.
   */
  public static @Nonnull PageContext fromProto(@Nonnull Context pageContext,
                                               @Nonnull Map<String, Object> props,
                                               @Nonnull Map<String, Object> injected,
                                               @Nullable SoyNamingMapProvider namingMapProvider,
                                               @Nullable SoyContext.SoyI18NContext i18n,
                                               @Nullable Predicate<String> delegatePredicate) {
    return new PageContext(
      pageContext,
      props,
      injected,
      namingMapProvider,
      i18n,
      delegatePredicate
    );
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
   * @param framework Framework-injected properties.
   * @return Map of injected properties and their values.
   */
  @Nonnull @Override
  public Map<String, Object> getInjectedProperties(@Nonnull Map<String, Object> framework) {
    return ImmutableMap
      .<String, Object>builder()
      .put(PAGE_CONTEXT_IJ_NAME, protoContext)
      .putAll(rawContext.getInjectedProperties(framework))
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

  /**
   * Specify whether to enable translation via Soy message bundles. The default implementation of this method simply
   * checks whether a translation file has been set by the controller.
   *
   * @return Whether to enable translation.
   */
  @Override
  public boolean translate() {
    return rawContext.translate();
  }

  /**
   * Return the file that should be loaded and interpreted to perform translation when rendering this page. If no file,
   * resource, or bundle is provided, no translation occurs.
   *
   * @return File to apply for translations.
   */
  @Nonnull @Override
  public Optional<File> messagesFile() {
    return rawContext.messagesFile();
  }

  /**
   * Return the URL to the resource that should be loaded and interpreted to perform translation when rendering this
   * page. If no resource, file, or bundle is provided, no translation occurs.
   *
   * @return Resource to apply for translations.
   */
  @Nonnull @Override
  public Optional<URL> messagesResource() {
    return rawContext.messagesResource();
  }

  /**
   * Return the pre-fabricated Soy message bundle, or the interpreted Soy message bundle based on the installed messages
   * file or resource URL.
   *
   * @return Pre-fabricated or interpreted message bundle.
   * @throws IOException If the bundle failed to load.
   */
  @Nonnull @Override
  public Optional<SoyMsgBundle> messageBundle() throws IOException {
    return rawContext.messageBundle();
  }

  /**
   * Return the delegate package that should be active when rendering the desired Soy output, if applicable. If no
   * delegate package should be applied, an empty {@link Optional} is returned.
   *
   * @return Predicate specifying the delegate package, or {@link Optional#empty()}.
   */
  @Nonnull @Override
  public Optional<Predicate<String>> delegatePackage() {
    return rawContext.delegatePackage();
  }
}
