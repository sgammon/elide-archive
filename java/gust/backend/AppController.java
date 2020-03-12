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

import com.google.common.html.types.TrustedResourceUrlProto;
import com.google.template.soy.data.SanitizedContent;
import com.google.template.soy.data.UnsafeSanitizedContentOrdainer;
import io.micronaut.http.MediaType;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;


/**
 * Describes a framework application controller, which is pre-loaded with convenient access to tooling, and any injected
 * application logic. Alternatively, if the invoking developer wishes to use their own base class, they can acquire most
 * or all of the enclosed functionality via injection.
 *
 * <p>Various <pre>protected</pre> properties are made available which allow easy access to stuff:</p>
 * <ul>
 *   <li><b><pre>context</pre>:</b> This property exposes a builder for the current flow's page context. This can be
 *       manipulated to provide/inject properties, and then subsequently used in Soy.</li>
 * </ul>
 *
 * <p>To make use of this controller, simply inherit from it in your own <pre>@Controller</pre>-annotated class. When a
 * request method is invoked, the logic provided by this object will have been initialized and will be ready to use.</p>
 */
@SuppressWarnings("unused")
public abstract class AppController extends BaseController {
  /** Pre-ordained HTML object which ensures the character encoding is set to UTF-8. */
  protected final static @Nonnull MediaType HTML;

  static {
    // initialize HTML page type
    HTML = new MediaType(MediaType.TEXT_HTML_TYPE.getName(), MediaType.TEXT_HTML_TYPE.getExtension(),
      Collections.singletonMap("charset", StandardCharsets.UTF_8.displayName()));
  }

  /**
   * Private constructor, which accepts injected manager objects. Some or all of these are passed up to
   * {@link BaseController}.
   *
   * @param pageContextManager Page context manager.
   */
  @Inject public AppController(@Nonnull PageContextManager pageContextManager) {
    super(pageContextManager);
  }

  // -- API: Trusted Resources -- //

  /**
   * Generate a trusted resource URL for the provided Java URL.
   *
   * @param url Pre-ordained trusted resource URL.
   * @return Trusted resource URL specification proto.
   */
  public TrustedResourceUrlProto trustedResource(@Nonnull URL url) {
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
  public TrustedResourceUrlProto trustedResource(@Nonnull URI uri) {
    return UnsafeSanitizedContentOrdainer.ordainAsSafe(
      uri.toString(),
      SanitizedContent.ContentKind.TRUSTED_RESOURCE_URI)
      .toTrustedResourceUrlProto();
  }
}
