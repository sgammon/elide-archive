package tests.dom;

import gust.Core;
import elemental2.dom.Element;
import jsinterop.annotations.JsType;


/**
 * Represents a DOM operation from Java.
 */
@JsType
public class DomOperation {
  /**
   * Mutates the DOM using Java and Elemental2. Called from JS.
   *
   * @param element HTML element to inject into.
   * @param message String message to set as `textContent`.
   * @return Element, after it has been mutated.
   */
  public static Element mutate(Element element, String message) {
    element.textContent = "Hello from " + message + "!";
    return element;
  }

  /**
   * Mutate the DOM using Java and Elemental2, injecting the current Gust version. Called from JS.
   *
   * @param element HTML element to inject into.
   * @return Element, after it has been mutated.
   */
  public static Element mutateVersion(Element element) {
    element.textContent = Core.getGustVersion();
    return element;
  }
}
