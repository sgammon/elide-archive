package javatests.ssr

import com.google.template.soy.SoyFileSet
import com.google.template.soy.data.SoyValueConverter
import com.google.template.soy.jbcsrc.api.SoySauce
import com.google.template.soy.jbcsrc.api.SoySauceBuilder
import io.micronaut.views.soy.SoyFileSetProvider
import io.micronaut.views.soy.SoyNamingMapProvider
import javax.inject.Singleton


/**
 * Force-load compiled templates during static init.
 */
@Singleton
class SSRTemplateLoader: SoyFileSetProvider, SoyNamingMapProvider {
  /** Initialized compiled template set. */
  companion object {
    @JvmStatic
    private val compiledTemplates: SoySauce = SoySauceBuilder()
      .build()

    init {
      val empty = SoyValueConverter.EMPTY_DICT
      val basicFactory = com.google.template.soy.jbcsrc.gen.javatests.ssr.basic(empty, empty).Factory()
      val complexFactory = com.google.template.soy.jbcsrc.gen.javatests.ssr.complex(empty, empty).Factory()
    }
  }

  /**
   * Provide a Soy file set for the embedded templates.
   *
   * @return Prepared Soy file set.
   */
  override fun provideSoyFileSet(): SoyFileSet? = null

  /**
   * Load SSR templates by name.
   *
   * @return Regular template manager instance.
   */
  override fun provideCompiledTemplates(): SoySauce = compiledTemplates
}
