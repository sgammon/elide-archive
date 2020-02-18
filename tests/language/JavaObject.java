package tests.language;

import jsinterop.annotations.JsType;


/**
 * Sample Java object via J2CL.
 */
@JsType
public class JavaObject {
  /**
   * Returns a simple string.
   *
   * @return String value.
   */
  public static String hello() {
    return "Java";
  }
}
