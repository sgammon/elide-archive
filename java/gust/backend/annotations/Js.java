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
import java.lang.annotation.*;


/**
 * Links a script module to a given controller endpoint, such that it will automatically be included and loaded in the
 * head of the page with the specified settings.
 */
@Documented
@Introduction
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Js {
  /**
   * Module name for the script module. This is generally the Closure module path (for example, `app.entrypoint`).
   *
   * @return Module name for this script module.
   */
  @Nonnull String value();

  /**
   * Whether to defer loading this script until the body DOM has initialized.
   *
   * @return Defaults to {@code true}.
   */
  boolean defer() default true;

  /**
   * Whether to completely unlink this script's loading flow from the DOM (i.e. async mode).
   *
   * @return Defaults to {@code false}.
   */
  boolean async() default false;

  /**
   * Whether to treat this script entry as a module.
   *
   * @return Defaults to {@code false}.
   */
  boolean module() default false;

  /**
   * Whether to treat this script entry as a module fallback.
   *
   * @return Defaults to {@code false}.
   */
  boolean noModule() default false;
}
