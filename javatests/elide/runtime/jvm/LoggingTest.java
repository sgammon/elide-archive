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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import static org.junit.jupiter.api.Assertions.*;


/** Tests framework logging features, built atop SLF4J and Logback. */
public final class LoggingTest {
  /** Acquire a logger for a given class. */
  @Test void testAcquireLogger() {
    Logger logger = Logging.logger(LoggingTest.class);
    assertNotNull(logger, "acquired logger should never be null");
    assertEquals(LoggingTest.class.getName(), logger.getName(),
      "acquired logger should have correct name");
  }

  /** Log a message at every level supported by SLF4J. */
  @Test void testLogLevelsSLF4J() {
    Logger logger = Logging.logger(LoggingTest.class);
    assertDoesNotThrow(() -> logger.trace("traceSample"));
    assertDoesNotThrow(() -> logger.trace("traceSample {}", "with formatting"));
    assertDoesNotThrow(() -> logger.trace("traceSample {}, {}", "with formatting", "with two params"));
    assertDoesNotThrow(() -> logger.trace("traceSample {}, {}, {}", "with formatting", "with two params", "with 3"));
    assertDoesNotThrow(() -> logger.debug("debugSample"));
    assertDoesNotThrow(() -> logger.debug("debugSample {}", "with formatting"));
    assertDoesNotThrow(() -> logger.debug("debugSample {}, {}", "with formatting", "with two params"));
    assertDoesNotThrow(() -> logger.debug("debugSample {}, {}, {}", "with formatting", "with two params", "with 3"));
    assertDoesNotThrow(() -> logger.info("infoSample"));
    assertDoesNotThrow(() -> logger.info("infoSample {}", "with formatting"));
    assertDoesNotThrow(() -> logger.info("infoSample {}, {}", "with formatting", "with two params"));
    assertDoesNotThrow(() -> logger.info("infoSample {}, {}, {}", "with formatting", "with two params", "with 3"));
    assertDoesNotThrow(() -> logger.warn("warnSample"));
    assertDoesNotThrow(() -> logger.warn("warnSample {}", "with formatting"));
    assertDoesNotThrow(() -> logger.warn("warnSample {}, {}", "with formatting", "with two params"));
    assertDoesNotThrow(() -> logger.warn("warnSample {}, {}, {}", "with formatting", "with two params", "with 3"));
    assertDoesNotThrow(() -> logger.error("errorSample"));
    assertDoesNotThrow(() -> logger.error("errorSample {}", "with formatting"));
    assertDoesNotThrow(() -> logger.error("errorSample {}, {}", "with formatting", "with two params"));
    assertDoesNotThrow(() -> logger.error("errorSample {}, {}, {}", "with formatting", "with two params", "with 3"));
  }

  /** Test log level enablement indicators supported by SLF4J. */
  @Test void testLogLevelsEnabledSLF4J() {
    Logger logger = Logging.logger(LoggingTest.class);
    assertDoesNotThrow((ThrowingSupplier<Boolean>) logger::isTraceEnabled);
    assertDoesNotThrow((ThrowingSupplier<Boolean>) logger::isDebugEnabled);
    assertDoesNotThrow((ThrowingSupplier<Boolean>) logger::isInfoEnabled);
    assertDoesNotThrow((ThrowingSupplier<Boolean>) logger::isWarnEnabled);
    assertDoesNotThrow((ThrowingSupplier<Boolean>) logger::isErrorEnabled);
  }

  /** Test logging exceptions via methods supported by SLF4J. */
  @Test void testThrowableLoggingSLF4J() {
    // manufacture an exception to test with
    Throwable throwable;
    try {
      throw new IllegalArgumentException("sample error (not real)");
    } catch (Throwable thr) {
      throwable = thr;
    }

    Logger logger = Logging.logger(LoggingTest.class);
    assertDoesNotThrow(() -> logger.trace("sample trace log with throwable", throwable));
    assertDoesNotThrow(() -> logger.debug("sample debug log with throwable", throwable));
    assertDoesNotThrow(() -> logger.info("sample info log with throwable", throwable));
    assertDoesNotThrow(() -> logger.warn("sample warn log with throwable", throwable));
    assertDoesNotThrow(() -> logger.error("sample error log with throwable", throwable));
  }

  /** Test basic log marking methods supported by SLF4J. */
  @Test void testLogMarkingSLF4J() {
    Logger logger = Logging.logger(LoggingTest.class);
    Marker marker = MarkerFactory.getMarker("SomeRequest");
    assertDoesNotThrow(() -> logger.trace(marker, "some string with marker"));
    assertDoesNotThrow(() -> logger.debug(marker, "some string with marker"));
    assertDoesNotThrow(() -> logger.info(marker, "some string with marker"));
    assertDoesNotThrow(() -> logger.warn(marker, "some string with marker"));
    assertDoesNotThrow(() -> logger.error(marker, "some string with marker"));
    assertDoesNotThrow(() -> logger.trace(marker, "some string with marker {}", "with formatting"));
    assertDoesNotThrow(() -> logger.debug(marker, "some string with marker {}", "with formatting"));
    assertDoesNotThrow(() -> logger.info(marker, "some string with marker {}", "with formatting"));
    assertDoesNotThrow(() -> logger.warn(marker, "some string with marker {}", "with formatting"));
    assertDoesNotThrow(() -> logger.error(marker, "some string with marker {}", "with formatting"));
    assertDoesNotThrow(() -> logger.trace(marker, "some string with marker {} {}", "1 arg", "2 args"));
    assertDoesNotThrow(() -> logger.debug(marker, "some string with marker {} {}", "1 arg", "2 args"));
    assertDoesNotThrow(() -> logger.info(marker, "some string with marker {} {}", "1 arg", "2 args"));
    assertDoesNotThrow(() -> logger.warn(marker, "some string with marker {} {}", "1 arg", "2 args"));
    assertDoesNotThrow(() -> logger.error(marker, "some string with marker {} {}", "1 arg", "2 args"));
    assertDoesNotThrow(() -> logger.trace(marker, "some string with marker {} {} {}", "1 arg", "2 args", "3 args"));
    assertDoesNotThrow(() -> logger.debug(marker, "some string with marker {} {} {}", "1 arg", "2 args", "3 args"));
    assertDoesNotThrow(() -> logger.info(marker, "some string with marker {} {} {}", "1 arg", "2 args", "3 args"));
    assertDoesNotThrow(() -> logger.warn(marker, "some string with marker {} {} {}", "1 arg", "2 args", "3 args"));
    assertDoesNotThrow(() -> logger.error(marker, "some string with marker {} {} {}", "1 arg", "2 args", "3 args"));
  }

  /** Test log level enablement indicators supported by SLF4J. */
  @Test void testLogMarkingEnabledSLF4J() {
    Logger logger = Logging.logger(LoggingTest.class);
    Marker marker = MarkerFactory.getMarker("SomeRequest");
    assertDoesNotThrow(() -> logger.isTraceEnabled(marker));
    assertDoesNotThrow(() -> logger.isDebugEnabled(marker));
    assertDoesNotThrow(() -> logger.isInfoEnabled(marker));
    assertDoesNotThrow(() -> logger.isWarnEnabled(marker));
    assertDoesNotThrow(() -> logger.isErrorEnabled(marker));
  }

  /** Test log level enablement indicators supported by SLF4J. */
  @Test void testLogMarkingExceptionsSLF4J() {
    Throwable throwable;
    try {
      throw new IllegalArgumentException("sample error (not real)");
    } catch (Throwable thr) {
      throwable = thr;
    }

    Logger logger = Logging.logger(LoggingTest.class);
    Marker marker = MarkerFactory.getMarker("SomeRequest");
    assertDoesNotThrow(() -> logger.trace(marker, "sample marked trace log with throwable", throwable));
    assertDoesNotThrow(() -> logger.debug(marker, "sample marked debug log with throwable", throwable));
    assertDoesNotThrow(() -> logger.info(marker, "sample marked info log with throwable", throwable));
    assertDoesNotThrow(() -> logger.warn(marker, "sample marked warn log with throwable", throwable));
    assertDoesNotThrow(() -> logger.error(marker, "sample marked error log with throwable", throwable));
  }

  /** Test static methods of acquiring loggers. */
  @Test void testAcquireJVMLoggerStatic() {
    Logger rootLogger = Logging.logger();
    Logger namedLogger = Logging.logger("some logger");
    Logger classLogger = Logging.logger(LoggingTest.class);

    assertNotNull(rootLogger, "should be able to acquire root logger via static methods");
    assertNotNull(namedLogger, "should be able to acquire named logger via static methods");
    assertNotNull(classLogger, "should be able to acquire class logger via static methods");
  }

  /** Test instance methods of acquiring loggers. */
  @Test void testAcquireJVMLoggerInstance() {
    Logging factory = Logging.factory();
    Logger rootLogger = factory.getLogger();
    Logger namedLogger = factory.getLogger("some logger");
    Logger classLogger = factory.getLoggerForClass(LoggingTest.class);

    assertNotNull(rootLogger, "should be able to acquire root logger via instance methods");
    assertNotNull(namedLogger, "should be able to acquire named logger via instance methods");
    assertNotNull(classLogger, "should be able to acquire class logger via instance methods");
  }

  /** Test instance methods of acquiring loggers. */
  @Test void testAcquireGenericLoggerInstance() {
    elide.runtime.Logging factory = Logging.factory();
    elide.runtime.Logger rootLogger = factory.getLogger();
    elide.runtime.Logger namedLogger = factory.getLogger("some logger");
    elide.runtime.Logger classLogger = factory.getLoggerForClass(LoggingTest.class);

    assertNotNull(rootLogger, "should be able to acquire generic root logger via instance methods");
    assertNotNull(namedLogger, "should be able to acquire generic named logger via instance methods");
    assertNotNull(classLogger, "should be able to acquire generic class logger via instance methods");
  }
}
