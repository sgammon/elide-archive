package gust.backend;


import javax.annotation.Nonnull;


/**
 * Supplies shared logic to all framework-provided controller base classes. Responsible for managing such things as the
 * {@link PageContextManager}, and any other request-scoped state.
 *
 * <p>Implementors of this class are provided with convenient access to initialized app logic and clients (such as gRPC
 * clients or database clients). However, controller authors need not select this route for convenience sake if they
 * have a better base class in mind: all the functionality provided here can easily be obtained via dependency
 * injection.</p>
 */
@SuppressWarnings("WeakerAccess")
public abstract class BaseController {
  /** Holds request-bound page context as it is built. */
  protected final @Nonnull PageContextManager context;

  /**
   * Protected (implementor) constructor. Demands each dependency directly (generally, implementors would make use of DI
   * and pass the provided objects in).
   *
   * @param contextManager Page context manager.
   */
  protected BaseController(@Nonnull PageContextManager contextManager) {
    this.context = contextManager;
  }
}
