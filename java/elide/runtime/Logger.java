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
package elide.runtime;


/**
 * Describes a generic, cross-platform interface for a named logger; loggers can be initialized from classes, in which
 * case the class' full package path is used as the name.
 *
 * Logging is implemented on each platform according to the typical semantics of that platform. On the JVM, logging is
 * backed by SLF4J, which then typically uses a runtime implementation like Logback. In frontend circumstances, the
 * browser console is used as the logging backend.
 */
public interface Logger {
}
