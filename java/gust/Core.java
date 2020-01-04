
package gust;

import jsinterop.annotations.JsType;


/**
 * Provides core values, utility methods, etc, which can be used throughout the back- and
 * front-end of a Gust-based application.
 */
@JsType
public class Core {
  /**
   * Retrieve the application version setting, which is applied via the JVM system property
   * <pre>`APP_VERSION</pre>.
   *
   * @returns Version assigned for the currently-running application.
   **/
  public static String getVersion() {
    return System.getProperty("APP_VERSION", "0.0.1-alpha1");
  }
}

