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

import com.google.common.base.Joiner;
import com.google.common.html.types.TrustedResourceUrlProto;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.template.soy.data.SanitizedContent;
import com.google.template.soy.data.UnsafeSanitizedContentOrdainer;
import gust.backend.runtime.AssetManager;
import gust.backend.runtime.AssetManager.ManagedAsset;
import gust.backend.runtime.Logging;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.runtime.http.scope.RequestScope;
import io.micronaut.views.soy.SoyNamingMapProvider;
import org.slf4j.Logger;
import tools.elide.assets.AssetBundle.StyleBundle.StyleAsset;
import tools.elide.assets.AssetBundle.ScriptBundle.ScriptAsset;
import tools.elide.page.Context;
import tools.elide.page.Context.Styles.Stylesheet;
import tools.elide.page.Context.Scripts.JavaScript;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;


/**
 * Manages the process of filling out {@link PageContext} objects before they are sealed, and delivered to Closure/Soy
 * to be reduced and rendered into content.
 *
 * <p>This object may be used from controllers via dependency injection, or used via the base controller classes
 * provided as part of the framework.</p>
 */
@RequestScope
@SuppressWarnings("unused")
public class PageContextManager implements Closeable, AutoCloseable, PageRender {
  private static final Logger logging = Logging.logger(PageContextManager.class);

  private static final String LIVE_RELOAD_TARGET_PROP = "live_reload_url";
  private static final String LIVE_RELOAD_SWITCH_PROP = "live_reload_enabled";
  private static final String LIVE_RELOAD_JS = "http://localhost:35729/livereload.js";

  /** Access to the asset manager. */
  private final @Nonnull AssetManager assetManager;

  /** Page context builder. */
  private final @Nonnull Context.Builder context;

  /** HTTP request bound to this flow. */
  private final @Nonnull HttpRequest request;

  /** Main properties to apply during Soy render. */
  private final @Nonnull ConcurrentMap<String, Object> props;

  /** Additional injected values to apply during Soy render. */
  private final @Nonnull ConcurrentMap<String, Object> injected;

  /** Set of headers that cause this response flow to vary. */
  private final @Nonnull SortedSet<String> varySegments;

  /** Naming map provider to apply during the Soy render flow. */
  private @Nonnull Optional<SoyNamingMapProvider> namingMapProvider;

  /** Built context: assembled when we "close" the page context manager. */
  private @Nullable PageContext builtContext = null;

  /** Whether to enable live-reload mode or not. */
  private final boolean liveReload;

  /** Whether we have closed context building or not. */
  private AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Constructor for page context.
   *
   * @param assetManager Manager for static embedded application assets.
   * @param namingMapProvider Style renaming map provider, if applicable.
   * @throws IllegalStateException If an attempt is made to construct context outside of a server-side HTTP flow.
   */
  PageContextManager(@Nonnull AssetManager assetManager, @Nonnull Optional<SoyNamingMapProvider> namingMapProvider) {
    if (logging.isDebugEnabled()) logging.debug("Initializing `PageContextManager`.");
    //noinspection SimplifyOptionalCallChains
    if (!ServerRequestContext.currentRequest().isPresent())
      throw new IllegalStateException("Cannot construct `PageContext` outside of a server-side HTTP flow.");
    this.request = ServerRequestContext.currentRequest().get();
    this.context = Context.newBuilder();
    this.props = new ConcurrentSkipListMap<>();
    this.injected = new ConcurrentSkipListMap<>();
    this.varySegments = new ConcurrentSkipListSet<>();
    this.namingMapProvider = namingMapProvider;
    this.assetManager = assetManager;

    // inject live-reload state
    this.liveReload = "enabled".equals(System.getProperty("LIVE_RELOAD"));
    this.injected.put(LIVE_RELOAD_SWITCH_PROP, this.liveReload);
    if (this.liveReload) {
      logging.info("Live-reload is currently ENABLED.");
      this.injected.put(LIVE_RELOAD_TARGET_PROP, UnsafeSanitizedContentOrdainer.ordainAsSafe(
        LIVE_RELOAD_JS,
        SanitizedContent.ContentKind.TRUSTED_RESOURCE_URI)
        .toTrustedResourceUrlProto());
    }
  }

  /** @return The current page context builder. */
  public @Nonnull Context.Builder getContext() {
    if (this.closed.get())
      throw new IllegalStateException("Cannot access mutable context after closing page manager state.");
    return context;
  }

  /** {@inheritDoc} */
  @Override
  public @Nonnull <T> MutableHttpResponse<T> finalizeResponse(@Nonnull MutableHttpResponse<T> response,
                                                              @Nonnull T body) {
    Optional<Context> pageContext = response.getAttribute("context", Context.class);
    if (pageContext.isPresent()) {
      if (logging.isDebugEnabled())
        logging.debug("Found request context, finalizing headers.");
      @Nonnull Context ctx = pageContext.get();

      // `Content-Language`
      if (ctx.getLanguage() != null && !ctx.getLanguage().isEmpty())
        response.getHeaders().add(HttpHeaders.CONTENT_LANGUAGE, ctx.getLanguage());

      // `Vary`
      if (ctx.getVaryCount() > 0)
        response.getHeaders().add(
          HttpHeaders.VARY,
          Joiner.on(", ").join(new TreeSet<>(ctx.getVaryList())));

      // additional headers
      if (ctx.getHeaderCount() > 0)
        ctx.getHeaderList().forEach(
          (header) -> response.getHeaders().add(header.getName(), header.getValue()));
    } else {
      logging.warn("Failed to find HTTP cycle context: cannot finalize headers.");
    }
    return response.body(body);
  }

  /** @return Built context. After calling this method the first time, context may no longer be mutated. */
  public @Nonnull PageContext render() {
    if (this.closed.get()) {
      assert this.builtContext != null;
    } else {
      this.closed.compareAndSet(false, true);
      this.builtContext = PageContext.fromProto(
        this.context.build(),
        this.props,
        this.injected,
        this.namingMapProvider.orElse(null));
      if (logging.isDebugEnabled()) {
        logging.debug(format("Exported page context with: %s props, %s injecteds, and proto follows \n%s",
          this.props.size(),
          this.injected.size(),
          this.builtContext.getPageContext()));
      }
    }
    return this.builtContext;
  }

  // -- Builder Interface (Context) -- //

  /**
   * Set the page title for the current render flow. If the app makes use of the framework's built-in page frame, the
   * title will automatically be used.
   *
   * @param title Title to set for the current page. Do not pass `null`.
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If `null` is passed for the title.
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager title(@Nonnull String title) {
    //noinspection ConstantConditions
    if (title == null) throw new IllegalArgumentException("Cannot pass `null` for page title.");
    this.context.getMetaBuilder().setTitle(title);
    return this;
  }

  /**
   * Include the specified JavaScript resource in the rendered page, according to the specified settings. The module is
   * expected to exist and be included in the application's asset bundle.
   *
   * @param name Name of the script module to load into the page.
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If `null` is passed for the module name, or it cannot be located, or is invalid.
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager script(@Nonnull String name) {
    // sensible defaults for script embedding
    return this.script(
      name, null, true, false, false, false, false, false);
  }

  /**
   * Include the specified JavaScript resource in the rendered page, according to the specified settings. The module is
   * expected to exist and be included in the application's asset bundle. This variant allows specification of the most
   * frequent attributes used with scripts.
   *
   * @param name Name of the script module to load into the page.
   * @param defer Whether to add the {@code defer} attribute to the script tag.
   * @param async Whether to add the {@code async} attribute to the script tag.
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If `null` is passed for the module name, or it cannot be located, or is invalid.
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager script(@Nonnull String name, @Nonnull Boolean defer, @Nonnull Boolean async) {
    return this.script(
      name, null, defer, async, false, false, false, false);
  }

  /**
   * Include the specified JavaScript resource in the rendered page, according to the specified settings. The module is
   * expected to exist and be included in the application's asset bundle.
   *
   * <p><b>Behavior:</b> Script assets included in this manner are always loaded in the document head, so be judicious
   * with {@code defer} if you are loading a significant amount of JavaScript. There are no default script assets.
   * Scripts are emitted in the order in which they are attached to the page context (i.e. via this method).</p>
   *
   * <p><b>Optimization:</b> Activating the {@code preload} flag causes the script asset to be mentioned in a
   * {@code Link} header, which makes supporting browsers aware of it before loading the DOM. For more aggressive
   * circumstances, the {@code push} flag proactively pushes the asset to the browser (where supported), unless the
   * framework knows the client has seen the asset already. Where HTTP/2 is not supported, special triggering
   * {@code Link} headers may be used.</p>
   *
   * @param name Name of the script module to load into the page.
   * @param id ID to assign the script block in the DOM, so it may be located dynamically.
   * @param defer Whether to add the {@code defer} attribute to the script tag.
   * @param async Whether to add the {@code async} attribute to the script tag.
   * @param module Whether to add the {@code module} attribute to the script tag.
   * @param nomodule Whether to add the {@code nomodule} attribute to the script tag.
   * @param preload Whether to link/hint about the asset in response headers.
   * @param push Whether to pro-actively push the asset, if we think the client doesn't have it.
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If `null` is passed for the module name, or it cannot be located, or is invalid.
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager script(@Nonnull String name,
                                            @Nullable String id,
                                            @Nonnull Boolean defer,
                                            @Nonnull Boolean async,
                                            @Nonnull Boolean module,
                                            @Nonnull Boolean nomodule,
                                            @Nonnull Boolean preload,
                                            @Nonnull Boolean push) {
    Optional<ManagedAsset<ScriptAsset>> maybeAsset = (
      this.assetManager.assetMetadataByModule(Objects.requireNonNull(name)));

    // fail if not present
    if (!maybeAsset.isPresent())
      throw new IllegalArgumentException(format("Failed to locate script module '%s'.", name));
    var asset = maybeAsset.get();

    if (!asset.getType().equals(AssetManager.ModuleType.JS))
      throw new IllegalArgumentException(format("Cannot include asset '%s' as %s, it is of type JS.",
        name,
        asset.getType().name()));

    // resolve constituent scripts
    for (ScriptAsset script : asset.getAssets()) {
      // pre-filled URI js record
      JavaScript.Builder js = JavaScript.newBuilder(script.getScript())
        .setDefer(defer)
        .setAsync(async)
        .setModule(module)
        .setNoModule(nomodule)
        .setPreload(preload)
        .setPush(push);

      if (id != null) js.setId(id);
      this.script(js);
    }
    return this;
  }

  /**
   * Include the specified JavaScript resource in the rendered page, according to enclosed settings (i.e. respecting
   * {@code defer}, {@code async}, and other attributes). If the script asset has an ID, it will <b>not</b> be
   * passed through ID rewriting before being rendered.
   *
   * <p><b>Behavior:</b> Script assets included in this manner are always loaded in the document head, so be judicious
   * with {@code defer} if you are loading a significant amount of JavaScript. There are no default script assets.
   * Scripts are emitted in the order in which they are attached to the page context (i.e. via this method).</p>
   *
   * @param script Script asset to load in the rendered page output. Do not pass `null`.
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If `null` is passed for the module name, or it cannot be located, or is invalid.
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager script(@Nonnull Context.Scripts.JavaScript.Builder script) {
    //noinspection ConstantConditions
    if (script == null) throw new IllegalArgumentException("Cannot pass `null` for script.");
    this.context.getScriptsBuilder().addLink(script);
    return this;
  }

  /**
   * Include the specified CSS stylesheet in the rendered page with default settings. The module is expected to exist
   * and be included in the application's asset bundle.
   *
   * @param name Name of the module to load.
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If `null` is passed for the module name, or it cannot be located, or is invalid.
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager stylesheet(@Nonnull String name) {
    return stylesheet(name, null);
  }

  /**
   * Include the specified CSS stylesheet in the rendered page, along with the specified media setting. The module is
   * expected to exist and be included in the application's asset bundle.
   *
   * @param name Name of the module to load.
   * @param media Media assignment for the stylesheet.
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If `null` is passed for the module name, or the module cannot be located.
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager stylesheet(@Nonnull String name, @Nullable String media) {
    return stylesheet(name, null, null, false, false);
  }

  /**
   * Include the specified CSS stylesheet in the rendered page, according to the specified settings. The module is
   * expected to exist and be included in the application's asset bundle.
   *
   * <p><b>Optimization:</b> Activating the {@code preload} flag causes the style asset to be mentioned in a
   * {@code Link} header, which makes supporting browsers aware of it before loading the DOM. For more aggressive
   * circumstances, the {@code push} flag proactively pushes the asset to the browser (where supported), unless the
   * framework knows the client has seen the asset already. Where HTTP/2 is not supported, special triggering
   * {@code Link} headers may be used.</p>
   *
   * @param name Name of the module to load.
   * @param id ID to assign the link tag in the DOM.
   * @param media Media assignment for the stylesheet.
   * @param preload Whether to link/hint about the asset in response headers.
   * @param push Whether to pro-actively push the asset, if we think the client doesn't have it.
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If `null` is passed for the module name, or it cannot be located, or is invalid.
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager stylesheet(@Nonnull String name,
                                                @Nullable String id,
                                                @Nullable String media,
                                                @Nonnull Boolean preload,
                                                @Nonnull Boolean push) {
    Optional<ManagedAsset<StyleAsset>> maybeAsset = (
      this.assetManager.assetMetadataByModule(Objects.requireNonNull(name)));

    // fail if not present
    if (!maybeAsset.isPresent())
      throw new IllegalArgumentException(format("Failed to locate style module '%s'.", name));
    var asset = maybeAsset.get();

    if (!asset.getType().equals(AssetManager.ModuleType.CSS))
      throw new IllegalArgumentException(format("Cannot include asset '%s' as %s, it is of type CSS.",
        name,
        asset.getType().name()));

    // resolve constituent scripts
    for (StyleAsset styleBundle : asset.getAssets()) {
      // pre-filled URI js record
      Stylesheet.Builder styles = Stylesheet.newBuilder(styleBundle.getStylesheet())
        .setPush(push)
        .setPreload(preload);

      if (id != null) styles.setId(id);
      if (media != null) styles.setMedia(media);
      this.stylesheet(styles);
    }
    return this;
  }

  /**
   * Include the specified CSS stylesheet resource in the rendered page, according to the enclosed settings (i.e.
   * respecting properties like <pre>media</pre>). If the stylesheet has an ID, it will <b>not</b> be passed through ID
   * rewriting before being rendered.
   *
   * <p>Stylesheets included in this manner are always loaded in the head, via a link tag. If you want to defer loading
   * of styles, you'll have to do so from JS. Stylesheet links are emitted in the order in which they are attached to
   * the page context (i.e. via this method).</p>
   *
   * @param stylesheet Stylesheet asset to load in the rendered page output. Do not pass `null`.
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If `null` is passed for the stylesheet.
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager stylesheet(@Nonnull Context.Styles.Stylesheet.Builder stylesheet) {
    //noinspection ConstantConditions
    if (stylesheet == null) throw new IllegalArgumentException("Cannot pass `null` for stylesheet.");
    this.context.getStylesBuilder().addLink(stylesheet);
    return this;
  }

  // -- Map-like Interface (Props) -- //

  /**
   * Install a regular context value, at the named key provided. This will make the value available in any bound Soy
   * render flow via a <pre>@param</pre> declaration on the subject template to be rendered.
   *
   * @param key Key at which to make this available as a param.
   * @param value Value to provide for the parameter.
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If the provided <pre>key</pre> is <pre>null</pre>, or a disallowed value, like
   *         <pre>context</pre> (which cannot be overridden).
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager put(@Nonnull String key, @Nonnull Object value) {
    //noinspection ConstantConditions
    if (key == null)
      throw new IllegalArgumentException("Must provide a key to put a Soy context property. Got `null` for key.");
    this.props.put(key, value);
    return this;
  }

  /**
   * Safely retrieve a value from the current render context properties. If no property is found at the provided key,
   * an empty {@link Optional} is returned. Otherwise, an {@link Optional} is returned wrapping whatever value was
   * found.
   *
   * @param key Key at which to retrieve the desired render context property.
   * @return Optional-wrapped context property value.
   */
  public @Nonnull Optional<Object> get(@Nonnull String key) {
    return this.get(key, false);
  }

  /**
   * Safely retrieve a value from either the current render context properties, or the current injected values. If no
   * value is found in whatever context we're looking in, an empty {@link Optional} is returned. Otherwise, an
   * {@link Optional} is returned wrapping whatever value was found.
   *
   * @param key Key at which to retrieve the desired render property or injected value.
   * @param injected Whether to look in the injected values, or regular property values.
   * @return Empty optional if nothing was found, otherwise, the found value wrapped in an optional.
   */
  public @Nonnull Optional<Object> get(@Nonnull String key, boolean injected) {
    final ConcurrentMap<String, Object> base = injected ? this.injected : this.props;
    if (base.containsKey(key))
      return Optional.of(base.get(key));
    return Optional.empty();
  }

  /**
   * Install an injected context value, at the named key provided. This will make the value available in any bound Soy
   * render flow via the <pre>@inject</pre> declaration, on any template in the render flow.
   *
   * @param key Key at which to make this available as an injected value.
   * @param value Value to provide for the parameter.
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If the provided <pre>key</pre> is <pre>null</pre>, or a disallowed value, like
   *         <pre>context</pre> (which cannot be overridden).
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager inject(@Nonnull String key, @Nonnull Object value) {
    //noinspection ConstantConditions
    if (key == null)
      throw new IllegalArgumentException("Must provide a key to put a Soy context property. Got `null` for key.");
    if ("context".equals(key.toLowerCase()))
      throw new IllegalArgumentException("Cannot use key 'context' for injected property.");
    this.injected.put(key, value);
    return this;
  }

  /**
   * Install, or uninstall, the request-scoped renaming map provider. This object will be used to lookup style classes
   * and IDs for render-time rewriting. To disable an existing naming map provider override, simply pass an empty
   * {@link Optional}.
   *
   * <p>If no renaming map provider is set here, but a global one is, and renaming is enabled, the global map will be
   * used. If a renaming map is set here, but renaming is <i>not</i> enabled, no renaming takes place.</p>
   *
   * @param namingMapProvider Renaming map provider to install, or {@link Optional#empty()} to uninstall any existing
   *                          overriding renaming map provider.
   * @return Current page context manager (for call chain-ability).
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager rewrite(@Nonnull Optional<SoyNamingMapProvider> namingMapProvider) {
    this.namingMapProvider = namingMapProvider;
    return this;
  }

  // -- Builder Interface (Response) -- //

  /**
   * Affix an arbitrary HTTP header to the response eventually produced by this page context, assuming no errors occur.
   * If an error occurs while rendering, an error page is served <b>without</b> the additional header (unless
   * {@code force} is passed via the companion method to this one).
   *
   * @param name Name of the header value to affix to the response.
   * @param value Value of the header to affix to the response, at {@code name}.
   * @return Current page context manager (for call chain-ability).
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager header(@Nonnull String name, @Nonnull String value) {
    return this.header(name, value, false);
  }

  /**
   * Affix an arbitrary HTTP header to the response eventually produced by this page context, assuming no errors occur.
   * If an error occurs while rendering, an error page is served <b>without</b> the additional header (unless
   * {@code force} is passed via the companion method to this one).
   *
   * @param name Name of the header value to affix to the response.
   * @param value Value of the header to affix to the response, at {@code name}.
   * @param force Whether to force the header to be applied, even when an error occurs.
   * @return Current page context manager (for call chain-ability).
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager header(@Nonnull String name, @Nonnull String value, @Nonnull Boolean force) {
    if (HttpHeaders.CONTENT_LANGUAGE.equals(name))
      throw new IllegalArgumentException("Please use `language()` instead of setting a `Content-Language` header.");
    if (HttpHeaders.VARY.equals(name))
      throw new IllegalArgumentException("Please use `vary()` instead of setting a `Vary` header.");
    if (HttpHeaders.ETAG.equals(name))
      throw new IllegalArgumentException("Please use `enableETags()` instead of setting an `ETag` header.");
    this.context.addHeader(Context.ResponseHeader.newBuilder()
      .setName(name)
      .setValue(value)
      .setForce(force));
    return this;
  }

  /**
   * Enable a dynamic {@code ETag} header value, which is computed from the rendered content produced by this page
   * context record.
   *
   * @param enableETags Whether to enable the {@code ETag} header.
   * @param strong Whether to compute a "strong" {@code ETag} (based on rendered content produced), or a "weak"
   *        {@code ETag} (based on the request and render context).
   * @return Current page context manager (for call chain-ability).
   */
  @CanIgnoreReturnValue
  @SuppressWarnings("WeakerAccess")
  public @Nonnull PageContextManager enableETags(@Nonnull Boolean enableETags, @Nonnull Boolean strong) {
    this.context.setEtag(Context.DynamicETag.newBuilder()
      .setEnabled(enableETags)
      .setStrong(strong));
    return this;
  }

  /**
   * Set the value to send back in this response's {@code Content-Language} header. If an {@link Optional#empty()}
   * instance is passed, no header is sent.
   *
   * @param language Language, or empty value, to send.
   * @return Current page context manager (for call chain-ability).
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager language(@Nonnull Optional<String> language) {
    if (language.isPresent()) {
      this.context.setLanguage(language.get());
    } else {
      this.context.clearLanguage();
    }
    return this;
  }

  /**
   * Append an HTTP request header considered as part of the {@code Vary} header in the response. These values are de-
   * duplicated before joining and affixing.
   *
   * @param variance HTTP request header which causes the associated response to vary.
   * @return Current page context manager (for call chain-ability).
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager vary(@Nonnull String variance) {
    return this.vary(Collections.singleton(variance));
  }

  /**
   * Append an HTTP request header considered as part of the {@code Vary} header in the response. These values are de-
   * duplicated before joining and affixing.
   *
   * @param variance HTTP request header which causes the associated response to vary.
   * @return Current page context manager (for call chain-ability).
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager vary(@Nonnull String... variance) {
    return this.vary(Arrays.asList(variance));
  }

  /**
   * Append an HTTP request header considered as part of the {@code Vary} header in the response. These values are de-
   * duplicated before joining and affixing.
   *
   * @param variance HTTP request header which causes the associated response to vary.
   * @return Current page context manager (for call chain-ability).
   */
  @CanIgnoreReturnValue
  @SuppressWarnings("WeakerAccess")
  public @Nonnull PageContextManager vary(@Nonnull Iterable<String> variance) {
    variance.forEach((segment) -> {
      if (this.varySegments.add(segment))
        this.context.addVary(segment);
    });
    return this;
  }

  // -- API: Trusted Resources -- //

  /**
   * Generate a trusted resource URL for the provided Java URL.
   *
   * @param url Pre-ordained trusted resource URL.
   * @return Trusted resource URL specification proto.
   */
  @SuppressWarnings("WeakerAccess")
  public @Nonnull TrustedResourceUrlProto trustedResource(@Nonnull URL url) {
    return UnsafeSanitizedContentOrdainer.ordainAsSafe(
      url.toString(),
      SanitizedContent.ContentKind.TRUSTED_RESOURCE_URI)
      .toTrustedResourceUrlProto();
  }

  /**
   * Generate a trusted resource URL for the provided Java URI.
   *
   * @param uri Pre-ordained trusted resource URI.
   * @return Trusted resource URL specification proto.
   */
  @SuppressWarnings("WeakerAccess")
  public @Nonnull TrustedResourceUrlProto trustedResource(@Nonnull URI uri) {
    return UnsafeSanitizedContentOrdainer.ordainAsSafe(
      uri.toString(),
      SanitizedContent.ContentKind.TRUSTED_RESOURCE_URI)
      .toTrustedResourceUrlProto();
  }

  // -- Interface: HTTP Request -- //

  /**
   * Return the currently-active HTTP request object, to which the current render/controller flow is bound.
   *
   * @return Active HTTP request object.
   */
  public @Nonnull HttpRequest getRequest() {
    return this.request;
  }

  // -- Interface: Closeable -- //

  /**
   * Closes this stream and releases any system resources associated with it. If the stream is already closed then
   * invoking this method has no effect.
   *
   * <p>As noted in {@link AutoCloseable#close()}, cases where the close may fail require careful attention. It is
   * strongly advised to relinquish the underlying resources and to internally <em>mark</em> the {@code Closeable} as
   * closed, prior to throwing the {@code IOException}.
   */
  @Override
  public void close() {
    if (!this.closed.get()) {
      this.render();
    }
  }

  // -- Interface: Delegated Context -- //

  /**
   * Retrieve serializable server-side-rendered page context, which should be assigned to the render flow bound to this
   * context mediator.
   *
   * @return Server-side rendered page context.
   */
  @Override
  public @Nonnull Context getPageContext() {
    this.close();
    if (this.builtContext == null) throw new IllegalStateException("Unable to read page context.");
    return this.builtContext.getPageContext();
  }

  /**
   * Retrieve properties which should be made available via regular, declared `@param` statements.
   *
   * @return Map of regular template properties.
   */
  @Nonnull
  @Override
  public Map<String, Object> getProperties() {
    this.close();
    if (this.builtContext == null) throw new IllegalStateException("Unable to read page context.");
    return this.builtContext.getProperties();
  }

  /**
   * Retrieve properties and values that should be made available via `@inject`.
   *
   * @param framework Framework-injected properties.
   * @return Map of injected properties and their values.
   */
  @Nonnull
  @Override
  public Map<String, Object> getInjectedProperties(@Nonnull Map<String, Object> framework) {
    this.close();
    if (this.builtContext == null) throw new IllegalStateException("Unable to read page context.");
    return this.builtContext.getInjectedProperties(framework);
  }

  /**
   * Specify a Soy renaming map which overrides the globally-installed map, if any. Renaming must still be activated via
   * config, or manually, for the return value of this method to have any effect.
   *
   * @return {@link SoyNamingMapProvider} that should be used for this render routine.
   */
  @Nonnull
  @Override
  public Optional<SoyNamingMapProvider> overrideNamingMap() {
    this.close();
    if (this.builtContext == null) throw new IllegalStateException("Unable to read page context.");
    return this.builtContext.overrideNamingMap();
  }

  /**
   * Indicate whether live-reload mode is enabled or not, which is governed by the built toolchain (i.e. a Bazel
   * condition, activated by the Makefile, which injects a JDK system property). Live-reload features additionally
   * require dev mode to be active.
   *
   * @return Whether live-reload is enabled.
   */
  public boolean isLiveReload() {
    return liveReload;
  }
}
