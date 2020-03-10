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

import io.micronaut.views.soy.SoyContextMediator;
import tools.elide.page.Context;

import javax.annotation.Nonnull;


/**
 * Interface by which protobuf-driven Soy render context can be managed and orchestrated by a custom {@link PageContext}
 * object. Provides the ability to specify variables for <pre>@param</pre> and <pre>@inject</pre> Soy declarations, and
 * the ability to override objects like the {@link io.micronaut.views.soy.SoyNamingMapProvider}.
 *
 * @author Sam Gammon (sam@momentum.io)
 * @see PageContext Default implementation of this interface
 */
public interface PageRender extends SoyContextMediator {
  /** Name at which proto-context is injected. */
  String PAGE_CONTEXT_IJ_NAME = "page";

  /**
   * Retrieve serializable server-side-rendered page context, which should be assigned to the render flow bound to this
   * context mediator.
   *
   * @return Server-side rendered page context.
   */
  @Nonnull Context getPageContext();
}
