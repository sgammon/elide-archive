package javatests.gust.backend;

import com.google.template.soy.jbcsrc.api.SoySauce;
import com.google.template.soy.jbcsrc.api.SoySauceBuilder;
import com.google.template.soy.shared.SoyCssRenamingMap;
import com.google.template.soy.shared.SoyIdRenamingMap;
import org.junit.Test;

import gust.backend.TemplateProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
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

  /** Ensure that, if templates are installed on the provider, it returns those instead of the default set. */
  @Test public void soyProvideInstalledTemplates() {
    final TemplateProvider provider = new TemplateProvider();
    final SoySauce sauce = new SoySauceBuilder().build();
    provider.installTemplates(sauce);

    final SoySauce other = provider.provideCompiledTemplates();
    assertSame("`TemplateProvider.provideCompiledTemplates()` should provide installed templates over defaults",
      sauce,
      other);
  }

  /** Ensure that, if renaming maps are installed on the provider, they are properly returned. */
  @Test public void soyProvideRenamingMaps() {
    final TemplateProvider provider = new TemplateProvider();
    final SoyCssRenamingMap cssMap = new SoyCssRenamingMap() {
      @Nullable @Override
      public String get(@Nonnull String className) { return null; }
    };

    provider.installRenamingMaps(cssMap, null);

    final SoyCssRenamingMap other = provider.cssRenamingMap();
    assertSame("`TemplateProvider.cssRenamingMap()` should provide installed renaming map, if any",
      cssMap,
      other);

    final SoyIdRenamingMap idOther = provider.idRenamingMap();
    assertNull("`TemplateProvider.idRenamingMap()` should not mash state with `cssRenamingMap()`",
      idOther);

    final SoyIdRenamingMap idMap = new SoyIdRenamingMap() {
      @Nullable @Override
      public String get(@Nonnull String idName) { return null; }
    };

    provider.installRenamingMaps(cssMap, idMap);

    final SoyIdRenamingMap idOther2 = provider.idRenamingMap();
    assertSame("`TemplateProvider.idRenamingMap()` should provide installed renaming map, if any",
      idMap,
      idOther2);

    provider.installRenamingMaps(cssMap, null);
    final SoyCssRenamingMap cssMap2 = provider.cssRenamingMap();
    final SoyIdRenamingMap idOther3 = provider.idRenamingMap();
    assertSame("`TemplateProvider.idRenamingMap()` should not allow `null` overwrite",
      idMap,
      idOther3);

    assertSame("`TemplateProvider.cssRenamingMap()` should be re-installable",
      cssMap2,
      other);
  }
}
