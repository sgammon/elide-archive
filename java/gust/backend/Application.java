package gust.backend;

import io.micronaut.runtime.Micronaut;


/**
 * Main application class, which bootstraps a backend Gust app via Micronaut, including any configured controllers,
 * services, or assets. This is where execution starts when running on a JVM.
 */
public class Application {
  /**
   * Main entrypoint into a Gust backend application, powered by Micronaut. This function will pre-load any static stuff
   * that needs to be bootstrapped, and then it initializes the app via Micronaut.
   *
   * @param args Arguments passed on the command line.
   */
  public static void main(String[] args) {
    System.out.println("Starting Micronaut application on port 8080...");
    Micronaut.run(Application.class);
  }
}
