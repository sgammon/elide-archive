package gust.backend.model;


import com.google.protobuf.FieldMask;

import javax.annotation.Nonnull;
import java.util.Optional;


/**
 * Specifies options which may be applied, generically, to model instance fetch operations implemented through the
 * {@link PersistenceDriver} interface.
 */
public interface FetchOptions extends CacheOptions, OperationOptions {
  /** Default set of fetch options. */
  FetchOptions DEFAULTS = new FetchOptions() {};

  /** Enumerates ways the {@code FieldMask} may be applied. */
  enum MaskMode {
    /** Only include fields mentioned in the field mask. */
    INCLUDE,

    /** Omit fields mentioned in the field mask. */
    EXCLUDE,

    /** Treat the field mask as a projection, for query purposes only. */
    PROJECTION
  }

  /** @return Field mask to apply when fetching properties. Fields not mentioned in the mask will be omitted. */
  default @Nonnull Optional<FieldMask> fieldMask() {
    return Optional.empty();
  }

  /** @return Mode to operate in when applying the affixed field mask, if any. */
  default @Nonnull MaskMode fieldMaskMode() {
    return MaskMode.INCLUDE;
  }
}
