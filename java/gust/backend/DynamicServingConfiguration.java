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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.bind.annotation.Bindable;
import tools.elide.page.Context.ClientHint;
import tools.elide.page.Context.FramingPolicy;
import tools.elide.page.Context.ReferrerPolicy;

import java.util.*;
import java.util.concurrent.TimeUnit;


/** Supplies configuration structure for dynamically-served app pages through Gust. */
@ConfigurationProperties("gust.serving")
public interface DynamicServingConfiguration {
  /** Set of sensible defaults for Gust dynamic serving. */
  DynamicServingConfiguration DEFAULTS = new DynamicServingConfiguration() {};

  /** Value to set for {@code Content-Language}. May be overridden by context. */
  @Bindable("language") default Optional<String> language() {
    return Optional.of("en-US");
  }

  /** {@code ETag} configuration for dynamic content. */
  @Bindable("etags") default DynamicETagsConfiguration etags() {
    return DynamicETagsConfiguration.DEFAULTS;
  }

  /** {@code Vary} configuration for dynamic content. */
  @Bindable("vary") default DynamicVarianceConfiguration variance() {
    return DynamicVarianceConfiguration.DEFAULTS;
  }

  /** {@code X-Frame-Options} configuration for dynamic content. */
  @Bindable("framingPolicy") default FramingPolicy framingPolicy() {
    return FramingPolicy.DENY;
  }

  /** Hostnames to pre-connect to from the browser. */
  @Bindable("preconnect") default List<String> preconnect() {
    return Collections.emptyList();
  }

  /** Hostnames to pre-load into the browser's DNS. */
  @Bindable("dnsPrefetch") default List<String> dnsPrefetch() {
    return Collections.emptyList();
  }

  /** Whether to apply {@code nosniff} to {@code X-Content-Type-Options} for dynamic content. */
  @Bindable("noSniff") default Boolean noSniff() {
    return true;
  }

  /** {@code Feature-Policy} configuration for dynamic content. */
  @Bindable("featurePolicy") default FeaturePolicyConfiguration featurePolicy() {
    return FeaturePolicyConfiguration.DEFAULTS;
  }

  /** {@code Referrer-Policy} configuration for dynamic content. */
  @Bindable("referrerPolicy") default ReferrerPolicy referrerPolicy() {
    return ReferrerPolicy.STRICT_ORIGIN;
  }

  /** {@code X-XSS-Protection} configuration for dynamic content. */
  @Bindable("xssProtection") default XSSProtectionConfiguration xssProtection() {
    return XSSProtectionConfiguration.DEFAULTS;
  }

  /** Settings related to support for Client Hints. */
  @Bindable("clientHints") default ClientHintsConfiguration clientHints() {
    return ClientHintsConfiguration.DEFAULTS;
  }

  /** Arbitrary headers to add to all responses. */
  @Bindable("additionalHeaders") default Map<String, String> additionalHeaders() {
    return Collections.emptyMap();
  }

  /** Describes settings regarding {@code ETag} headers for dynamic content. */
  @ConfigurationProperties("gust.serving.etags")
  interface DynamicETagsConfiguration {
    /** Sensible defaults for dynamic etags. */
    DynamicETagsConfiguration DEFAULTS = new DynamicETagsConfiguration() {};

    /** Whether to enable {@code ETag} headers for dynamically-served content. */
    @Bindable("enabled") default Boolean enabled() {
      return true;
    }
  }

  /** Describes settings regarding {@code Feature-Policy} headers for dynamic content. */
  @ConfigurationProperties("gust.serving.featurePolicy")
  interface FeaturePolicyConfiguration {
    /** Sensible defaults for {@code Feature-Policy}. */
    FeaturePolicyConfiguration DEFAULTS = new FeaturePolicyConfiguration() {};

    /** Whether to enable {@code Feature-Policy} headers for dynamically-served content. */
    @Bindable("enabled") default Boolean enabled() {
      return true;
    }

    /** Specifies the default {@code Feature-Policy} to apply to dynamically-served content. */
    @Bindable("policy") default SortedSet<String> policy() {
      return ImmutableSortedSet.of(
        "document-domain 'none';",
        "legacy-image-formats 'none';",
        "oversized-images 'none';",
        "sync-xhr 'none';",
        "unoptimized-images 'none';"
      );
    }
  }

  /** Describes settings related to Client Hints support. */
  @ConfigurationProperties("gust.serving.clientHints")
  interface ClientHintsConfiguration {
    /** Sensible defaults for client hints. */
    ClientHintsConfiguration DEFAULTS = new ClientHintsConfiguration() {};

    /** Whether to enable support for client hints. */
    @Bindable("enabled") default Boolean enabled() {
      return true;
    }

    /** Return the set of hints supported by the server. */
    @Bindable("hints") default ImmutableSet<ClientHint> hints() {
      return Sets.immutableEnumSet(ClientHint.ECT, ClientHint.RTT, ClientHint.DPR);
    }

    /** Client Hints configuration time-to-live value. */
    @Bindable("ttl") default Optional<Long> ttl() {
      return Optional.of(7L);
    }

    /** Client Hints configuration time-to-live unit. Defaults to {@code DAYS}. */
    @Bindable("ttlUnit") default TimeUnit ttlUnit() {
      return TimeUnit.DAYS;
    }
  }

  /** Describes settings regarding {@code Vary} headers for dynamic content. */
  @ConfigurationProperties("gust.serving.vary")
  interface DynamicVarianceConfiguration {
    /** Sensible defaults for dynamic page variance. */
    DynamicVarianceConfiguration DEFAULTS = new DynamicVarianceConfiguration() {};

    /** Whether to enable {@code Vary} headers for dynamically-served content. */
    @Bindable("enabled") default Boolean enabled() {
      return true;
    }

    /** Whether to indicate response variance by {@code Accept}. */
    @Bindable("accept") default Boolean accept() {
      return true;
    }

    /** Whether to indicate response variance by {@code Accept-Charset}. */
    @Bindable("charset") default Boolean charset() {
      return false;
    }

    /** Whether to indicate response variance by {@code Accept-Encoding}. */
    @Bindable("encoding") default Boolean encoding() {
      return true;
    }

    /** Whether to indicate response variance by {@code Accept-Language}. */
    @Bindable("language") default Boolean language() {
      return false;
    }

    /** Whether to indicate response variance by {@code Origin}. */
    @Bindable("origin") default Boolean origin() {
      return false;
    }
  }

  /** Describes settings regarding {@code X-XSS-Protection} headers for dynamic content. */
  @ConfigurationProperties("gust.serving.xssProtection")
  interface XSSProtectionConfiguration {
    /** Sensible defaults for cross-site scripting protection. */
    XSSProtectionConfiguration DEFAULTS = new XSSProtectionConfiguration() {};

    /** Whether to enable old-style XSS protection. */
    @Bindable("enabled") default Boolean enabled() {
      return true;
    }

    /** Whether to specify XSS protection as active. */
    @Bindable("filter") default Boolean filter() {
      return true;
    }

    /** Whether to add the {@code block} flag. */
    @Bindable("block") default Boolean block() {
      return true;
    }
  }
}
