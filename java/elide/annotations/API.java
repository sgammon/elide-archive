/*
 * Copyright © 2022, The Elide Framework Authors. All rights reserved.
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
package elide.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


/**
 * Marks an application-level class as an API interface, which defines the abstract surface of a single unit of business
 * logic; combined with {@link Logic}, classes annotated with `API` constitute a set of interface and implementation
 * pairs.
 *
 * API should only be affixed to interfaces or abstract classes. API interface parameters are preserved and other AOT-
 * style configurations are possible based on this annotation.
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE})
public @interface API {
    /* This space left intentionally blank. */
}
