package gust.backend.model;

import com.google.protobuf.Descriptors;
import tools.elide.core.FieldType;

import javax.annotation.Nonnull;


/** Specifies that a model was missing a required annotated-field for a given operation. */
@SuppressWarnings("WeakerAccess")
public final class MissingAnnotatedField extends PersistenceException {
  /** Specifies the descriptor that violated the required field constraint. */
  private final @Nonnull Descriptors.Descriptor violatingSchema;

  /** Field type that was required but not found. */
  private final @Nonnull FieldType requiredField;

  /**
   * Create an exception describing a missing, but required, annotated schema field.
   *
   * @param descriptor Descriptor for the message type in question.
   * @param type Field type that was required but missing.
   */
  MissingAnnotatedField(@Nonnull Descriptors.Descriptor descriptor, @Nonnull FieldType type) {
    super(String.format(
      "Model type '%s' failed to be processed, because it is missing the annotated field '%s', " +
      "which was required for the requested operation.",
      descriptor.getName(),
      type.name()));

    this.violatingSchema = descriptor;
    this.requiredField = type;
  }

  // -- Getters -- //

  /** @return Descriptor for the schema type which violated the required-field constraint. */
  public @Nonnull Descriptors.Descriptor getViolatingSchema() {
    return violatingSchema;
  }

  /** @return The type of field that must be added to pass the failing constraint. */
  public @Nonnull FieldType getRequiredField() {
    return requiredField;
  }
}
