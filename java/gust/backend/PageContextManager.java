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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import com.google.common.html.types.TrustedResourceUrlProto;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.ByteString;
import com.google.template.soy.data.SanitizedContent;
import com.google.template.soy.data.UnsafeSanitizedContentOrdainer;
import gust.backend.runtime.AssetManager;
import gust.backend.runtime.AssetManager.ManagedAsset;
import gust.backend.runtime.Logging;
import gust.util.Hex;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.runtime.http.scope.RequestScope;
import io.micronaut.views.soy.SoyNamingMapProvider;
import org.slf4j.Logger;
import tools.elide.assets.AssetBundle.StyleBundle.StyleAsset;
import tools.elide.assets.AssetBundle.ScriptBundle.ScriptAsset;
import tools.elide.page.Context;
import tools.elide.page.Context.ClientHint;
import tools.elide.page.Context.FramingPolicy;
import tools.elide.page.Context.ClientHints;
import tools.elide.page.Context.ConnectionHint;
import tools.elide.page.Context.ReferrerPolicy;
import tools.elide.page.Context.Styles.Stylesheet;
import tools.elide.page.Context.Scripts.JavaScript;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

  private static final int ETAG_LENGTH = 6;

  private static final String DPR_HEADER = "DPR";
  private static final String ECT_HEADER = "ECT";
  private static final String RTT_HEADER = "RTT";
  private static final String LINK_HEADER = "Link";
  private static final String MEMORY_HEADER = "Device-Memory";
  private static final String DOWNLINK_HEADER = "Downlink";
  private static final String VIEWPORT_WIDTH_HEADER = "Viewport-Width";
  private static final String ACCEPT_CH_HEADER = "Accept-CH";
  private static final String ACCEPT_CH_LIFETIME_HEADER = "Accept-CH-Lifetime";
  private static final String FEATURE_POLICY_HEADER = "Feature-Policy";
  private static final String X_FRAME_OPTIONS_HEADER = "X-Frame-Options";
  private static final String X_CONTENT_TYPE_OPTIONS_HEADER = "X-Content-Type-Options";
  private static final String X_XSS_PROTECTION_HEADER = "X-XSS-Protection";
  private static final String LINK_DNS_PREFETCH_TOKEN = "dns-prefetch";
  private static final String LINK_PRECONNECT_TOKEN = "preconnect";
  private static final String REFERRER_POLICY_HEADER = "Referrer-Policy";
  private static final ConnectionHint DEFAULT_ECT = ConnectionHint.FAST;

  private static final String CDN_PREFIX_IJ_PROP = "cdn_prefix";

  private static final String LIVE_RELOAD_TARGET_PROP = "live_reload_url";
  private static final String LIVE_RELOAD_SWITCH_PROP = "live_reload_enabled";
  private static final String LIVE_RELOAD_JS = "http://localhost:35729/livereload.js";

  /** Access to the asset manager. */
  private final @Nonnull AssetManager assetManager;

  /** Page context builder. */
  private final @Nonnull Context.Builder context;

  /** HTTP request bound to this flow. */
  @SuppressWarnings("rawtypes")
  private final @Nonnull HttpRequest request;

  /** Set of interpreted/immutable client hints. */
  private final @Nonnull ClientHints hints;

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
  private @Nonnull final AtomicBoolean closed = new AtomicBoolean(false);

  /** CDN prefix to apply to this HTTP cycle. */
  private @Nonnull volatile Optional<String> cdnPrefix = Optional.empty();

  /**
   * Constructor for page context.
   *
   * @param assetManager Manager for static embedded application assets.
   * @param namingMapProvider Style renaming map provider, if applicable.
   * @throws IllegalStateException If an attempt is made to construct context outside of a server-side HTTP flow.
   */
  PageContextManager(@Nonnull AssetManager assetManager,
                     @Nonnull Optional<SoyNamingMapProvider> namingMapProvider) {
    if (logging.isDebugEnabled()) logging.debug("Initializing `PageContextManager`.");
    //noinspection SimplifyOptionalCallChains
    if (!ServerRequestContext.currentRequest().isPresent())
      throw new IllegalStateException("Cannot construct `PageContext` outside of a server-side HTTP flow.");
    this.request = ServerRequestContext.currentRequest().get();
    this.context = interpretRequest(request);
    this.hints = this.context.getHintsBuilder().build();
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

  /**
   * Interpret the provided HTTP request, reading any properties and hints that are specified. For instance, this is
   * where we can load/interpret <i>Client Hints</i> provided by the browser, as applicable.
   *
   * @param request HTTP request to interpret/read.
   * @return Context builder to use, factoring in the provided request.
   */
  @VisibleForTesting
  @SuppressWarnings({"WeakerAccess", "rawtypes"})
  static @Nonnull Context.Builder interpretRequest(@Nonnull HttpRequest request) {
    Context.Builder builder = Context.newBuilder();
    interpretHints(request, builder);
    return builder;
  }

  /**
   * Interpret <i>Client Hints</i> headers from the provided HTTP request, and apply them to the provided context.
   *
   * @param request HTTP request to interpret hints from.
   * @param builder Builder to apply the hints to, if found.
   */
  @VisibleForTesting
  @SuppressWarnings({"WeakerAccess", "rawtypes"})
  static void interpretHints(@Nonnull HttpRequest request, @Nonnull Context.Builder builder) {
    HttpHeaders headers = request.getHeaders();
    Context.ClientHints.Builder hints = builder.getHintsBuilder();

    checkHint(ClientHint.DPR, headers, hints, (value) -> hints.setDevicePixelRatio(Integer.parseUnsignedInt(value)));
    checkHint(ClientHint.ECT, headers, hints, (value) ->
      hints.setEffectiveConnectionType(connectionHintForECT(value).orElse(DEFAULT_ECT)));
    checkHint(ClientHint.RTT, headers, hints, (value) -> hints.setRoundTripTime(Integer.parseUnsignedInt(value)));
    checkHint(ClientHint.DOWNLINK, headers, hints, (value) -> hints.setDownlink(Float.parseFloat(value)));
    checkHint(ClientHint.DEVICE_MEMORY, headers, hints, (value) -> hints.setDeviceMemory(Float.parseFloat(value)));
    checkHint(ClientHint.SAVE_DATA, headers, hints, (value) -> hints.setSaveData(true));
    checkHint(ClientHint.WIDTH, headers, hints, (value) -> hints.setWidth(Integer.parseUnsignedInt(value)));
    checkHint(ClientHint.VIEWPORT_WIDTH, headers, hints,
      (value) -> hints.setViewportWidth(Integer.parseUnsignedInt(value)));
  }

  /**
   * Check the provided set of HTTP headers for the specified client hint. If a value is found, dispatch the provided
   * function with the hint, and the discovered header value.
   *
   * @param hint Client hint to check for in the specified headers.
   * @param headers HTTP request headers to check.
   * @param callable Callable to dispatch if the header is found.
   */
  @VisibleForTesting
  @SuppressWarnings("WeakerAccess")
  static void checkHint(@Nonnull ClientHint hint,
                        @Nonnull HttpHeaders headers,
                        @Nonnull ClientHints.Builder hints,
                        Consumer<String> callable) {
    String headerName = clientHintForEnum(hint);
    if (headers.contains(headerName)) {
      String headerValue = headers.get(headerName);
      if (headerValue != null && !headerValue.isEmpty()) {
        try {
          hints.addIndicated(hint);
          callable.accept(headerValue);
        } catch (IllegalArgumentException iae) {
          logging.warn(format("Failed to parse client hint '%s': %s.", hint.name(), iae.getMessage()));
        }
      }
    }
  }

  /**
   * Resolve an enumerated connection hint for the provided connection hint name, which was presumably found in a
   * <i>Client Hints</i> Effective Connection Type header value.
   *
   * @param value Value to interpret.
   * @return Connection hint type, resolved.
   */
  @VisibleForTesting
  @SuppressWarnings("WeakerAccess")
  static @Nonnull Optional<ConnectionHint> connectionHintForECT(@Nonnull String value) {
    switch (value.toLowerCase().trim()) {
      case "slow_2g": return Optional.of(ConnectionHint.SLOW_TWO);
      case "2g": return Optional.of(ConnectionHint.SLOW);
      case "3g": return Optional.of(ConnectionHint.TYPICAL);
      case "4g": return Optional.of(ConnectionHint.FAST);
      default: return Optional.empty();
    }
  }

  /**
   * Produce a string token for the provided client hint.
   *
   * @param hint Enumerated hint type.
   * @return String token matching the hint.
   */
  @VisibleForTesting
  @SuppressWarnings("WeakerAccess")
  static @Nonnull String clientHintForEnum(@Nonnull ClientHint hint) {
    switch (hint) {
      case DPR: return "DPR";
      case ECT: return "ECT";
      case RTT: return "RTT";
      case DOWNLINK: return "Downlink";
      case DEVICE_MEMORY: return "Device-Memory";
      case SAVE_DATA: return "Save-Data";
      case WIDTH: return "Width";
      case VIEWPORT_WIDTH: return "Viewport-Width";
      default:
        throw new IllegalStateException(format("Unrecognized client hint type: '%s'.", hint.name()));
    }
  }

  /**
   * Produce a token for the specified {@code Referrer-Policy} selection.
   *
   * @param policy Policy to produce a token for.
   * @return String token.
   */
  @VisibleForTesting
  @SuppressWarnings("WeakerAccess")
  static @Nonnull Optional<String> tokenForReferrerPolicy(@Nonnull ReferrerPolicy policy) {
    switch (policy) {
      case NO_REFERRER: return Optional.of("no-referrer");
      case NO_REFERRER_WHEN_DOWNGRADE: return Optional.of("no-referrer-when-downgrade");
      case ORIGIN: return Optional.of("origin");
      case ORIGIN_WHEN_CROSS_ORIGIN: return Optional.of("origin-when-cross-origin");
      case SAME: return Optional.of("same-origin");
      case STRICT_ORIGIN: return Optional.of("strict-origin");
      case STRICT_ORIGIN_WHEN_CROSS_ORIGIN: return Optional.of("strict-origin-when-cross-origin");
      case UNSAFE_URL: return Optional.of("unsafe-url");
      default: return Optional.empty();
    }
  }

  /**
   * Produce a token for the specified {@code X-Frame-Options} policy.
   *
   * @param policy Policy to produce a token for.
   * @return String token.
   */
  @VisibleForTesting
  @SuppressWarnings("WeakerAccess")
  static @Nonnull Optional<String> tokenForFramingPolicy(@Nonnull FramingPolicy policy) {
    switch (policy) {
      case SAMEORIGIN: return Optional.of("SAMEORIGIN");
      case DENY: return Optional.of("DENY");
      default: return Optional.empty();
    }
  }

  /**
   * Format a hostname for DNS pre-fetching by the browser.
   *
   * @param hostname Hostname to pre-fetch.
   * @param relevance Indicates the type of link being specified.
   * @return Formatted {@code Link} header value.
   */
  @VisibleForTesting
  @SuppressWarnings("WeakerAccess")
  static @Nonnull String formatLinkHeader(@Nonnull String hostname, @Nonnull String relevance) {
    return "<" + hostname + ">; rel=" + relevance;
  }

  /** @return The current page context builder. */
  public @Nonnull Context.Builder getContext() {
    if (this.closed.get())
      throw new IllegalStateException("Cannot access mutable context after closing page manager state.");
    return context;
  }

  /** {@inheritDoc} */
  @Override
  public @Nonnull <T> MutableHttpResponse<T> finalizeResponse(@Nonnull HttpRequest<?> request,
                                                              @Nonnull MutableHttpResponse<T> soyResponse,
                                                              @Nonnull T body,
                                                              @Nullable MessageDigest digester) {
    MutableHttpResponse<T> response = soyResponse.body(body);
    Optional<Context> pageContext = response.getAttribute("context", Context.class);
    if (pageContext.isPresent()) {
      if (logging.isDebugEnabled())
        logging.debug("Found request context, finalizing headers.");
      @Nonnull Context ctx = pageContext.get();

      // process `ETag` first, because if `If-None-Match` processing is enabled, this may kick is out of this response
      // flow entirely.
      if (digester != null) {
        if (ctx.getEtag().hasPreimage()) {
          digester.update(ctx.getEtag().getPreimage().getFingerprint().asReadOnlyByteBuffer().array());
        }
        String contentDigest = Hex.bytesToHex(digester.digest(), ETAG_LENGTH);
        if (!Objects.requireNonNull(contentDigest).isEmpty()) {
          if (request.getHeaders().contains(HttpHeaders.IF_NONE_MATCH)) {
            // we have a potential conditional match
            if (contentDigest.equals(Objects.requireNonNull(request.getHeaders()
                  .get(HttpHeaders.IF_NONE_MATCH))
                .replace("\"", "")
                .replace("W/", ""))) {
              if (logging.isDebugEnabled()) {
                logging.debug(format(
                  "Response matched `If-None-Match` etag value '%s'. Sending 304.", ("W/" + contentDigest)));
              }

              // drop the body - explicitly truncate so we don't get caught by chunked TE - and reset the status to
              // indicate a conditional request match
              response.body(null);
              response.contentLength(0);
              response.status(HttpStatus.NOT_MODIFIED.getCode());
              return response;
            }
          }
          response.getHeaders().add(HttpHeaders.ETAG, "W/" + "\"" + contentDigest + "\"");
        }
      } else if (logging.isTraceEnabled()) {
        logging.trace("Dynamic ETags are disabled.");
      }

      // `Accept-CH`
      if (ctx.hasHints() &&  // we must support hints to emit this header
          ctx.getHints().getSupportedCount() > 0 &&  // there must be hint types to send
          (ctx.getHints().getIndicatedCount() == 0 ||  // we should only send if there are no hints from the client, or
          !Sets.difference(  // the provided set of hints from the client doesn't match with the supported set
             ImmutableSortedSet.copyOf(ctx.getHints().getIndicatedList()),
             ImmutableSortedSet.copyOf(ctx.getHints().getSupportedList()))
            .isEmpty())) {
        SortedSet<String> tokens = ctx.getHints().getSupportedList().stream()
          .map(PageContextManager::clientHintForEnum)
          .collect(Collectors.toCollection(TreeSet::new));

        String renderedHints = Joiner.on(", ").join(tokens);
        if (logging.isDebugEnabled())
          logging.debug(format("Indicating `Accept-CH`: '%s'.", renderedHints));
        response.getHeaders().add(ACCEPT_CH_HEADER, renderedHints);

        // since we've appended `Accept-CH`, check for a lifetime
        long lifetime = ctx.getHints().getLifetime();
        if (lifetime > 0) {
          response.getHeaders().add(ACCEPT_CH_LIFETIME_HEADER, String.valueOf(lifetime));
        }
      } else if (logging.isTraceEnabled()) {
        logging.trace("`Accept-CH` not configured for response.");
      }

      // `Content-Language`
      if (ctx.getLanguage() != null && !ctx.getLanguage().isEmpty())
        response.getHeaders().add(HttpHeaders.CONTENT_LANGUAGE, ctx.getLanguage());
      else if (logging.isTraceEnabled())
        logging.trace("`Content-Language` not configured for response.");

      // `Vary`
      if (ctx.getVaryCount() > 0)
        response.getHeaders().add(
          HttpHeaders.VARY,
          Joiner.on(", ").join(new TreeSet<>(ctx.getVaryList())));
      else if (logging.isTraceEnabled())
        logging.trace("`Vary` not configured for response.");

      // `Feature-Policy`
      if (ctx.getFeaturePolicyCount() > 0) {
        // gather policies
        SortedSet<String> segments = new TreeSet<>(ctx.getFeaturePolicyList());
        if (!segments.isEmpty()) {
          String renderedPolicy = Joiner.on(" ").join(segments);
          response.getHeaders().add(FEATURE_POLICY_HEADER, renderedPolicy);
        }
      } else if (logging.isTraceEnabled()) {
        logging.trace("`Feature-Policy` not configured for response.");
      }

      // `Referrer-Policy`
      Optional<String> referrerPolicyToken = tokenForReferrerPolicy(ctx.getReferrerPolicy());
      if (referrerPolicyToken.isPresent()) {
        if (logging.isDebugEnabled())
          logging.debug(format("Indicating `Referrer-Policy`: '%s'.", referrerPolicyToken.get()));
        response.getHeaders().add(
          REFERRER_POLICY_HEADER,
          referrerPolicyToken.get());
      } else if (logging.isTraceEnabled()) {
        logging.trace("`Referrer-Policy` not configured for response.");
      }

      // `X-Frame-Options`
      Optional<String> framingToken = tokenForFramingPolicy(ctx.getFramingPolicy());
      if (framingToken.isPresent()) {
        if (logging.isDebugEnabled())
          logging.debug(format("Indicating `X-Frame-Options`: '%s'.", ctx.getFramingPolicy().name()));
        response.getHeaders().add(
          X_FRAME_OPTIONS_HEADER,
          framingToken.get());

      } else if (logging.isTraceEnabled()) {
        logging.trace("No `X-Frame-Options` configured for this response.");
      }

      // `X-Content-Type-Options`
      if (ctx.getContentTypeNosniff()) {
        if (logging.isDebugEnabled())
          logging.debug("Indicating `X-Content-Type-Options`: 'nosniff'.");
        response.getHeaders().add(
          X_CONTENT_TYPE_OPTIONS_HEADER,
          "nosniff");
      } else if (logging.isTraceEnabled()) {
        logging.trace("No `X-Content-Type-Options` configured for this response.");
      }

      // `Link` (domain pre-connection)
      if (ctx.getPreconnectCount() > 0) {
        SortedSet<String> preconnectList = new TreeSet<>(ctx.getPreconnectList());
        preconnectList.forEach((preconnect) ->
          response.getHeaders().add(LINK_HEADER, formatLinkHeader(preconnect, LINK_PRECONNECT_TOKEN)));

        if (logging.isDebugEnabled()) {
          logging.debug(format("Indicated (via `Link`) %s domains for pre-connection.", ctx.getPreconnectCount()));
        }
        if (logging.isTraceEnabled()) {
          logging.trace(format("Domains for pre-connection: %s.", Joiner.on(", ").join(preconnectList)));
        }
      } else if (logging.isTraceEnabled()) {
        logging.trace("No domains to apply to pre-connect hints.");
      }

      // `Link` (DNS pre-fetching)
      if (ctx.getDnsPrefetchCount() > 0) {
        SortedSet<String> dnsPrefetches = new TreeSet<>(ctx.getDnsPrefetchList());
        dnsPrefetches.forEach((prefetch) ->
          response.getHeaders().add(LINK_HEADER, formatLinkHeader(prefetch, LINK_DNS_PREFETCH_TOKEN)));

        if (logging.isDebugEnabled()) {
          logging.debug(format("Indicated (via `Link`) %s domains for DNS prefetching.", ctx.getDnsPrefetchCount()));
        }
        if (logging.isTraceEnabled()) {
          logging.trace(format("DNS domains prefetched: %s.", Joiner.on(", ").join(dnsPrefetches)));
        }
      } else if (logging.isTraceEnabled()) {
        logging.trace("No domains to apply to DNS prefetch hints.");
      }

      // `X-XSS-Protection`
      if (ctx.getXssProtection() != null && !ctx.getXssProtection().isEmpty()) {
        if (logging.isDebugEnabled())
          logging.debug(format("Applying `X-XSS-Protection` policy: '%s'.", ctx.getXssProtection()));
        response.getHeaders().add(X_XSS_PROTECTION_HEADER, ctx.getXssProtection());
      } else if (logging.isTraceEnabled()) {
        logging.trace("No `X-XSS-Protection` policy configured for this response cycle.");
      }

      // additional headers
      if (ctx.getHeaderCount() > 0)
        ctx.getHeaderList().forEach(
          (header) -> response.getHeaders().add(header.getName(), header.getValue()));
      else if (logging.isTraceEnabled())
        logging.trace("No additional headers to apply.");
    } else {
      logging.warn("Failed to find HTTP cycle context: cannot finalize headers.");
    }
    return response;
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
   * Retrieve the current value for the page title, set in the builder. If there is no value, {@link Optional#empty()}
   * is supplied as the return value.
   *
   * @return Current page title, wrapped in an optional value.
   */
  public @Nonnull Optional<String> title() {
    final String val = this.context.getMetaBuilder().getTitle();
    return "".equals(val) ? Optional.empty() : Optional.of(val);
  }

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
   * Retrieve the current value for the page description, set in the builder. If there is no value set,
   * {@link Optional#empty()} is supplied as the return value.
   *
   * @return Current page description, wrapped in an optional value.
   */
  public @Nonnull Optional<String> description() {
    final String val = this.context.getMetaBuilder().getDescription();
    return "".equals(val) ? Optional.empty() : Optional.of(val);
  }

  /**
   * Set the page description for the current render flow. If the app makes use of the framework's built-in page frame,
   * the value will automatically be used.
   *
   * @param description Description to set for the current page. Do not pass `null`.
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If `null` is passed for the title.
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager description(@Nonnull String description) {
    //noinspection ConstantConditions
    if (description == null) throw new IllegalArgumentException("Cannot pass `null` for page description.");
    this.context.getMetaBuilder().setDescription(description);
    return this;
  }

  /**
   * Retrieve the current value for the page keywords, set in the builder. If there is no value set,
   * {@link Optional#empty()} is supplied as the return value.
   *
   * @return Current page keywords, wrapped in an optional value.
   */
  public @Nonnull Optional<List<String>> keywords() {
    final ArrayList<String> val = this.context.getMetaBuilder().getKeywordList().asByteStringList()
            .stream()
            .map(ByteString::toString)
            .collect(Collectors.toCollection(() -> new ArrayList<>(this.context.getMetaBuilder().getKeywordCount())));
    return val.isEmpty() ? Optional.empty() : Optional.of(val);
  }

  /**
   * Add the provided page keywords for the current render flow. If the app makes use of the framework's built-in page
   * frame, the value will automatically be used.
   *
   * @param keywords Keywords to set for the current page. Do not pass `null`.
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If `null` is passed for the title.
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager addKeyword(@Nonnull String... keywords) {
    //noinspection ConstantConditions
    if (keywords == null) throw new IllegalArgumentException("Cannot pass `null` for page title.");
    for (String keyword : keywords) {
      this.context.getMetaBuilder().addKeyword(keyword);
    }
    return this;
  }

  /**
   * Clear the current set of page keywords for the current render flow. If the app makes use of the framework's
   * built-in page frame, the value will automatically be used.
   *
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If `null` is passed for the title.
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager clearKeywords() {
    this.context.getMetaBuilder().clearKeyword();
    return this;
  }

  /**
   * Overwrite the current set of page keywords for the current render flow. If the app makes use of the framework's
   * built-in page frame, the value will automatically be used.
   *
   * @param keywords Keywords to set for the current page. Do not pass `null`.
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If `null` is passed for the title.
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager setKeywords(@Nonnull Collection<String> keywords) {
    //noinspection ConstantConditions
    if (keywords == null) throw new IllegalArgumentException("Cannot pass `null` for page title.");
    this.clearKeywords();
    if (!keywords.isEmpty()) this.context.getMetaBuilder().addAllKeyword(keywords);
    return this;
  }

  /**
   * Retrieve the full set of regular HTML metadata links attached to the current render flow. If the app makes use of
   * the framework's built-in age frame, these links will automatically be applied.
   *
   * @return Current set of links that will be listed in page metadata.
   */
  public @Nonnull Optional<List<Context.PageLink>> links() {
    final List<Context.PageLink> links = this.context.getMetaBuilder().getLinkList();
    return links.isEmpty() ? Optional.empty() : Optional.of(links);
  }

  /**
   * Add a regular HTML metadata link to the current render flow, specified by a {@link Context.PageLink} proto record.
   * Adding via this method is sufficient for the link to make it into the rendered page, so long as the framework's
   * page frame is invoked.
   *
   * @param link HTML metadata link to add to the page.
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If `null` is passed for the title.
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager addLink(@Nonnull Context.PageLink.Builder link) {
    //noinspection ConstantConditions
    if (link == null) throw new IllegalArgumentException("Cannot pass `null` for page link spec.");
    this.context.getMetaBuilder().addLink(link);
    return this;
  }

  /**
   * Add a regular HTML metadata link to the current render flow, specified by the provided method parameters. Each
   * parameter maps to an attribute specified for the <pre>link</pre> HTML element.
   *
   * @param relevance HTML "rel" attribute.
   * @param href HTML "href" attribute.
   * @param type HTML "type" attribute. Wrapped in an optional.
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If `null` is passed for any parameter.
   */
  @CanIgnoreReturnValue
  @SuppressWarnings({"ConstantConditions", "OptionalAssignedToNull"})
  public @Nonnull PageContextManager addLink(@Nonnull String relevance,
                                             @Nonnull URI href,
                                             @Nonnull Optional<String> type) {
    if (relevance == null) throw new IllegalArgumentException("Cannot pass `null` for page link relevance.");
    if (href == null) throw new IllegalArgumentException("Cannot pass `null` for page link href.");
    if (type == null) throw new IllegalArgumentException("Cannot pass `null` for page link type.");
    final Context.PageLink.Builder builder = Context.PageLink.newBuilder()
      .setRelevance(relevance)
      .setHref(this.trustedResource(href));
    type.ifPresent(builder::setType);
    this.context.getMetaBuilder().addLink(builder);
    return this;
  }

  /**
   * Clear the set of HTML metadata links assigned to the current render flow.
   *
   * @return Current page context manager (for call chain-ability).
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager clearLinks() {
    this.context.getMetaBuilder().clearLink();
    return this;
  }

  /**
   * Overwrite the set of page metadata links for the current render flow. If the app makes use of the framework's
   * built-in page frame, the value will automatically be used.
   *
   * @param links Link directives to set for the current page. Do not pass `null`.
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If `null` is passed for the provided links.
   */
  @CanIgnoreReturnValue
  @SuppressWarnings("ConstantConditions")
  public @Nonnull PageContextManager setLinks(@Nonnull Collection<Context.PageLink.Builder> links) {
    if (links == null) throw new IllegalArgumentException("Cannot pass `null` for page links.");
    this.clearLinks();
    links.forEach(this::addLink);
    return this;
  }

  /**
   * Overwrite the value specified for the "robots" metadata key in the current render flow. If the app makes use of the
   * framework's built-in page frame, the value will automatically be used.
   *
   * @param value Robots metadata value to use.
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If `null` is passed for the provided value.
   */
  @CanIgnoreReturnValue
  @SuppressWarnings("ConstantConditions")
  public @Nonnull PageContextManager setRobots(@Nonnull String value) {
    if (value == null) throw new IllegalArgumentException("Cannot pass `null` for robots value.");
    this.context.getMetaBuilder().setRobots(value);
    return this;
  }

  /**
   * Overwrite the value specified for the "googlebot" metadata key in the current render flow. If the app makes use of
   * the framework's built-in page frame, the value will automatically be used.
   *
   * @param value Googlebot metadata value to use.
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If `null` is passed for the provided value.
   */
  @CanIgnoreReturnValue
  @SuppressWarnings("ConstantConditions")
  public @Nonnull PageContextManager setGooglebot(@Nonnull String value) {
    if (value == null) throw new IllegalArgumentException("Cannot pass `null` for googlebot value.");
    this.context.getMetaBuilder().setGooglebot(value);
    return this;
  }

  /**
   * Clear the current value, if any, set for <pre>robots</pre> in the current render flow metadata.
   *
   * @return Current page context manager (for call chain-ability).
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager clearRobots() {
    this.context.getMetaBuilder().clearRobots();
    return this;
  }

  /**
   * Clear the current value, if any, set for <pre>googlebot</pre> in the current render flow metadata.
   *
   * @return Current page context manager (for call chain-ability).
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager clearGooglebot() {
    this.context.getMetaBuilder().clearGooglebot();
    return this;
  }

  /**
   * Overwrite the OpenGraph metadata configuration for the current render flow, with the provided OpenGraph metadata
   * configuration. If the rendered page uses the framework's page template, the values will be serialized and rendered
   * into the page head.
   *
   * @param content OpenGraph content to render.
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If `null` is passed for the provided content.
   */
  @CanIgnoreReturnValue
  @SuppressWarnings("ConstantConditions")
  public @Nonnull PageContextManager setOpenGraph(@Nonnull Context.Metadata.OpenGraph.Builder content) {
    if (content == null) throw new IllegalArgumentException("Cannot pass `null` for OpenGraph content.");
    this.clearOpenGraph();
    this.context.getMetaBuilder().setOpenGraph(content);
    return this;
  }

  /**
   * Apply the provided OpenGraph metadata configuration to the <i>current</i> OpenGraph metadata configuration, if any.
   * If no OpenGraph metadata configuration is set, this method effectively overwrites it.
   *
   * @param content OpenGraph content to merge and apply.
   * @return Current page context manager (for call chain-ability).
   * @throws IllegalArgumentException If `null` is passed for the provided content.
   */
  @CanIgnoreReturnValue
  @SuppressWarnings("ConstantConditions")
  public @Nonnull PageContextManager applyOpenGraph(@Nonnull Context.Metadata.OpenGraph.Builder content) {
    if (content == null) throw new IllegalArgumentException("Cannot pass `null` for OpenGraph content.");
    Context.Metadata.OpenGraph.Builder ogContent = this.context.getMetaBuilder().getOpenGraphBuilder();
    this.setOpenGraph(ogContent.mergeFrom(content.build()));
    return this;
  }

  /**
   * Clear any OpenGraph metadata configuration attached to the current render flow. If there is no such configuration,
   * this method is a no-op.
   *
   * @return Current page context manager (for call chain-ability).
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager clearOpenGraph() {
    this.context.getMetaBuilder().clearOpenGraph();
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
    if (maybeAsset.isEmpty())
      throw new IllegalArgumentException(format("Failed to locate script module '%s'.", name));
    ManagedAsset<ScriptAsset> asset = maybeAsset.get();

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
    if (maybeAsset.isEmpty())
      throw new IllegalArgumentException(format("Failed to locate style module '%s'.", name));
    ManagedAsset<StyleAsset> asset = maybeAsset.get();

    if (!asset.getType().equals(AssetManager.ModuleType.CSS))
      throw new IllegalArgumentException(format("Cannot include asset '%s' as %s, it is of type CSS.",
        name,
        asset.getType().name()));

    // resolve constituent stylesheets
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
    if (ACCEPT_CH_HEADER.equals(name))
      throw new IllegalArgumentException("Please use `clientHints()` instead of setting an `Accept-CH` header.");
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
   * @return Current page context manager (for call chain-ability).
   */
  @CanIgnoreReturnValue
  @SuppressWarnings("WeakerAccess")
  public @Nonnull PageContextManager enableETags(@Nonnull Boolean enableETags) {
    this.context.setEtag(Context.DynamicETag.newBuilder()
      .setEnabled(enableETags)
      .setStrong(true));
    return this;
  }

  /**
   * Enable the provided set of server-indicated client hint types. If the client supports any of the indicated types,
   * it will enclose matching client-hints accordingly, on subsequent requests for resources. If an empty optional
   * ({@link Optional#empty()}) is passed, the current set of client hints are cleared. This method is additive.
   *
   * @param hints Client hints to indicate as supported by the server.
   * @return Current page context manager (for call chain-ability).
   */
  @CanIgnoreReturnValue
  @SuppressWarnings("WeakerAccess")
  public @Nonnull PageContextManager supportedClientHints(Optional<Iterable<ClientHint>> hints,
                                                          @Nonnull Optional<Long> ttl) {
    if (hints.isPresent()) {
      // add hints to indicated set
      this.context.getHintsBuilder().addAllSupported(hints.get());
      ttl.ifPresent(aLong -> this.context.getHintsBuilder().setLifetime(aLong));
    } else {
      // if an empty optional is passed, clear the current set
      this.context.getHintsBuilder().clearIndicated();
    }
    return this;
  }

  /**
   * Enable the provided set of server-indicated client hint types. This method is additive. If the client supports any
   * of the indicated types, it will enclose matching client-hints accordingly, on subsequent requests for resources.
   *
   * @param hints Client hints to indicate as supported by the server.
   * @return Current page context manager (for call chain-ability).
   */
  @CanIgnoreReturnValue
  public @Nonnull PageContextManager supportedClientHints(ClientHint... hints) {
    return supportedClientHints(Optional.of(Arrays.asList(hints)), Optional.empty());
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

  /**
   * Set the specified {@code prefix} as the Content Distribution Network hostname prefix to use when rendering asset
   * links for this HTTP cycle. <b>Do not use a user-provided value for this.</b> If no prefix is set, or one is cleared
   * by passing {@link Optional#empty()}, configuration will be read and used instead.
   *
   * @param prefix Prefix to apply as a CDN hostname for static assets.
   * @return Current page context manager (for call chain-ability).
   */
  @CanIgnoreReturnValue
  @SuppressWarnings("WeakerAccess")
  public @Nonnull PageContextManager cdnPrefix(@Nonnull Optional<String> prefix) {
    this.cdnPrefix = prefix;
    if (prefix.isPresent()) {
      TrustedResourceUrlProto proto = this.trustedResource(URI.create(prefix.get()));
      this.context.setCdnPrefix(proto);
      this.injected.put(CDN_PREFIX_IJ_PROP, proto);
    } else {
      this.context.clearCdnPrefix();
      this.injected.remove(CDN_PREFIX_IJ_PROP);
    }
    return this;
  }

  /**
   * Retrieve the currently-configured CDN prefix value, if one exists. If none can be located, return an empty optional
   * via {@link Optional#empty()}.
   *
   * @return Current CDN prefix value.
   */
  @SuppressWarnings("WeakerAccess")
  public @Nonnull Optional<String> getCdnPrefix() {
    return this.cdnPrefix;
  }

  /**
   * Inject the specified list of hosts as DNS records to prefetch from the browser. There is no guarantee made by the
   * browser that the records will be fetched, it's just a performance hint.
   *
   * @param hosts Hosts to add to the DNS prefetch list.
   * @return Current page context manager (for call chain-ability).
   */
  @CanIgnoreReturnValue
  @SuppressWarnings("WeakerAccess")
  public @Nonnull PageContextManager dnsPrefetch(Iterable<String> hosts) {
    hosts.forEach(this.context::addDnsPrefetch);
    return this;
  }

  /**
   * Inject the specified DNS hostname(s) as records to prefetch from the browser. There is no guarantee made by the
   * browser that the records will be fetched, it's just a performance hint.
   *
   * @param hosts Hosts to add to the DNS prefetch list.
   * @return Current page context manager (for call chain-ability).
   */
  @CanIgnoreReturnValue
  @SuppressWarnings("WeakerAccess")
  public @Nonnull PageContextManager dnsPrefetch(String... hosts) {
    return dnsPrefetch(Arrays.asList(hosts));
  }

  /**
   * Inject the specified list of hosts as pre-connect hints for the browser. There is no guarantee made by the browser
   * that the connections will be established, it's just a performance hint.
   *
   * @param hosts Hosts to add to the server pre-connection list.
   * @return Current page context manager (for call chain-ability).
   */
  @CanIgnoreReturnValue
  @SuppressWarnings("WeakerAccess")
  public @Nonnull PageContextManager preconnect(Iterable<String> hosts) {
    hosts.forEach(this.context::addPreconnect);
    return this;
  }

  /**
   * Inject the specified list of hosts as pre-connect hints for the browser. There is no guarantee made by the browser
   * that the connections will be established, it's just a performance hint.
   *
   * @param hosts Hosts to add to the server pre-connection list.
   * @return Current page context manager (for call chain-ability).
   */
  @CanIgnoreReturnValue
  @SuppressWarnings("WeakerAccess")
  public @Nonnull PageContextManager preconnect(String... hosts) {
    return preconnect(Arrays.asList(hosts));
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

  // -- Interface: Client Hints -- //

  /**
   * Attempt to retrieve an interpreted <i>Client Hints</i> client-indicated value from the current HTTP request. If no
   * value can be found, or no valid value can be decoded for the hint type specified, {@link Optional#empty()} is
   * returned.
   *
   * @param hint Hint type to look for on the current request.
   * @param <V> Value type to return for this hint. If incorrect, a warning is logged and an empty value returned.
   * @return Optional-wrapped found value, or {@link Optional#empty()} if no value could be located.
   */
  @SuppressWarnings("unchecked")
  public @Nonnull <V> Optional<V> hint(@Nonnull ClientHint hint) {
    if (this.hints.getIndicatedCount() > 0 && this.hints.getIndicatedList().contains(hint)) {
      // we found the hint - try to decode the value
      try {
        final V value;
        switch (hint) {
          case DPR: value = (V)(Integer.valueOf(this.hints.getDevicePixelRatio())); break;
          case ECT: value = (V)(this.hints.getEffectiveConnectionType()); break;
          case RTT: value = (V)(Integer.valueOf(this.hints.getRoundTripTime())); break;
          case DOWNLINK: value = (V)(Float.valueOf(this.hints.getDownlink())); break;
          case DEVICE_MEMORY: value = (V)(Float.valueOf(this.hints.getDevicePixelRatio())); break;
          case SAVE_DATA: value = (V)(Boolean.valueOf(this.hints.getSaveData())); break;
          case WIDTH: value = (V)(Integer.valueOf(this.hints.getWidth())); break;
          case VIEWPORT_WIDTH: value = (V)(Integer.valueOf(this.hints.getViewportWidth())); break;
          default:
            logging.warn(format("Unrecognized client hint: '%s'.", hint.name()));
            return Optional.empty();
        }
        return Optional.of(value);

      } catch (ClassCastException cce) {
        logging.warn(format("Failed to cast client hint value '%s'.", hint.name()));
      }
    }
    return Optional.empty();
  }

  // -- Interface: HTTP Request -- //

  /**
   * Return the currently-active HTTP request object, to which the current render/controller flow is bound.
   *
   * @return Active HTTP request object.
   */
  @SuppressWarnings("rawtypes")
  public @Nonnull HttpRequest getRequest() {
    return this.request;
  }

  /**
   * Return the set of interpreted <i>Client Hints</i> headers for the current request.
   *
   * @return Client hints configuration.
   */
  public @Nonnull ClientHints getHints() {
    return this.hints;
  }

  // -- Interface: HTTP `ETag`s -- //

  /** {@inheritDoc} */
  @Override
  public boolean enableETags() {
    return this.builtContext != null ?
      this.builtContext.getPageContext().getEtag().getEnabled() :
      this.context.getEtag().getEnabled();
  }

  /** {@inheritDoc} */
  @Override
  public boolean strongETags() {
    return this.builtContext != null ?
      this.builtContext.getPageContext().getEtag().getStrong() :
      this.context.getEtag().getStrong();
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
