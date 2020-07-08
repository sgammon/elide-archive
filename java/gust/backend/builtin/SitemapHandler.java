package gust.backend.builtin;

import gust.backend.PageContextManager;
import gust.backend.PageRender;
import gust.backend.annotations.Page;
import gust.util.Pair;
import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.qualifiers.Qualifiers;
import tools.elide.backend.builtin.Sitemap;

import javax.annotation.Nonnull;
import javax.inject.Singleton;
import java.util.*;


/**
 * Built-in handler for generating a compliant XML sitemap from {@link gust.backend.annotations.Page}-annotated
 * controllers and methods, as applicable (depending on annotation configuration properties).
 */
@Singleton
@BuiltinHandler
public final class SitemapHandler extends InternalHandler {
  /** Template context key for sitemap render data. */
  private final static @Nonnull String sitemapKey = "sitemap";

  /** Context from which to pull annotation data. */
  private final @Nonnull BeanContext beanContext;

  SitemapHandler(@Nonnull BeanContext context) {
    this.beanContext = context;
  }

  private List<Pair<AnnotationValue<Page>, AnnotationValue<Get>>> resolvePages() {
    Collection<BeanDefinition<?>> definitions =
        beanContext.getBeanDefinitions(Qualifiers.byStereotype(Controller.class));

    // build pages list
    List<Pair<AnnotationValue<Page>, AnnotationValue<Get>>> pages = new ArrayList<>(definitions.size());
    for(BeanDefinition definition : definitions) {
      //noinspection unchecked
      Collection<ExecutableMethod> methods = definition.getExecutableMethods();
      for (var method : methods) {
        Optional<AnnotationValue<Get>> targetGet = method.findAnnotation(Get.class);
        if (targetGet.isEmpty()) continue;

        Optional<AnnotationValue<Page>> pageAnnotationWrap = method.findAnnotation(Page.class);
        if (pageAnnotationWrap.isEmpty()) continue;

        AnnotationValue<Page> pageAnnotation = pageAnnotationWrap.get();
        Optional<Boolean> enableSitemap = pageAnnotation.booleanValue("sitemap");

        if (enableSitemap.isEmpty() || enableSitemap.get()) {
          pages.add(Pair.of(pageAnnotation, targetGet.get()));
        }
      }
    }
    return pages;
  }

  private boolean isSet(@Nonnull Optional<String> value) {
    //noinspection OptionalAssignedToNull,ConstantConditions
    if (value == null || value.isEmpty())
      return false;
    String inner = value.get();
    return (
      !inner.isEmpty() &&
      !inner.isBlank() &&
      !Page.NO_VALUE.equals(inner)
    );
  }

  private @Nonnull HttpResponse<PageRender> renderSitemap(
      @Nonnull PageContextManager context, @Nonnull List<Pair<AnnotationValue<Page>, AnnotationValue<Get>>> pages) {
    var builder = Sitemap.newBuilder();
    for (var page : pages) {
      var entry = Sitemap.PageEntry.newBuilder();
      AnnotationValue<Page> pageInfo = page.getKey();
      AnnotationValue<Get> getInfo = page.getValue();

      Optional<Boolean> enableIndexing = pageInfo.booleanValue(Page.ROBOTS_ENABLE);
      if (enableIndexing.isEmpty() || enableIndexing.get()) {
        Optional<Boolean> enableSitemap = pageInfo.booleanValue(Page.SITEMAP);
        if (enableSitemap.isEmpty() || enableSitemap.get()) {
          Optional<String> location = getInfo.stringValue();
          Optional<String> canonical = pageInfo.stringValue(Page.CANONICAL);

          if (isSet(location)) {
            String canonicalUrl = canonical.orElseGet(location::get);
            Optional<String> lastModified = pageInfo.stringValue(Page.LAST_MODIFIED);
            Optional<String> priority = pageInfo.stringValue(Page.PRIORITY);
            Optional<Sitemap.ChangeFrequency> changeFrequency = pageInfo.enumValue(
                Page.CHANGE_FREQUENCY, Sitemap.ChangeFrequency.class);

            entry.setLocation(canonicalUrl);
            if (isSet(canonical)) {
              //noinspection OptionalGetWithoutIsPresent
              String lastModifiedValue = lastModified.get();
              if (lastModifiedValue.length() == 10)
                entry.setLastModified(lastModifiedValue);
            }

            changeFrequency.ifPresent(entry::setChangeFrequency);
            priority.ifPresent(entry::setPriority);
            builder.addPage(entry);
          }
        }
      }
    }

    // prep and render a response
    return HttpResponse.ok(context
        .contentType(MediaType.APPLICATION_XML)
        .put(sitemapKey, builder.build())
        .render());
  }

  /** @inheritDoc */
  @Override
  public @Nonnull HttpResponse<PageRender> respond(@Nonnull PageContextManager context,
                                                   @Nonnull HttpRequest request) {
    var pages = resolvePages();
    if (!pages.isEmpty())
      return renderSitemap(context, pages);
    return HttpResponse.notFound();
  }
}
