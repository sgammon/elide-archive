package javatests.gust.backend;

import org.junit.Test;

import gust.backend.TemplateProvider;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


/** Tests the core backend {@link TemplateProvider}, which is responsible for loading Soy templates. */
public final class TemplateProviderTest {
  /** Test that the template provider can be constructed without error. */
  @Test public void templateProviderInit() {
    new TemplateProvider();
  }

  /** Ensure that, by default, {@link TemplateProvider#provideSoyFileSet()} is <pre>null</pre>. */
  @Test public void soyFileSetDefaultNull() {
    final TemplateProvider provider = new TemplateProvider();
    assertNull("`TemplateProvider.provideSoyFileSet` should return `null` by default",
      provider.provideSoyFileSet());
  }

  /** Ensure that, by default, {@link TemplateProvider#provideCompiledTemplates()} is not <pre>null</pre>. */
  @Test public void soyCompiledTemplatesNotNull() {
    final TemplateProvider provider = new TemplateProvider();
    assertNotNull("`TemplateProvider.provideCompiledTemplates` should never return `null`",
      provider.provideCompiledTemplates());
  }

  /** Ensure that, by default, {@link TemplateProvider#idRenamingMap()} is <pre>null</pre>. */
  @Test public void soyRenamingIDDefaultNull() {
    final TemplateProvider provider = new TemplateProvider();
    assertNull("`TemplateProvider.idRenamingMap()` should return `null` by default",
      provider.idRenamingMap());
  }

  /** Ensure that, by default, {@link TemplateProvider#cssRenamingMap()} is <pre>null</pre>. */
  @Test public void soyRenamingClassDefaultNull() {
    final TemplateProvider provider = new TemplateProvider();
    assertNull("`TemplateProvider.cssRenamingMap()` should return `null` by default",
      provider.cssRenamingMap());
  }
}
