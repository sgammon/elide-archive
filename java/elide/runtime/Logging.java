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

import elide.annotations.Generated;


/**
 * Describes a generic, cross-platform interface for application-wide logging tools, which provide functions for
 * acquiring and managing {@link Logger} instances.
 *
 * Loggers are conventionally static objects provisioned on a per-class basis, but can be used any way the developer
 * sees fit. {@link Logging} roughly corresponds to SLF4J's {@link org.slf4j.LoggerFactory} on the JVM, and has no
 * analog on the frontend.
 */
@Generated
public interface Logging {
    /**
     * Acquire a logger at the specified name; in this case, the name is used to instantiate the logger without
     * modification.
     *
     * If `null` is passed for the logger name, the root logger is acquired and returned (however, this is generally
     * discouraged).
     *
     * @param name Name of the logger to acquire.
     * @return Logger at the specified name.
     */
    Logger getLogger(String name);

    /**
     * Acquire a default logger instance, with no provided explicit name; this is discouraged because logs cannot be
     * traced back to their origin point (at least via the name of their logger interface).
     *
     * Calling this method is equivalent to calling `logger(null)` (i.e. a logger with no explicit name).
     *
     * @return Root logger.
     */
    default Logger getLogger() {
        return getLogger(null);
    }

    /**
     * Acquire a logger at the specified class name, by using the class' full path is used as the "name" of the logger;
     * this is functionally equivalent to calling `logger(SomeClass.class.name)`.
     *
     * @param cls Class for which we should acquire a logger.
     * @return Logger acquired for the provided class.
     */
    default Logger getLoggerForClass(Class<?> cls) {
        return getLogger(cls.getName());
    }
}
