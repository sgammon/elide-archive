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
import gust.backend.runtime.Logging;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import org.slf4j.Logger;
import tools.elide.page.Context.ClientHint;
import tools.elide.page.Context.FramingPolicy;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;

import static java.lang.String.format;


/**
 * Describes a framework application controller, which is pre-loaded with convenient access to tooling, and any injected
 * application logic. Alternatively, if the invoking developer wishes to use their own base class, they can acquire most
 * or all of the enclosed functionality via injection.
 *
 * <p>Various properties are made available to sub-classes which allow easy access to stuff like:</p>
 * <ul>
 *   <li><b>{@code context}:</b> This property exposes a builder for the current flow's page context. This can be
 *       manipulated to provide/inject properties, and then subsequently used in Soy.</li>
 * </ul>
 *
 * <p>To make use of this controller, simply inherit from it in your own {@code @Controller}-annotated class. When a
 * request method is invoked, the logic provided by this object will have been initialized and will be ready to use.</p>
 */
@SuppressWarnings("unused")
public abstract class AppController extends BaseController {
  /** Private logging pipe for {@code AppController}. */
  private final Logger logging = Logging.logger(AppController.class);

  /** Pre-ordained HTML object which ensures the character encoding is set to UTF-8. */
  protected final static @Nonnull MediaType HTML;

  /** Configuration for dynamic serving. */
  private @Inject DynamicServingConfiguration servingConfiguration;

  /** Configuration for dynamic serving. */
  private @Inject AssetConfiguration assetConfiguration;

  static {
    // initialize HTML page type
    HTML = new MediaType(MediaType.TEXT_HTML_TYPE.getName(), MediaType.TEXT_HTML_TYPE.getExtension(),
      Collections.singletonMap("charset", StandardCharsets.UTF_8.displayName()));
  }

  /**
   * Initialize a new application controller.
   *
   * @param context Injected page context manager.
   */
  protected AppController(@Nonnull PageContextManager context) {
    super(context);
  }

  // -- API: Trusted Resources (Delegated to Context) -- //

  /**
   * Generate a trusted resource URL for the provided Java URL.
   *
   * @param url Pre-ordained trusted resource URL.
   * @return Trusted resource URL specification proto.
   */
  public @Nonnull TrustedResourceUrlProto trustedResource(@Nonnull URL url) {
    return context.trustedResource(url);
  }

  /**
   * Generate a trusted resource URL for the provided Java URI.
   *
   * @param uri Pre-ordained trusted resource URI.
   * @return Trusted resource URL specification proto.
   */
  public @Nonnull TrustedResourceUrlProto trustedResource(@Nonnull URI uri) {
    return context.trustedResource(uri);
  }

  // -- API: Configurable Serving -- //

  /**
   * Affix headers to the provided response, according to the provided app {@code config} for dynamic serving. The
   * resulting response is kept mutable for further changes.
   *
   * @param response HTTP response to affix headers to.
   * @return HTTP response, with headers affixed.
   */
  @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
  protected @Nonnull <T> MutableHttpResponse<T> affixHeaders(@Nonnull MutableHttpResponse<T> response,
                                                             @Nonnull DynamicServingConfiguration config) {
    // first up: content language
    if (config.language().isPresent()) {
      logging.debug(format("Affixing `Content-Language` header from config: '%s'.", config.language().get()));
      response.setAttribute("language", config.language().get());
      this.context.language(config.language());
    } else if (logging.isTraceEnabled()) {
      logging.trace("No `Content-Language` header value to set.");
    }

    // next up: etags
    if (config.etags().enabled()) {
      if (logging.isTraceEnabled())
        logging.trace("Dynamic `ETag` values are enabled.");
      this.context.enableETags(true);
    } else if (logging.isTraceEnabled()) {
      logging.trace("Dynamic `ETag` values are disabled.");
    }

    // next up: client hints
    if (config.clientHints().enabled()) {
      Set<ClientHint> hints = config.clientHints().hints();
      if (!hints.isEmpty()) {
        if (logging.isDebugEnabled())
          logging.debug(format("Client hints are ENABLED. Applying hints: '%s'.", Joiner.on(", ").join(hints)));
        this.context.supportedClientHints(
          Optional.of(hints),
          config.clientHints().ttl().isPresent() ?
            Optional.of(config.clientHints().ttlUnit().toSeconds(config.clientHints().ttl().get())) :
            Optional.empty());

      } else if (logging.isTraceEnabled()) {
        logging.trace("No client hints are enabled.");
      }
    } else if (logging.isTraceEnabled()) {
      logging.trace("Client hints are DISABLED.");
    }

    // next up: vary
    if (config.variance().enabled()) {
      Set<String> varySet = new TreeSet<>();
      response.setAttribute("vary", varySet);
      DynamicServingConfiguration.DynamicVarianceConfiguration varyConfig = config.variance();

      BiConsumer<Boolean, String> affixVarySegment = (condition, header) -> {
        if (condition) {
          varySet.add(header);
          if (logging.isDebugEnabled()) {
            logging.debug(format("Indicating dynamic response variance by `%s` (due to config).", header));
          }
        } else if (logging.isTraceEnabled()) {
          logging.trace(format("Dynamic response variance by `%s` is DISABLED (due to config).", header));
        }
      };

      affixVarySegment.accept(varyConfig.accept(), HttpHeaders.ACCEPT);  // `Accept`
      affixVarySegment.accept(varyConfig.charset(), HttpHeaders.ACCEPT_CHARSET);  // `Accept-Charset`
      affixVarySegment.accept(varyConfig.encoding(), HttpHeaders.ACCEPT_ENCODING);  // `Accept-Encoding`
      affixVarySegment.accept(varyConfig.language(), HttpHeaders.ACCEPT_LANGUAGE);  // `Accept-Language`
      affixVarySegment.accept(varyConfig.origin(), HttpHeaders.ORIGIN);  // `Origin`
      if (!varySet.isEmpty()) {
        if (logging.isDebugEnabled())
          logging.debug(format("Indicating configured variance for response: '%s'.", Joiner.on(", ").join(varySet)));
        this.context.vary(varySet);
      } else if (logging.isTraceEnabled()) {
        logging.trace("No variance configured for response.");
      }
    }

    // next up: feature policy
    if (config.featurePolicy().enabled()) {
      SortedSet<String> featurePolicies = config.featurePolicy().policy();
      if (logging.isDebugEnabled())
        logging.debug(format("Indicating `Feature-Policy` for response: '%s'.", Joiner.on(", ").join(featurePolicies)));
      featurePolicies.forEach((policy) ->
        this.context.getContext().addFeaturePolicy(policy));
    } else if (logging.isDebugEnabled()) {
      logging.debug("`Feature-Policy` disabled via config.");
    }

    // next up: framing policy
    if (config.framingPolicy() != FramingPolicy.DEFAULT_FRAMING_POLICY) {
      this.context.getContext().setFramingPolicy(config.framingPolicy());
      if (logging.isDebugEnabled())
        logging.debug(format("Applying `X-Frame-Options` policy: '%s'.", config.framingPolicy().name()));
    } else if (logging.isDebugEnabled()) {
      logging.debug("`X-Frame-Options` disabled via config.");
    }

    // next up: DNS domain prefetch
    if (!config.dnsPrefetch().isEmpty()) {
      if (logging.isDebugEnabled())
        logging.debug(format("Pre-fetching %s domains via browser DNS: '%s'.",
          config.dnsPrefetch().size(),
          Joiner.on(", ").join(config.dnsPrefetch())));

      this.context.dnsPrefetch(config.dnsPrefetch());

    } else if (logging.isDebugEnabled()) {
      logging.debug("No domains to prefetch via DNS.");
    }

    // next up: domain pre-connect
    if (!config.preconnect().isEmpty()) {
      if (logging.isDebugEnabled())
        logging.debug(format("Pre-connecting to %s domains via browser: '%s'.",
          config.dnsPrefetch().size(),
          Joiner.on(", ").join(config.preconnect())));

      this.context.preconnect(config.preconnect());

    } else if (logging.isDebugEnabled()) {
      logging.debug("No domains to pre-connect to.");
    }

    // next up: `nosniff`
    if (config.noSniff()) {
      if (logging.isDebugEnabled())
        logging.debug("Indicating `nosniff` for `X-Content-Type-Options`.");
      this.context.getContext().setContentTypeNosniff(true);
    } else if (logging.isDebugEnabled())
      logging.debug("`X-Content-Type-Options` disabled via config.");

    // next up: `X-XSS-Protection`
    if (config.xssProtection().enabled()) {
      String xssProtectToken = config.xssProtection().filter() ? "1" : "0";
      String modeToken = config.xssProtection().block() ? "; mode=block" : "";
      String composedHeader = xssProtectToken + modeToken;

      if (logging.isDebugEnabled())
        logging.debug(format("Old-style `X-XSS-Protection` is enabled. Affixing header: '%s'.", composedHeader));
      this.context.getContext().setXssProtection(composedHeader);
    } else if (logging.isDebugEnabled())
      logging.debug("Old-style `X-XSS-Protection` is disabled by configuration.");

    // finally: arbitrary headers
    if (!config.additionalHeaders().isEmpty()) {
      config.additionalHeaders().forEach(this.context::header);
    }
    return response;
  }

  /**
   * Affix headers to the provided response, according to current app configuration for dynamic serving. The resulting
   * response is kept mutable for further changes.
   *
   * @param response HTTP response to affix headers to.
   * @return HTTP response, with headers affixed.
   */
  protected @Nonnull <T> MutableHttpResponse<T> affixHeaders(@Nonnull MutableHttpResponse<T> response) {
    return affixHeaders(response, DynamicServingConfiguration.DEFAULTS);
  }

  /**
   * Select the set of CDN prefixes to use in this HTTP cycle, from the configured set. Once this action completes, the
   * set of CDN prefixes is considered "frozen" for this cycle.
   *
   * @param render Page context manager, after the handler has completed.
   * @param response Response object, on which we should set the CDN prefix property.
   * @param servingConfig Serving configuration from which to calculate the active set of CDN prefixes.
   */
  @SuppressWarnings("WeakerAccess")
  protected void selectCdnPrefixes(@Nonnull PageContextManager render,
                                   @Nonnull MutableHttpResponse response,
                                   @Nonnull AssetConfiguration servingConfig) {
    if (!render.getCdnPrefix().isPresent()) {
      // resolve CDN prefix to use for this run
      if (servingConfig.cdn().enabled()) {
        List<String> hostnames = servingConfig.cdn().hostnames();
        if (hostnames.isEmpty() && logging.isDebugEnabled()) {
          logging.debug("No CDN prefixes available.");
        } else {
          final String hostname;
          if (hostnames.size() == 1) {
            hostname = hostnames.get(0);
          } else {
            // select one
            hostname = hostnames.get(new Random().nextInt(hostnames.size()));
          }
          if (logging.isDebugEnabled())
            logging.debug(format("Selected CDN prefix for HTTP cycle: '%s'.", hostname));
          response.setAttribute("cdn_prefix", hostname);
          render.cdnPrefix(Optional.of(hostname));

          // add to pre-connect list and DNS prefetch list
          render.dnsPrefetch(hostname);
          render.preconnect(hostname);
        }

      } else if (logging.isDebugEnabled()) {
        logging.debug("CDN prefix skipped (DISABLED via config).");
      }
    } else if (logging.isDebugEnabled()) {
      logging.debug("Deferring to developer-specified CDN prefix.");
    }
  }

  /**
   * Serve the provided rendered-page response, while applying any app configuration related to dynamic page headers.
   * This may include headers like {@code Vary}, {@code ETag}, and so on, which may be calculated based on the response
   * intended to be provided to the client.
   *
   * @param render Page render to perform before responding.
   * @return Prepped and rendered HTTP response.
   */
  protected @Nonnull MutableHttpResponse<PageRender> serve(@Nonnull PageContextManager render) {
    DynamicServingConfiguration servingConfig = (
      this.servingConfiguration != null ? this.servingConfiguration : DynamicServingConfiguration.DEFAULTS);
    AssetConfiguration assetConfig = (
      this.assetConfiguration != null ? this.assetConfiguration : AssetConfiguration.DEFAULTS);

    // order matters here. `selectCdnPrefix` must be called first, to load any CDN prefix before link pre-loads go out.
    // next, `affixHeaders` must be called before `render`, which produces the `ctx` that is set on the response (so it
    // so it may be picked up in `PageContextManager#finalizeResponse`).
    MutableHttpResponse<PageRender> response = HttpResponse.ok(render);
    selectCdnPrefixes(render, response, assetConfig);
    this.affixHeaders(response, servingConfig);
    response.setAttribute("context", render.render().getPageContext());
    return response;
  }
}
