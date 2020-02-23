package gust.backend;

import com.google.template.soy.SoyFileSet;
import com.google.template.soy.jbcsrc.api.SoySauce;
import com.google.template.soy.jbcsrc.api.SoySauceBuilder;
import com.google.template.soy.shared.SoyCssRenamingMap;
import com.google.template.soy.shared.SoyIdRenamingMap;
import io.micronaut.views.soy.SoyFileSetProvider;
import io.micronaut.views.soy.SoyNamingMapProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;


/** Default Soy template provider. */
@Singleton
public final class TemplateProvider implements SoyFileSetProvider, SoyNamingMapProvider {
  /** Default set of templates, detected from the classpath. */
  private static final @Nonnull SoySauce defaultCompiledTemplates;

  /** Templates explicitly provided by the developer. */
  private @Nullable SoySauce overrideTemplates = null;

  /** CSS class renaming map to apply during render. */
  private @Nullable SoyIdRenamingMap idRenamingMap = null;

  /** CSS ID renaming map to apply during render. */
  private @Nullable SoyCssRenamingMap cssRenamingMap = null;

  static {
    defaultCompiledTemplates = new SoySauceBuilder().build();
  }

  // -- Public API: Installation -- //

  /**
   * Install a set of compiled templates manually into the local template provider context. These templates will be used
   * instead of any detected templates on the classpath (via the {@link #defaultCompiledTemplates}).
   *
   * @param compiled Compiled templates to install.
   * @return Template provider, for method chaining.
   */
  public TemplateProvider installTemplates(@Nonnull SoySauce compiled) {
    this.overrideTemplates = compiled;
    return this;
  }

  /**
   * Install a Soy-compatible style renaming map for CSS classes, and optionally one for CSS IDs as well. These maps are
   * installed locally and provided to Soy during template render operations. Any templates rendered subsequent to this
   * call should have the rewritten styles applied in the DOM, and with any served assets associated with the page.
   *
   * If an existing ID rewriting map is mounted, a call to unseat it with `null` will be ignored.
   *
   * @param cssMap CSS renaming map to apply during render.
   * @param idMap Optional ID renaming map to apply during render.
   * @return Template provider, for method chaining.
   */
  public TemplateProvider installRenamingMaps(@Nonnull SoyCssRenamingMap cssMap, @Nullable SoyIdRenamingMap idMap) {
    this.cssRenamingMap = cssMap;
    if (this.idRenamingMap == null || idMap != null)
      this.idRenamingMap = idMap;
    return this;
  }

  // -- Public API: Consumption -- //

  /**
   * Provide a Soy file set for the embedded templates.
   *
   * @return Prepared Soy file set.
   * @deprecated Soy file sets are slow due to runtime template interpretation. Please use compiled templates via the
   *     {@link SoySauce} class instead. See the see-also listings for this method for more information.
   * @see #provideCompiledTemplates() to acquire compiled template instances.
   */
  @Deprecated @Nullable @Override public SoyFileSet provideSoyFileSet() {
    return null;
  }

  /**
   * Provide the compiled Soy file set for embedded templates.
   *
   * @return Pre-compiled Soy templates.
   */
  @Nonnull @Override public SoySauce provideCompiledTemplates() {
    if (overrideTemplates != null)
      return overrideTemplates;
    return defaultCompiledTemplates;
  }

  /**
   * By default, return `null` for the Soy CSS renaming map.
   *
   * @return `null`, by default.
   */
  @Nullable @Override public SoyCssRenamingMap cssRenamingMap() {
    return cssRenamingMap;
  }

  /**
   * By default, return `null` for the Soy ID renaming map.
   *
   * @return `null`, by default.
   */
  @Nullable @Override public SoyIdRenamingMap idRenamingMap() {
    return idRenamingMap;
  }
}
