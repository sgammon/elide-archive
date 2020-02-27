package gust.backend;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.runtime.http.scope.RequestScope;
import io.micronaut.views.soy.SoyNamingMapProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.elide.page.Context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;


/**
 * Manages the process of filling out {@link PageContext} objects before they are sealed, and delivered to Closure/Soy
 * to be reduced and rendered into content.
 *
 * <p>This object may be used from controllers via dependency injection, or used via the base controller classes provided
 * as part of the framework.</p>
 */
@RequestScope
@SuppressWarnings("unused")
public class PageContextManager implements Closeable, AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(PageContextManager.class);

  /** Page context builder. */
  private final @Nonnull Context.Builder context;

  /** HTTP request bound to this flow. */
  private final @Nonnull HttpRequest request;

  /** Main properties to apply during Soy render. */
  private final @Nonnull ConcurrentMap<String, Object> props;

  /** Additional injected values to apply during Soy render. */
  private final @Nonnull ConcurrentMap<String, Object> injected;

  /** Naming map provider to apply during the Soy render flow. */
  private @Nonnull Optional<SoyNamingMapProvider> namingMapProvider;

  /** Built context: assembled when we "close" the page context manager. */
  private @Nullable PageContext builtContext = null;

  /** Whether we have closed context building or not. */
  private boolean closed = false;

  /**
   * Constructor for page context.
   *
   * @param namingMapProvider Style renaming map provider.
   * @throws IllegalStateException If an attempt is made to construct context outside of a server-side HTTP flow.
   */
  PageContextManager(@Nonnull Optional<SoyNamingMapProvider> namingMapProvider) {
    if (LOG.isDebugEnabled()) LOG.debug("Initializing `PageContextManager`.");
    //noinspection SimplifyOptionalCallChains
    if (!ServerRequestContext.currentRequest().isPresent())
      throw new IllegalStateException("Cannot construct `PageContext` outside of a server-side HTTP flow.");
    this.request = ServerRequestContext.currentRequest().get();
    this.context = Context.newBuilder();
    this.props = new ConcurrentSkipListMap<>();
    this.injected = new ConcurrentSkipListMap<>();
    this.namingMapProvider = namingMapProvider;
  }

  /** @return The current page context builder. */
  @Nonnull public Context.Builder getContext() {
    if (this.closed)
      throw new IllegalStateException("Cannot access mutable context after closing page manager state.");
    return context;
  }

  /** @return Built context. After calling this method the first time, context may no longer be mutated. */
  @Nonnull public PageContext render() {
    if (this.closed) {
      assert this.builtContext != null;
    } else {
      this.close();
      if (LOG.isDebugEnabled())
        LOG.debug("Exporting page context...");
      this.builtContext = PageContext.fromProto(
        this.context.build(),
        this.props,
        this.injected,
        this.namingMapProvider.orElse(null));
      if (LOG.isDebugEnabled())
        LOG.debug(String.format("Exported page context: %s", this.builtContext));
    }
    return this.builtContext;
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
  public @Nonnull PageContextManager rewrite(@Nonnull Optional<SoyNamingMapProvider> namingMapProvider) {
    this.namingMapProvider = namingMapProvider;
    return this;
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
    this.closed = true;
  }
}
