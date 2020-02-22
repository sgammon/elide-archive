package gust.backend;

import com.google.template.soy.SoyFileSet;
import com.google.template.soy.jbcsrc.api.SoySauce;
import com.google.template.soy.jbcsrc.api.SoySauceBuilder;
import com.google.template.soy.shared.SoyCssRenamingMap;
import com.google.template.soy.shared.SoyIdRenamingMap;
import io.micronaut.views.soy.SoyFileSetProvider;
import io.micronaut.views.soy.SoyNamingMapProvider;

import javax.annotation.Nullable;
import javax.inject.Singleton;


/** Default Soy template provider. */
@Singleton
public class TemplateProvider implements SoyFileSetProvider, SoyNamingMapProvider {
  private static final SoySauce compiledTemplates;

  static {
    compiledTemplates = new SoySauceBuilder().build();
  }

  /**
   * Provide a Soy file set for the embedded templates.
   *
   * @return Prepared Soy file set.
   */
  @Nullable @Override public SoyFileSet provideSoyFileSet() {
    return null;
  }

  /**
   * Provide the compiled Soy file set for embedded templates.
   *
   * @return Pre-compiled Soy templates.
   */
  @Nullable @Override public SoySauce provideCompiledTemplates() {
    return compiledTemplates;
  }

  /**
   * By default, return `null` for the Soy CSS renaming map.
   *
   * @return `null`, by default.
   */
  @Nullable @Override public SoyCssRenamingMap cssRenamingMap() {
    return null;
  }

  /**
   * By default, return `null` for the Soy ID renaming map.
   *
   * @return `null`, by default.
   */
  @Nullable @Override public SoyIdRenamingMap idRenamingMap() {
    return null;
  }
}
