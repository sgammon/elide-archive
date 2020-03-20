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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.protobuf.Timestamp;
import gust.Core;
import gust.backend.runtime.AssetManager;
import gust.backend.runtime.Logging;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.*;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.security.annotation.Secured;
import io.micronaut.validation.Validated;
import io.micronaut.views.csp.CspConfiguration;
import io.micronaut.views.csp.CspFilter;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import tools.elide.core.data.CompressedData;
import tools.elide.core.data.CompressionMode;
import tools.elide.page.Context.CrossOriginResourcePolicy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;


/**
 * Built-in backend controller for serving dynamic managed assets. Assets managed in this manner are hooked into the
 * build pipeline, so that a binary asset manifest is produced at the root of the application JAR.
 *
 * <p>{@link AssetManager} loads the binary manifest/bundle, and then using this controller, we serve the URLs and
 * assets mentioned therein. Controllers may then reference these assets (to be served here), via utility methods on
 * {@link BaseController}.</p>
 */
@Controller("/_/assets")
@Secured("isAnonymous()")
public class AssetController {
  /** Private logging pipe. */
  private static final @Nonnull Logger logging = Logging.logger(AssetController.class);

  /** Testing flag, indicating that compression should be forced, regardless of whether it saves time/space. */
  private static final boolean FORCE_COMPRESSION_IF_SUPPORTED = false;

  /** Static {@code Content-Encoding} value for non-compressed content. */
  private static final @Nonnull String IDENTITY_CONTENT_ENCODING = "identity";

  /** Static {@code Content-Encoding} value for gzip-compressed content. */
  private static final @Nonnull String GZIP_CONTENT_ENCODING = "gzip";

  /** Static {@code Content-Encoding} value for Brotli-compressed content. */
  private static final @Nonnull String BROTLI_CONTENT_ENCODING = "br";

  /** Static {@code Cross-Origin-Resource-Policy} header name. */
  private static final @Nonnull String CROSS_ORIGIN_RESOURCE_POLICY_HEADER = "Cross-Origin-Resource-Policy";

  /** Date/time tool for HTTP-formatted timestamps. */
  private static final @Nonnull SimpleDateFormat isoDateTimeFormat = (
    new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz"));

  /** Main asset manager singleton. */
  private final @Nonnull AssetManager assetManager;

  /** Configuration for the asset engine. */
  private final @Nonnull AssetConfiguration config;

  /** Replacement for {@link CspFilter} which disables itself when assets (or non-HTML content) are served. */
  @Replaces(CspFilter.class)
  public static final class AssetsAwareCspFilter extends CspFilter {
    AssetsAwareCspFilter(@Nonnull CspConfiguration cspConfiguration) {
      super(cspConfiguration);
    }

    private static boolean nonAssetRequest(@Nonnull HttpRequest request) {
      return !(request.getPath().startsWith(Core.getDynamicAssetPrefix()));
    }

    private @Nullable String nonceValue(@Nonnull HttpRequest<?> request) {
      return (this.cspConfiguration.isNonceEnabled() && nonAssetRequest(request)) ?
        this.cspConfiguration.generateNonce() : null;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
      String nonce = this.nonceValue(request);
      if (logging.isDebugEnabled())
        logging.debug(format("Generated nonce: '%s'.", nonce));
      return Flowable.fromPublisher(
        chain.proceed(request.setAttribute("cspNonce", nonce))).doOnNext((response) -> {
        if (nonAssetRequest(request)
            && request.getContentType().orElse(MediaType.TEXT_HTML_TYPE).equals(MediaType.TEXT_HTML_TYPE)) {
          if (logging.isDebugEnabled())
            logging.debug("Encountered a non-asset request. Applying CSP.");

          // it's not an asset request. apply CSP.
          this.cspConfiguration.getPolicyDirectives().map(StringUtils::trimToNull).ifPresent((directives) -> {
            String header = this.cspConfiguration.isReportOnly() ?
              "Content-Security-Policy-Report-Only" : "Content-Security-Policy";
            String headerValue;
            if (directives.contains("{#nonceValue}")) {
              if (nonce == null) {
                throw new IllegalArgumentException(
                  "Must enable CSP nonce generation to use '{#nonceValue}' placeholder.");
              }

              headerValue = directives.replace("{#nonceValue}", nonce);
            } else {
              headerValue = directives;
            }
            response.getHeaders().add(header, headerValue);
          });
        } else {
          // it's an asset request. let it through without appending CSP.
          if (logging.isDebugEnabled())
            logging.debug("CSP policy skipped because this request is being served as an asset.");
        }
      });
    }
  }

  /** Construct a new asset controller from scratch. Usually invoked from DI. */
  @Inject AssetController(@Nonnull AssetConfiguration config,
                          @Nonnull AssetManager assetManager) {
    this.config = config;
    this.assetManager = assetManager;
  }

  /**
   * Produce a token for the specified {@code Cross-Origin-Resource-Policy}.
   *
   * @param policy Policy to produce a token for.
   * @return String token.
   */
  @VisibleForTesting
  @SuppressWarnings("WeakerAccess")
  static @Nonnull Optional<String> tokenForCrossOriginResourcePolicy(@Nonnull CrossOriginResourcePolicy policy) {
    switch (policy) {
      case SAME_SITE: return Optional.of("same-site");
      case SAME_ORIGIN: return Optional.of("same-origin");
      case CROSS_ORIGIN: return Optional.of("cross-origin");
      default: return Optional.empty();
    }
  }

  /** Resolve a {@code Content-Encoding} value for the supplied {@code compressionMode}. */
  private @Nonnull CharSequence resolveContentEncoding(@Nonnull CompressionMode compressionMode) {
    switch (compressionMode) {
      case IDENTITY: return IDENTITY_CONTENT_ENCODING;
      case GZIP: return GZIP_CONTENT_ENCODING;
      case BROTLI: return BROTLI_CONTENT_ENCODING;
      default:
        throw new IllegalStateException(format("Unrecognized compression mode: '%s'.", compressionMode.name()));
    }
  }

  /** Resolve a {@code Content-Type} value for the supplied {@code ext}. */
  private @Nonnull MediaType resolveMediaType(@Nonnull String ext) {
    final MediaType mediaType;
    Optional<MediaType> mediaTypeResolved = MediaType.forExtension(ext);
    if (!mediaTypeResolved.isPresent()) {
      logging.error(format("Failed to resolve MIME type for asset extension '%s'", ext));
      mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
    } else {
      String charsetToken = StandardCharsets.UTF_8.displayName();
      mediaType = new MediaType(
        mediaTypeResolved.get().getName(),
        mediaTypeResolved.get().getExtension(),
        Collections.singletonMap("charset", charsetToken));
      if (logging.isTraceEnabled())
        logging.trace(format("Resolved MIME type for asset as '%s' (extension '%s', charset '%s').",
          mediaType.getType(),
          ext,
          charsetToken));
    }
    return mediaType;
  }

  /** Resolve an HTTP-compliant timestamp value from a Protobuf timestamp, for the {@code Last-Modified} header. */
  private @Nonnull Optional<String> resolveLastModified(@Nonnull Timestamp timestamp) {
    //noinspection ConstantConditions
    if (timestamp == null || !timestamp.isInitialized() || timestamp.getSeconds() < 100) {
      return Optional.empty();
    }
    return Optional.of(
      isoDateTimeFormat.format(Date.from(Instant.ofEpochSecond(timestamp.getSeconds()))));
  }

  /** Setup HTTP cache control settings. */
  private void resolveCacheControl(@Nonnull MutableHttpResponse response) {
    if (response.getHeaders().contains("Set-Cookie")) {
      logging.warn("Refusing to enable HTTP cache control on response: noticed `Set-Cookie` header.");
      return;
    }

    // resolve main TTL
    var mode = config.httpCaching().mode();
    var ttl = config.httpCaching().ttl();
    var ttlUnit = config.httpCaching().ttlUnit();
    var ttlValue = ttlUnit.convert(ttl, TimeUnit.SECONDS);

    StringBuilder spec = new StringBuilder();
    if (mode != null && !mode.isEmpty() && ttlValue > -1L) {
      if (logging.isDebugEnabled())
        logging.debug(format("Indicating `Cache-Control` mode and TTL: %s, max-age=%s.",
          mode,
          String.valueOf(ttl)));

      spec.append(format("%s; max-age=%s",
        mode,
        String.valueOf(ttl)));

      if (config.httpCaching().enableShared()) {
        // affix the shared-cache settings, too, if so directed
        var sharedTtl = config.httpCaching().sharedTtl();
        var sharedTtlUnit = config.httpCaching().sharedTtlUnit();
        var sharedTtlValue = sharedTtlUnit.convert(sharedTtl, TimeUnit.SECONDS);
        if (sharedTtlValue > -1L) {
          if (logging.isDebugEnabled())
            logging.debug(format("Indicating `Cache-Control` shared TTL: s-max-age=%s.",
              String.valueOf(sharedTtlValue)));

          spec.append(format(" s-max-age=%s%s",
            String.valueOf(sharedTtlValue),
            config.httpCaching().additionalDirectives().isPresent() ?
              " " + Joiner.on(" ").join(config.httpCaching().additionalDirectives().get()) :
              ""));

          if (logging.isDebugEnabled() && config.httpCaching().additionalDirectives().isPresent()) {
            logging.debug(format("Indicating `Cache-Control` additional directives: %s.",
              Joiner.on(" ").join(config.httpCaching().additionalDirectives().get())));
          }
        } else {
          logging.warn("`Cache-Control` shared caching was enabled, but a TTL value was invalid or missing.");
        }
      } else if (logging.isDebugEnabled()) {
        logging.debug("Shared HTTP caching is disabled.");
      }
    } else {
      logging.warn("`Cache-Control` value was invalid or missing.");
    }
    var renderedSpec = spec.toString();
    if (!renderedSpec.isEmpty()) {
      response.header(HttpHeaders.CACHE_CONTROL, renderedSpec);
    }
  }

  /** Affix headers for the asset to the response. */
  private void affixHeaders(@Nonnull MediaType type,
                            @Nonnull CompressionMode compression,
                            @Nonnull Timestamp modified,
                            @Nonnull AssetManager.ManagedAssetContent asset,
                            @Nonnull MutableHttpResponse response) {
    // start with content type and encodings
    response.contentType(type);
    var contentEncoding = resolveContentEncoding(compression);
    response.contentEncoding(contentEncoding);
    if (logging.isTraceEnabled()) {
      logging.trace(format("Indicating `Content-Encoding`: '%s'.", contentEncoding));
    }

    // next up is `Vary`
    if (config.variance().enabled()) {
      Set<String> segments = new TreeSet<>();

      // if the asset is a CSS or JS bundle, we must examine configuration to decide whether to specify `Accept` as a
      // `Vary` header entry, which is generally done to enable differential serving of photos (as WebP, for instance).
      if (type.getName().equals("css") || type.getName().equals("javascript") && config.variance().accept())
        segments.add(HttpHeaders.ACCEPT);
      else if (logging.isDebugEnabled())
        logging.debug(format("Asset type '%s' did not qualify for `Accept` variance.", type));

      // if the application is internationalized, we can indicate asset variance based on the provided request value of
      // the `Accept-Language` header, so apply that if it is configured.
      if (config.variance().language()) segments.add(HttpHeaders.ACCEPT_LANGUAGE);
      else if (logging.isDebugEnabled()) logging.debug("Asset variance by language is disabled in asset config.");

      // if the application is internationalized to the point of using custom/special charsets, we can indicate asset
      // variance based on the browser's accepted character sets. this is generally a bad idea, but is needed for some
      // corner cases, so we apply it here.
      if (config.variance().charset()) segments.add(HttpHeaders.ACCEPT_CHARSET);
      else if (logging.isDebugEnabled()) logging.debug("Asset variance by charset is disabled in asset config.");

      // if the application hosts dynamically originated content (i.e. in a multi-tenant posture), config can opt-in to
      // varying by browser origin.
      if (config.variance().origin()) segments.add(HttpHeaders.ORIGIN);
      else if (logging.isDebugEnabled()) logging.debug("Asset variance by variance is disabled in asset config.");

      // if we have more than one representation of the asset, and compression is enabled, and `Vary` is not disabled,
      // we must affix the `Vary` tag. generally for CSS and JS this might include `Accept`, so we look for that in the
      // asset configuration, too.
      if (config.compression().enabled() && config.compression().enableVary()
          && asset.getContent().getVariantList().size() > 1)
        segments.add(HttpHeaders.ACCEPT_ENCODING);
      else if (logging.isDebugEnabled())
        logging.debug("No need for compression variance: too few representations, or disabled by config.");

      if (!segments.isEmpty()) {
        String varyHeader = Joiner.on(", ").join(segments);
        if (logging.isDebugEnabled())
          logging.debug(format("Calculated `Vary` header value: '%s'.", varyHeader));
        response.header(HttpHeaders.VARY, varyHeader);

      } else if (logging.isDebugEnabled()) {
        logging.debug("No segments to apply to the `Vary` header value. Skipping.");
      }
    }

    // next up is etags and last-modified
    if (config.enableETags()) {
      if (logging.isDebugEnabled())
        logging.debug(format("Indicating `ETag`: '%s'.", asset.getETag()));
      response.header(HttpHeaders.ETAG, "\"" + asset.getETag() + "\"");
    } else if (logging.isDebugEnabled()) {
      logging.debug("`ETag`s are disabled.");
    }

    if (config.enableLastModified()) {
      Optional<String> lastModifiedValue = resolveLastModified(modified);
      if (lastModifiedValue.isPresent()) {
        String lastModified = lastModifiedValue.get();
        response.header(HttpHeaders.LAST_MODIFIED, lastModified);
        if (logging.isDebugEnabled())
          logging.debug(format("Indicating `Last-Modified`: '%s'.", lastModified));
      } else {
        logging.warn(format("`Last-Modified` headers are enabled, but none could be generated for asset '%s'.",
          asset.getToken()));
      }
    } else if (logging.isDebugEnabled()) {
      logging.debug("`Last-Modified` is disabled.");
    }

    // mention original filename if operating in debug mode
    if (Core.isDebugMode()) {
      logging.debug(format("Serving managed asset '%s'.", asset.getFilename()));
      response.header(HttpHeaders.CONTENT_DISPOSITION, format("inline; filename=\"%s\"", asset.getFilename()));
    }

    // `Cross-Origin-Resource-Policy`
    if (config.crossOriginResources().enabled()) {
      Optional<String> policyToken = tokenForCrossOriginResourcePolicy(
        config.crossOriginResources().policy());
      if (policyToken.isPresent()) {
        if (logging.isDebugEnabled())
          logging.debug(format("Applying `Cross-Origin-Resource-Policy` '%s'.", policyToken.get()));
        response.getHeaders().add(
          CROSS_ORIGIN_RESOURCE_POLICY_HEADER,
          policyToken.get());
      }
    } else if (logging.isTraceEnabled()) {
      logging.trace("No `Cross-Origin-Resource-Policy` applied: policy token was not present.");
    }

    // apply HTTP caching, if enabled
    if (config.httpCaching().enabled()) {
      if (logging.isDebugEnabled())
        logging.debug("HTTP caching is enabled.");
      resolveCacheControl(response);
    } else if (logging.isDebugEnabled()) {
      logging.debug("HTTP caching is disabled.");
    }

    // apply no-sniff policy
    if (config.enableNoSniff()) {
      // @TODO get this into Micronaut main
      response.header("X-Content-Type-Options", "nosniff");
    }
  }

  /** Calculate the set of supported compression modes for a given HTTP request's Accept-Encoding header. */
  private EnumSet<CompressionMode> supportedCompressionModes(@Nonnull HttpRequest request) {
    if (request.getHeaders().contains(HttpHeaders.ACCEPT_ENCODING)) {
      @Nonnull String encodingSpec = Objects.requireNonNull(request.getHeaders().get(HttpHeaders.ACCEPT_ENCODING));
      EnumSet<CompressionMode> modes = EnumSet.allOf(CompressionMode.class);
      modes.remove(CompressionMode.UNRECOGNIZED);

      for (CompressionMode mode : CompressionMode.values()) {
        final String token;
        switch (mode) {
          case GZIP: token = "gzip"; break;
          case BROTLI: token = "br"; break;
          case IDENTITY:
          case UNRECOGNIZED: continue;
          default:
            throw new IllegalStateException(format("Unsupported compression mode: '%s'.", mode.name()));
        }

        if (!encodingSpec.contains(token))
          modes.remove(mode);
      }
      return modes;
    }
    // no compression support indicated at all
    return EnumSet.noneOf(CompressionMode.class);
  }

  /** Check if the request is conditional, and, if so, if the conditions match the static asset. */
  private boolean conditionalRequestMatches(@Nonnull HttpRequest request,
                                            @Nonnull AssetManager.ManagedAssetContent asset) {
    if (config.enableETags() && request.getHeaders().contains(HttpHeaders.IF_NONE_MATCH)) {
      // the request has an etag, and etags are on. match them.
      String etagValue = request.getHeaders().get(HttpHeaders.IF_NONE_MATCH);
      if (logging.isTraceEnabled()) {
        logging.trace(format("`ETag` value from asset: '%s'", asset.getETag()));
        logging.trace(format("`ETag` value from request: '%s'", etagValue));
      }

      boolean match = (asset.getETag().equals(etagValue));
      if (match && logging.isDebugEnabled()) {
        logging.debug("`ETag` value matched.");
      } else if (logging.isDebugEnabled()) {
        logging.debug("`ETag` value did not match.");
      }
      return match;

    } else if (config.enableLastModified() && request.getHeaders().contains(HttpHeaders.IF_MODIFIED_SINCE)) {
      // the request has an if-modified-since, and last-modified is on. calculate a result.
      try {
        var ifModifiedSince = request.getHeaders().get(HttpHeaders.IF_MODIFIED_SINCE);
        if (logging.isTraceEnabled())
          logging.trace(format("Parsing '%s' `If-Modified-Since` value...", ifModifiedSince));
        Date parsed = isoDateTimeFormat.parse(ifModifiedSince);

        if (parsed != null) {
          long requestSince = parsed.toInstant().getEpochSecond();
          long assetTimestamp = asset.getLastModified().getSeconds();
          boolean match = assetTimestamp <= requestSince;

          if (logging.isDebugEnabled()) {
            logging.debug(format("Parsed `If-Modified-Since` at timestamp %s.", requestSince));
            logging.debug(format("Asset timestamp loaded as %s.", assetTimestamp));
            if (match) logging.debug("Asset timestamp occurs before-or-at request. Match.");
            else logging.debug("Asset timestamp occurs after request. No match.");
          }
          return match;
        }
      } catch (ParseException err) {
        logging.error(format("Failed to parse `If-Modified-Since`: %s.", err.getMessage()));
      }
    }
    return false;  // either nothing is on, or the request was not conditional.
  }

  /** Choose the optimal variant to serve, and serve it. */
  private Flowable<HttpResponse> chooseAndServeVariant(@Nonnull HttpRequest request,
                                                       @Nonnull MediaType type,
                                                       @Nonnull AssetManager.ManagedAssetContent asset,
                                                       @Nonnull MutableHttpResponse<byte[]> response) {
    if (this.conditionalRequestMatches(request, asset)) {
      if (logging.isDebugEnabled())
        logging.debug("Conditional request matches response. Serving 304-Not-Modified.");
      return Flowable.just(HttpResponse.notModified());
    } else if (logging.isDebugEnabled()) {
      if (!config.enableETags() && !config.enableLastModified()) logging.debug(
        "Conditional requests were not considered because `ETag`s and `Last-Modified` are disabled.");
      else
        logging.debug("Request was not conditional, or did not match.");
    }

    // based on the request, calculate supported compression modes
    EnumSet<CompressionMode> supportedCompressions = supportedCompressionModes(request);
    EnumSet<CompressionMode> supportedVariants = asset.getCompressionOptions();
    ImmutableSet<CompressionMode> compressionOptions = Sets.immutableEnumSet(
      Sets.intersection(
        Sets.intersection(supportedCompressions, supportedVariants),
        config.compression().compressionModes()));

    if (logging.isTraceEnabled()) {
      // describe the conditions that led to our compression decisions
      logging.trace(format("Client indicated compression support: '%s'.", Joiner.on(", ").join(
        supportedCompressions.stream().map(Enum::name).collect(Collectors.toSet()))));
      logging.trace(format("Asset variant options: '%s'.", Joiner.on(", ").join(
        supportedVariants.stream().map(Enum::name).collect(Collectors.toSet()))));
    }

    final Optional<CompressedData> resolvedData;

    if (/* if we can't use compression... */ supportedCompressions.size() == 0 ||
        /* or compression is disabled entirely... */ (!config.compression().enabled()) ||
        /* or no compression algorithms are enabled... */ (config.compression().compressionModes().isEmpty()) ||
        /* or the optimal compression is none... */ (asset.getOptimalCompression() == CompressionMode.IDENTITY &&
        /* and we aren't forcing compression where available... */ !FORCE_COMPRESSION_IF_SUPPORTED) ||
        /* or the variant has no compressed options... */ asset.getVariantCount() < 2) {
      if (logging.isDebugEnabled()) {
        // explain why we got here
        if (supportedVariants.size() == 0) logging.debug("Compression disabled: unsupported by asset.");
        else if (supportedCompressions.size() == 0) logging.debug("Compression disabled: unsupported by client.");
        else if (compressionOptions.size() == 0) logging.debug("Compression disabled: no agreement with client.");
        else if (asset.getVariantCount() < 2) logging.debug("Compression disabled: no variance for asset.");
        else if (asset.getOptimalCompression() == CompressionMode.IDENTITY)
          logging.debug("Compression disabled: un-compressed is optimal.");
      }

      // no ability to serve a compressed response.
      resolvedData = asset.getContent().getVariantList()
        .stream()
        .filter((bundle) -> bundle.getCompression().equals(CompressionMode.IDENTITY))
        .findFirst();
    } else {
      // if the optimal compression choice is supported by the client, choose it
      var optimal = asset.getOptimalCompression();
      if ((!FORCE_COMPRESSION_IF_SUPPORTED || !optimal.equals(CompressionMode.IDENTITY))
          && compressionOptions.contains(optimal)) {
        if (logging.isDebugEnabled())
          logging.debug(format("Optimal compression (%s) is supported and was selected.",
            optimal.name()));

        resolvedData = asset.getContent().getVariantList()
          .stream()
          .filter((bundle) -> bundle.getCompression().equals(optimal))
          .findFirst();
      } else {
        // otherwise, pick the next-optimal-choice supported by the client
        resolvedData = compressionOptions.stream()
          .flatMap((mode) -> asset.getContent().getVariantList().stream()
            .filter((variant) -> !variant.getCompression().equals(CompressionMode.IDENTITY))
            .filter((variant) -> compressionOptions.contains(variant.getCompression())))
          .peek((variant) -> {
            if (logging.isDebugEnabled()) {
              logging.debug(format("Considering variant of size %sb (%s)...",
                variant.getSize(),
                variant.getCompression().name()));
            }
          })
          .min(Comparator.comparing(CompressedData::getSize));

        if (resolvedData.isPresent() && logging.isDebugEnabled()) {
          logging.debug(format("Selected variant %s, of size %s bytes.",
            resolvedData.get().getCompression().name(),
            resolvedData.get().getSize()));
        }
      }
    }

    if (!resolvedData.isPresent()) {
      // we were unable to agree on a content variant. this essentially should not happen.
      logging.warn("Failed to agree with client on a variant for asset. Failing.");
      return Flowable.just(HttpResponse.badRequest("UNSUPPORTED_ENCODING"));
    } else {
      // we've resolved the correct data for the body. calculate headers and send it.
      this.affixHeaders(
        type,
        resolvedData.get().getCompression(),
        asset.getLastModified(),
        asset,
        response);

      byte[] assetPayload = resolvedData
        .get()
        .getData()
        .getRaw()
        .toByteArray();

      if (logging.isDebugEnabled()) {
        logging.debug(format("Serving payload of size (%s bytes) for asset '%s'.",
          assetPayload.length,
          asset.getToken()));
      }
      return Flowable.just(response.body(assetPayload));
    }
  }

  /**
   * Main GET serving endpoint for dynamic managed assets. Assets come through this endpoint mentioning their unique
   * token and file extension, and we do the rest.
   *
   * <p>We accomplish this by (1) querying the {@link AssetManager} for a matching asset (404 is yielded if a match
   * cannot be located), then (2) calculating headers and an appropriate variant for the subject asset, and finally (3)
   * writing the headers and the asset body to the response.</p>
   *
   * @return Response
   */
  @Validated
  @Get(value = "/{asset}.{ext}", produces = {
    "text/html",
    "text/css",
    "application/javascript"
  })
  public @Nonnull Flowable<HttpResponse> asset(@Nonnull String asset,
                                               @Nonnull String ext,
                                               @Nonnull HttpRequest request) {
    //noinspection ConstantConditions
    if (asset == null || ext == null
        || asset.isEmpty() || ext.isEmpty()
        || asset.length() < 2 || ext.length() < 2) {
      logging.warn("Invalid asset token or extension. Returning 400.");
      return Flowable.just(HttpResponse.badRequest());
    }

    // okay, resolve content through the cache, backed by the asset manager
    @Nonnull Optional<AssetManager.ManagedAssetContent> assetContent = this.assetManager.assetDataByToken(asset);
    if (assetContent.isPresent() && assetContent.get().getVariantCount() > 0) {
      // make sure the user is asking for this kind of content
      var content = assetContent.get();
      String[] ogFilename = content.getFilename().split("\\.");
      if (ogFilename.length > 1 && ogFilename[ogFilename.length - 1].equals(ext)) {
        // create response, affix other asset headers, and serve
        return this.chooseAndServeVariant(
          request,
          resolveMediaType(ext),
          content,
          HttpResponse.ok());
      } else {
        logging.warn(format("Asset extension mismatch: '%s' does not match expected value for asset '%s' ('%s').",
          ext,
          asset,
          ogFilename[ogFilename.length - 1]));
      }
    }
    logging.warn(format("Asset '%s' not found.", asset));
    return Flowable.just(HttpResponse.notFound());
  }
}
