package gust.backend.model;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


/**
 * Operational options that can be applied to individual calls into the {@link ModelAdapter} framework. See individual
 * options interfaces for more information.
*/
@SuppressWarnings("UnstableApiUsage")
public interface OperationOptions {
  /** @return Value to apply to the operation timeout. If left unspecified, the global default is used. */
  default @Nonnull Optional<Long> timeoutValue() {
    return Optional.empty();
  }

  /** @return Unit to apply to the operation timeout. If left unspecified, the global default is used. */
  default @Nonnull Optional<TimeUnit> timeoutUnit() {
    return Optional.empty();
  }

  /** @return Executor service that should be used for calls that reference this option set. */
  default @Nonnull Optional<ListeningScheduledExecutorService> executorService() {
    return Optional.empty();
  }
}
