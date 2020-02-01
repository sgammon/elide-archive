package gust.backend;

import io.micronaut.runtime.Micronaut;

import java.io.IOException;
import java.io.InputStream;


/**
 * Main application class, which bootstraps a backend Gust app via Micronaut, including any configured controllers,
 * services, or assets. This is where execution starts when running on a JVM.
 */
public class Application {
  /** Root configuration for a Micronaut app. */
  private static final String rootConfig = "/application.yml";

  /** Default configuration provided by Gust. */
  private static final String defaultConfig = "/gust/application.yml";

  /**
   * Attempt to load the global Micronaut config, failing if we can't find it in the expected spots.
   *
   * @throws RuntimeException Wrapping an {@link IOException}, If the configuration can't be loaded.
   */
  private static void loadConfig() {
    try (final InputStream configStream = Application.class.getResourceAsStream(rootConfig)) {
      if (configStream == null) {
        try (final InputStream defaultConfigStream = Application.class.getResourceAsStream(defaultConfig)) {
          if (defaultConfigStream == null)
            throw new IOException("Loaded config was `null`.");
        }
        throw new IOException("Config stream was `null` when loaded.");
      } else {
        System.out.println("Config loaded.");
      }

    } catch (IOException ioe) {
      System.out.println("Failed to load server configuration. Failing.");
      throw new RuntimeException(ioe);
    }
  }

  /**
   * Main entrypoint into a Gust backend application, powered by Micronaut. This function will pre-load any static stuff
   * that needs to be bootstrapped, and then it initializes the app via Micronaut.
   *
   * @param args Arguments passed on the command line.
   */
  public static void main(String[] args) {
    // validate config & start the server
    loadConfig();
    System.out.println("Starting Micronaut application on port 8080...");
    Micronaut.run(Application.class);
  }
}
