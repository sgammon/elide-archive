package gust.backend;

import io.micronaut.runtime.Micronaut;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;


/**
 * Main application class, which bootstraps a backend Gust app via Micronaut, including any configured controllers,
 * services, or assets. This is where execution starts when running on a JVM. Micronaut uses build-time annotation
 * processing, and a number of other techniques, to pre-initialize and wire up the app before it ever gets to the
 * server to be executed at runtime.
 */
@SuppressWarnings("WeakerAccess")
public final class Application {
  private Application() { /* Disallow instantiation. */ }

  /** Root configuration for a Micronaut app. */
  public static final String rootConfig = "/application.yml";

  /** Default configuration provided by Gust. */
  public static final String defaultConfig = "/gust" + rootConfig;

  /** Root logging configuration for a Micronaut app. */
  public static final String loggingConfig = "/logback.xml";

  /** Default configuration provided by Gust. */
  public static final String defaultLoggingConfig = "/gust" + loggingConfig;

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
    try (final InputStream configStream = Application.class.getResourceAsStream(name)) {
      if (configStream == null) {
        if (alt != null) {
          try (final InputStream defaultConfigStream = Application.class.getResourceAsStream(alt)) {
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
   *
   * @param exitOnFail Whether to exit the program if a failure occurs.
   */
  public static void load(boolean exitOnFail) {
    try {
      // validate config & start the server
      loadConfig("app", rootConfig, defaultConfig);
      loadConfig("logging", loggingConfig, defaultLoggingConfig);
    } catch (Throwable ex) {
      reportStartupError(ex, exitOnFail);
      if (!exitOnFail) throw ex;
    }
  }

  /**
   * Report an error that occurred during server startup, which prevented the server from starting. Errors encountered
   * and reported in this manner are fatal.
   *
   * @param err Fatal error that occurred and prevented server startup.
   * @param exitOnFail Whether to exit the program if a failure occurs.
   */
  public static void reportStartupError(@Nonnull Throwable err, boolean exitOnFail) {
    System.err.println("Uncaught exception: " + err.getMessage());
    err.printStackTrace(System.err);
    if (exitOnFail) System.exit(1);
    else throw new RuntimeException(err);
  }

  /**
   * Main entrypoint into a Gust backend application, powered by Micronaut. This function will pre-load any static stuff
   * that needs to be bootstrapped, and then it initializes the app via Micronaut.
   *
   * @param args Arguments passed on the command line.
   */
  public static void main(String[] args) {
    load(true);
    Micronaut.run(Application.class);
  }
}
