package gust.backend;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;

/**
 * Responsible for any testable functionality that occurs when bootstrapping a Java-based application, including
 * force-resolving critical configuration files, setting up logging, and so on.
 */
public final class ApplicationBoot {
  private ApplicationBoot() { /* Disallow instantiation. */ }

  /** Root configuration for a Micronaut app. */
  public static final String rootConfig = "/application.yml";

  /** Default configuration provided by Gust. */
  public static final String defaultConfig = "/gust" + rootConfig;

  /** Root logging configuration for a Micronaut app. */
  private static final String loggingConfig = "/logback.xml";

  /** Default configuration provided by Gust. */
  private static final String defaultLoggingConfig = "/gust" + loggingConfig;

  /**
   * Report an error that occurred during server startup, which prevented the server from starting. Errors encountered
   * and reported in this manner are fatal.
   *
   * @param err Fatal error that occurred and prevented server startup.
   */
  public static void reportStartupError(@Nonnull Throwable err) {
    System.err.println("Uncaught exception: " + err.getMessage());
    err.printStackTrace(System.err);
    throw new IllegalStateException(
      String.format("Catastrophic startup error thrown: %s.", err.getMessage()));
  }

  /**
   * Attempt to load a given global config file, failing if we can't find it in the expected spot, or the backup spot,
   * optionally provided as the second param.
   *
   * @param role Role description for this file.
   * @param name Configuration file name.
   * @param alt Alternate configuration file name.
   * @throws RuntimeException Wrapping an {@link IOException}, If the configuration can't be loaded.
   */
  public static void loadConfig(@Nonnull String role, @Nonnull String name, @Nullable String alt) {
    try (final InputStream configStream = ApplicationBoot.class.getResourceAsStream(name)) {
      if (configStream == null) {
        if (alt != null) {
          try (final InputStream defaultConfigStream = ApplicationBoot.class.getResourceAsStream(alt)) {
            if (defaultConfigStream == null)
              throw new IOException("Loaded config was `null` (for configuration '" + role + "').");
            return;  // we loaded it at the alternate location: good to go
          }
        }
        throw new IOException("Config stream was `null` when loaded (for configuration '" + role + "').");
      }

    } catch (IOException ioe) {
      System.out.println("Failed to load server configuration '" + role + "'. Failing.");
      throw new RuntimeException(ioe);
    }
  }

  /**
   * Load main application configs, including the `app` config (usually `application.yml`), containing configuration for
   * Micronaut, and `logback.xml` which contains configuration for logging. If either config file cannot be loaded, then
   * an error is thrown which prevents server startup.
   */
  public static void load() {
    // validate config & start the server
    loadConfig("app", rootConfig, defaultConfig);
    loadConfig("logging", loggingConfig, defaultLoggingConfig);
  }
}
