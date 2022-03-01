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
package elide.runtime.jvm;

import org.slf4j.LoggerFactory;


/** Sugar bridge to SLF4J. */
public final class Logging implements elide.runtime.Logging {
  public final static String rootLoggerName = "_root_";
  private final static Logging _default_factory_ = new Logging();
  private Logging() { /* Disallow direct instantiation. */ }

  /**
   * Return a factory instance of the {@link elide.runtime.Logging} interface, which is capable of producing named
   * loggers on-the-fly.
   *
   * @return Logging factory.
   */
  public static Logging factory() {
    return _default_factory_;
  }

  /**
   * Retrieve a logger for a particular Java class, named after the fully-qualified path to the class.
   *
   * @param cls Java class to get a logger for.
   * @return Logger for the specified class.
   */
  public static elide.runtime.jvm.Logger logger(Class<?> cls) {
    return logger(cls.getName());
  }

  /**
   * Retrieve a logger for a particular name or path.
   *
   * @param name Name of the desired logger.
   * @return Logger for the specified class.
   */
  public static elide.runtime.jvm.Logger logger(String name) {
    return elide.runtime.jvm.Logger.wrap(
        LoggerFactory.getLogger(name == null ? rootLoggerName : name)
    );
  }

  /**
   * Retrieve a reference to the root logger, with no specific name or path.
   *
   * @return Logger for the specified class.
   */
  public static elide.runtime.jvm.Logger logger() {
    return elide.runtime.jvm.Logger.wrap(
        LoggerFactory.getLogger(rootLoggerName)
    );
  }

  @Override
  public elide.runtime.jvm.Logger getLogger(String name) {
    return logger(name);
  }

  @Override
  public elide.runtime.jvm.Logger getLogger() {
    return logger();
  }

  @Override
  public elide.runtime.jvm.Logger getLoggerForClass(Class<?> cls) {
    return logger(cls);
  }
}
