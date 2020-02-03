
package gust;

import jsinterop.annotations.JsType;


/**
 * Provides core values, utility methods, etc, which can be used throughout the back- and
 * front-end of a Gust-based application.
 */
@JsType
public class Core {
  /**
   * Retrieve the application version setting, which is applied via the JVM system property <pre>gust.version</pre>.
   * This value also shows up in frontend libraries as <pre>gust.version</pre>. The default value for this property, if
   * left unspecified by the runtime, is `alpha`.
   *
   * @return Version assigned for the currently-running application.
   **/
  public static String getGustVersion() {
    return System.getProperty("GUST_VERSION", "alpha");
  }
}
