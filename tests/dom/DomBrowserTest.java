package tests.dom;

import com.google.testing.web.WebTest;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.openqa.selenium.WebDriver;


/**
 * Performs the basic DOM test in Gecko and Chromium.
 */
@RunWith(JUnit4.class)
public class DomBrowserTest {
  private WebDriver driver;

  @Before public void createDriver() {
    driver = new WebTest().newWebDriverSession();
  }

  @After public void quitDriver() {
    try {
      driver.quit();
    } finally {
      driver = null;
    }
  }

  @Test
  public void newWebDriverSession() {
    WebTest wt = new WebTest();
    WebDriver driver = wt.newWebDriverSession();
    driver.get(wt.HTTPAddress().resolve("/healthz").toString());
    driver.quit();
  }
}
