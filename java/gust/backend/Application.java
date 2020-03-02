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
  /**
   * Main entrypoint into a Gust backend application, powered by Micronaut. This function will pre-load any static stuff
   * that needs to be bootstrapped, and then it initializes the app via Micronaut.
   *
   * @param args Arguments passed on the command line.
   */
  public static void main(String[] args) {
    try {
      ApplicationBoot.load();
      Micronaut.run(Application.class);
    } catch (Throwable thr) {
      ApplicationBoot.reportStartupError(thr);
    }
  }

  private Application() { /* Disallow instantiation. */ }
}
