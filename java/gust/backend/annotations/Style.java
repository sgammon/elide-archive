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
package gust.backend.annotations;

import io.micronaut.aop.Introduction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.*;


/**
 * Links a style module to a given controller endpoint, such that it will automatically be included and loaded in the
 * head of the page, applying any applicable rewrite settings (if enabled).
 */
@Documented
@Introduction
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Style {
  /** Applicable media types. */
  enum MediaType {
    /** For screen display. */
    SCREEN,

    /** For print display. */
    PRINT
  }

  /**
   * Module name for the style module. This is generally the GSS/style module path (for example, `app.styles`).
   *
   * @return Module name for this style module.
   */
  @Nonnull String value();

  /**
   * Media type to apply to the injected spreadsheet, if applicable.
   *
   * @return Defaults to {@code SCREEN}.
   */
  @Nullable MediaType media() default MediaType.SCREEN;
}
