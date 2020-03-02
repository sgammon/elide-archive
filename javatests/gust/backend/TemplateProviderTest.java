package gust.backend;

import com.google.template.soy.jbcsrc.api.SoySauce;
import com.google.template.soy.jbcsrc.api.SoySauceBuilder;
import com.google.template.soy.shared.SoyCssRenamingMap;
import com.google.template.soy.shared.SoyIdRenamingMap;

import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.*;


/** Tests the core backend {@link TemplateProvider}, which is responsible for loading Soy templates. */
@SuppressWarnings("deprecation")
public final class TemplateProviderTest {
  /** Test that the template provider can be constructed without error. */
  @Test void templateProviderInit() {
    new TemplateProvider();
  }

  /** Ensure that, by default, {@link TemplateProvider#provideSoyFileSet()} is {@code null}. */
  @Test void soyFileSetDefaultNull() {
    final TemplateProvider provider = new TemplateProvider();
    assertNull(provider.provideSoyFileSet(),
      "`TemplateProvider.provideSoyFileSet` should return `null` by default");
  }

  /** Ensure that, by default, {@link TemplateProvider#provideCompiledTemplates()} is not {@code null}. */
  @Test void soyCompiledTemplatesNotNull() {
    final TemplateProvider provider = new TemplateProvider();
    assertNotNull(provider.provideCompiledTemplates(),
      "`TemplateProvider.provideCompiledTemplates` should never return `null`");
  }

  /** Ensure that, by default, {@link TemplateProvider#idRenamingMap()} is {@code null}. */
  @Test void soyRenamingIDDefaultNull() {
    final TemplateProvider provider = new TemplateProvider();
    assertNull(provider.idRenamingMap(),
      "`TemplateProvider.idRenamingMap()` should return `null` by default");
  }

  /** Ensure that, by default, {@link TemplateProvider#cssRenamingMap()} is {@code null}. */
  @Test void soyRenamingClassDefaultNull() {
    final TemplateProvider provider = new TemplateProvider();
    assertNull(provider.cssRenamingMap(),
      "`TemplateProvider.cssRenamingMap()` should return `null` by default");
  }

  /** Ensure that, if templates are installed on the provider, it returns those instead of the default set. */
  @Test void soyProvideInstalledTemplates() {
    final TemplateProvider provider = new TemplateProvider();
    final SoySauce sauce = new SoySauceBuilder().build();
    provider.installTemplates(sauce);

    final SoySauce other = provider.provideCompiledTemplates();
    assertSame(sauce, other,
      "`TemplateProvider.provideCompiledTemplates()` should provide installed templates over defaults");
  }

  /** Ensure that, if renaming maps are installed on the provider, they are properly returned. */
  @Test void soyProvideRenamingMaps() {
    final TemplateProvider provider = new TemplateProvider();
    final SoyCssRenamingMap cssMap = new SoyCssRenamingMap() {
      @Nullable @Override
      public String get(@Nonnull String className) { return null; }
    };

    provider.installRenamingMaps(cssMap, null);

    final SoyCssRenamingMap other = provider.cssRenamingMap();
    assertSame(cssMap, other,
      "`TemplateProvider.cssRenamingMap()` should provide installed renaming map, if any");

    final SoyIdRenamingMap idOther = provider.idRenamingMap();
    assertNull(idOther,
      "`TemplateProvider.idRenamingMap()` should not mash state with `cssRenamingMap()`");

    final SoyIdRenamingMap idMap = new SoyIdRenamingMap() {
      @Nullable @Override
      public String get(@Nonnull String idName) { return null; }
    };

    provider.installRenamingMaps(cssMap, idMap);

    final SoyIdRenamingMap idOther2 = provider.idRenamingMap();
    assertSame(idMap, idOther2,
      "`TemplateProvider.idRenamingMap()` should provide installed renaming map, if any");

    provider.installRenamingMaps(cssMap, null);
    final SoyCssRenamingMap cssMap2 = provider.cssRenamingMap();
    final SoyIdRenamingMap idOther3 = provider.idRenamingMap();
    assertSame(idMap, idOther3,
      "`TemplateProvider.idRenamingMap()` should not allow `null` overwrite");

    assertSame(cssMap2, other,
      "`TemplateProvider.cssRenamingMap()` should be re-installable");
  }
}
