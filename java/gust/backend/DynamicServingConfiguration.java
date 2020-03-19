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
import com.google.common.collect.Sets;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.bind.annotation.Bindable;
import tools.elide.page.Context.ClientHint;

import java.util.*;


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

    /** Whether to enable strong {@code ETag}s (usually recommended). */
    @Bindable("strong") default Boolean strong() {
      return true;
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
}
