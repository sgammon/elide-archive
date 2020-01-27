package javatests.dom;

import elemental2.dom.Element;
import jsinterop.annotations.JsType;


/**
 * Represents a DOM operation from Java.
 */
@JsType
public class DomOperation {
  /**
   * Mutates the DOM using Java and Elemental2. Called from JS.
   */
  public static Element mutate(Element element, String message) {
    element.textContent = "Hello from " + message + "!";
    return element;
  }
}
