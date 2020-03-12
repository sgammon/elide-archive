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


import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.bind.annotation.Bindable;
import tools.elide.core.data.CompressionMode;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;


/** App configuration bindings for asset management and serving. */
@ConfigurationProperties("gust.assets")
public interface AssetConfiguration {
  /** Sensible defaults for asset configuration. */
  AssetConfiguration DEFAULTS = new AssetConfiguration() {};

  /** Specifies a bump value to apply to all asset URLs. */
  @Bindable("bump") default Optional<Integer> bump() {
    return Optional.empty();
  }

  /** Specifies whether to affix {@code ETag} values. */
  @Bindable("etags") default Boolean enableETags() {
    return true;
  }

  /** Specifies whether to affix {@code Last-Modified} values. */
  @Bindable("lastModified") default Boolean enableLastModified() {
    return true;
  }

  /** Specifies whether to affix a {@code X-Content-Type-Options} policy for {@code nosniff}. */
  @Bindable("noSniff") default Boolean enableNoSniff() {
    return true;
  }

  /** Specifies settings regarding CDN use. */
  @Bindable("cdn") default ContentDistributionConfiguration cdn() {
    return ContentDistributionConfiguration.DEFAULTS;
  }

  /** Specifies settings for HTTP compression. */
  @Bindable("compression") default AssetCompressionConfiguration compression() {
    return AssetCompressionConfiguration.DEFAULTS;
  }

  /** Specifies settings for HTTP caching. */
  @Bindable("httpCaching") default AssetCachingConfiguration httpCaching() {
    return AssetCachingConfiguration.DEFAULTS;
  }

  /** Describes settings regarding compressed asset serving. */
  @ConfigurationProperties("gust.assets.compression")
  interface AssetCompressionConfiguration {
    /** Sensible defaults for asset compression. */
    AssetCompressionConfiguration DEFAULTS = new AssetCompressionConfiguration() {};

    /** Whether to enable serving of pre-compressed assets. */
    @Bindable("enabled") default Boolean enabled() {
      return true;
    }

    /** Whether to enable serving of pre-compressed assets. */
    @Bindable("modes") default List<CompressionMode> compressionModes() {
      return Collections.singletonList(CompressionMode.GZIP);
    }
  }

  /** Describes the structure of asset caching configuration. */
  @ConfigurationProperties("gust.assets.httpCaching")
  interface AssetCachingConfiguration {
    /** Sensible defaults for asset caching over HTTP. */
    AssetCachingConfiguration DEFAULTS = new AssetCachingConfiguration() {};

    /** Whether to enable intelligent HTTP caching for assets served dynamically. */
    @Bindable("enabled") default Boolean enabled() {
      return false;
    }

    /** Main mode to apply with regard to HTTP caching for assets served dynamically. */
    @Bindable("mode") default String mode() {
      return "private";
    }

    /** Additional directives to inject into the HTTP caching header. */
    @Bindable("additionalDirectives") default Optional<List<String>> additionalDirectives() {
      return Optional.empty();
    }

    /** Time-to-live value to apply to the main HTTP cache directive. Units tunable with {@link #ttlUnit()}. */
    @Bindable("ttl") default Long ttl() {
      return 300L;
    }

    /** Whether to enable a shared-cache directive in the HTTP cache settings. */
    @Bindable("shared") default Boolean enableShared() {
      return false;
    }

    /** When a shared-cache directive is enabled, this sets the TTL for shared caches. */
    @Bindable("sharedTtl") default Long sharedTtl() {
      return 86400L;
    }

    /** Time unit to apply to the value specified by {@link #ttl()}. Defaults to {@code SECONDS}. */
    default TimeUnit ttlUnit() {
      return TimeUnit.SECONDS;
    }

    /** Time unit to apply to the value specified by {@link #sharedTtl()}. Defaults to {@code SECONDS}. */
    default TimeUnit sharedTtlUnit() {
      return TimeUnit.SECONDS;
    }
  }

  /** Describes the structure of CDN-related asset settings. */
  @ConfigurationProperties("gust.assets.cdn")
  interface ContentDistributionConfiguration {
    /** Sensible defaults for asset CDN settings. */
    ContentDistributionConfiguration DEFAULTS = new ContentDistributionConfiguration() {};

    /** Whether to enable CDN features. */
    @Bindable("enabled") default Boolean enabled() {
      return false;
    }

    /** CDN host names to use for assets. When running over HTTP/2, only the first hostname is employed. */
    @Bindable("hostnames") default SortedSet<String> hostnames() {
      return Collections.emptySortedSet();
    }
  }
}
