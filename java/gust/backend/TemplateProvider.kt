package gust.backend

import com.google.template.soy.SoyFileSet
import com.google.template.soy.jbcsrc.api.SoySauce
import com.google.template.soy.jbcsrc.api.SoySauceBuilder
import io.micronaut.views.soy.SoyFileSetProvider
import io.micronaut.views.soy.SoyNamingMapProvider
import javax.inject.Singleton


/** Default Soy template provider. */
@Singleton
class TemplateProvider: SoyFileSetProvider, SoyNamingMapProvider {
  /** Initialized compiled template set. */
  private val compiledTemplates: SoySauce = SoySauceBuilder()
    .build()

  /**
   * Provide a Soy file set for the embedded templates.
   *
   * @return Prepared Soy file set.
   */
  override fun provideSoyFileSet(): SoyFileSet? = null

  /**
   * Provide the compiled Soy file set for embedded templates.
   *
   * @return Pre-compiled Soy templates.
   */
  override fun provideCompiledTemplates(): SoySauce? = compiledTemplates
}
