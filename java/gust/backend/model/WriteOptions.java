package gust.backend.model;

import javax.annotation.Nonnull;


/** Describes options involved with operations to persist model entities. */
public interface WriteOptions extends OperationOptions {
  /** Default set of write operation options. */
  WriteOptions DEFAULTS = new WriteOptions() {};

  /** Enumerates write attitudes with regard to existing record collisions. */
  enum WriteDisposition {
    /** We don't care. Just write it. */
    BLIND,

    /** The record must exist for the write to proceed (an <i>update</i> operation). */
    MUST_EXIST,

    /** The record must <b>not</b> exist for the write to proceed (a <i>create</i> operation). */
    MUST_NOT_EXIST
  }

  /** @return Specifies the write mode for an operation. Overridden by some methods (for instance, {@code create}). */
  default @Nonnull WriteDisposition writeMode() {
    return WriteDisposition.BLIND;
  }
}
