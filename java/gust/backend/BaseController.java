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

import javax.annotation.Nonnull;


/**
 * Supplies shared logic to all framework-provided controller base classes. Responsible for managing such things as the
 * {@link PageContextManager}, and any other request-scoped state.
 *
 * <p>Implementors of this class are provided with convenient access to initialized app logic and clients (such as gRPC
 * clients or database clients). However, controller authors need not select this route for convenience sake if they
 * have a better base class in mind: all the functionality provided here can easily be obtained via dependency
 * injection.</p>
 */
public abstract class BaseController {
  /** Holds request-bound page context as it is built. */
  protected final @Nonnull PageContextManager context;

  /**
   * Initialize a base Gust controller from scratch.
   *
   * @param context Page context manager, injected.
   */
  public BaseController(@Nonnull PageContextManager context) {
    this.context = context;
  }
}
