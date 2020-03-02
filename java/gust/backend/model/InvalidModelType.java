package gust.backend.model;

import com.google.common.base.Joiner;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;
import tools.elide.core.DatapointType;

import javax.annotation.Nonnull;
import java.util.Set;


/**
 * Specifies an error, wherein a user has requested a data adapter or some other database object, for a model which is
 * not usable with data storage systems (via annotations).
 */
public final class InvalidModelType extends PersistenceException {
  /** Message that violated some type expectation. */
  private final Descriptor violatingSchema;

  /** Set of allowed types that the message didn't match. */
  private final Set<DatapointType> datapointTypes;

  /**
   * Create a persistence exception from scratch.
   *
   * @param type Descriptor type.
   * @param expectedTypes Available types, any one of which it can conform to.
   */
  InvalidModelType(@Nonnull Descriptor type, @Nonnull Set<DatapointType> expectedTypes) {
    super(String.format("Invalid model type: '%s' is not one of the allowed types '%s'.",
      type.getName(),
      Joiner.on(", ").join(expectedTypes)));
    this.violatingSchema = type;
    this.datapointTypes = expectedTypes;
  }

  /**
   * Factory to create a new `InvalidModelType` exception directly from a model descriptor.
   *
   * @param type Type of model to create this exception for.
   * @param expectedTypes Available types, any one of which it can conform to.
   * @return Created `InvalidModelType` exception.
   */
  static @Nonnull InvalidModelType from(@Nonnull Descriptor type, @Nonnull Set<DatapointType> expectedTypes) {
    return new InvalidModelType(type, expectedTypes);
  }

  /**
   * Factory to create a new `InvalidModelType` exception from a model instance.
   *
   * @param type Type of model to create this exception for.
   * @param expectedTypes Available types, any one of which it can conform to.
   * @return Created `InvalidModelType` exception.
   */
  static @Nonnull InvalidModelType from(@Nonnull Message type, @Nonnull Set<DatapointType> expectedTypes) {
    return from(type.getDescriptorForType(), expectedTypes);
  }

  // -- Getters -- //

  /** @return Message instance that violated this type constraint. */
  public Descriptor getViolatingSchema() {
    return violatingSchema;
  }

  /** @return Allowed types which the violating message did not match. */
  public Set<DatapointType> getDatapointTypes() {
    return datapointTypes;
  }
}
