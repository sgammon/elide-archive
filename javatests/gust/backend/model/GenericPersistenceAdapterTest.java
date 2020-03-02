package gust.backend.model;

import org.junit.jupiter.api.TestFactory;

import javax.annotation.Nonnull;


/** Abstract test factory, which is responsible for testing/enforcing {@link ModelAdapter} surfaces. */
public abstract class GenericPersistenceAdapterTest<Adapter extends ModelAdapter>
  extends GenericPersistenceDriverTest<Adapter> {
  /**
   * @return Replace the tested driver with the subject adapter.
   */
  @Override
  protected @Nonnull Adapter driver() {
    return adapter();
  }

  /**
   * Adapter hook-point for the generic {@link ModelAdapter} suite of unit compliance tests. Checks that the adapter
   * exposes required components, and can perform all common driver tasks, both as a {@link ModelAdapter} and a
   * {@link PersistenceDriver}.
   *
   * <p>Automatically spawns tests via {@link TestFactory} (using JUnit 5). These tests may be customized on a per-
   * driver basis by overriding individual test methods, such as {@link #testDriverCodec()}. At runtime, during testing,
   * these cases are dynamically generated and run against each driver.</p>
   *
   * @return Persistence adapter to execute tests against.
   */
  protected abstract @Nonnull Adapter adapter();
}
