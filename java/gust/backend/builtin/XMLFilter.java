package gust.backend.builtin;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.views.ViewsFilterOrderProvider;
import io.micronaut.views.model.ViewModelProcessor;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;

import javax.annotation.Nullable;
import java.util.Collection;


@Filter({"/_/site/**"})
public class XMLFilter implements HttpServerFilter {
  public XMLFilter(@Nullable ViewsFilterOrderProvider orderProvider,
                   Collection<ViewModelProcessor> modelProcessors) {
    /* no-op */
  }

  public int getOrder() {
    return 9999;
  }

  public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
    return Flowable.fromPublisher(chain.proceed(request))
        .doOnNext(response -> {
          if (request.getUri().getPath().endsWith(".xml")) {
            response.contentType(MediaType.APPLICATION_XML_TYPE);
          }
        });
  }
}
