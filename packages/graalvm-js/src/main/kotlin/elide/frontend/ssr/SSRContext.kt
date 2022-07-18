package elide.frontend.ssr

import kotlin.reflect.KClass

/** Context access utility for SSR-shared state. */
class SSRContext<State: Any> private constructor (
  private val data: SSRStateContainer<State>? = null,
  private val decodedState: State? = null,
) {
  public companion object {
    /** Key where shared state is placed in the execution input data map. */
    public const val STATE: String = "_state_"

    /** Key where combined state is placed in the execution input data map. */
    public const val CONTEXT: String = "_ctx_"

    /** @return SSR context, decoded from the provided input [ctx]. */
    fun of(ctx: dynamic = null): SSRContext<Any> {
      return if (ctx != null) {
        SSRContext(ctx)
      } else {
        SSRContext()
      }
    }

    /** @return SSR context, decoded from the provided input [ctx], with the provided [stateType] class. */
    @Suppress("UNUSED_PARAMETER")
    fun <State : Any> typed(ctx: dynamic = null): SSRContext<State> {
      return if (ctx != null) {
        SSRContext(ctx, ctx.state())
      } else {
        SSRContext()
      }
    }
  }

  /** Execute the provided [fn] within the context of this decoded SSR context. */
  fun <R> execute(fn: SSRContext<State>.() -> R): R {
    return fn.invoke(this)
  }

  /** @return State container managed by this context. */
  val state: State? get() {
    return decodedState
  }
}
