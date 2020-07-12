package gust.backend.builtin;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.views.ViewsFilterOrderProvider;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;


@Filter({"/_/site/*.xml"})
public class XMLFilter implements HttpServerFilter {
  public XMLFilter(@Nullable ViewsFilterOrderProvider orderProvider) {
    /* no-op */
  }

  public int getOrder() {
    return 9999;
  }

  public Publisher<MutableHttpResponse<?>> doFilter(@Nonnull HttpRequest<?> request,
                                                    @Nonnull ServerFilterChain chain) {
    return Flowable.fromPublisher(chain.proceed(request))
        .doOnNext(response -> {
          response.contentType(MediaType.APPLICATION_XML_TYPE);
        });
  }
}
