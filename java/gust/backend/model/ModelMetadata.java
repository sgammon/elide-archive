/*
 * Copyright © 2020, The Gust Framework Authors. All rights reserved.
 *
 * The Gust/Elide framework and tools, and all associated source or object computer code, except where otherwise noted,
 * are licensed under the Zero Prosperity license, which is enclosed in this repository, in the file LICENSE.txt. Use of
 * this code in object or source form requires and implies consent and agreement to that license in principle and
 * practice. Source or object code not listing this header, or unless specified otherwise, remain the property of
 * Elide LLC and its suppliers, if any. The intellectual and technical concepts contained herein are proprietary to
 * Elide LLC and its suppliers and may be covered by U.S. and Foreign Patents, or patents in process, and are protected
 * by trade secret and copyright law. Dissemination of this information, or reproduction of this material, in any form,
 * is strictly forbidden except in adherence with assigned license requirements.
 */
package gust.backend.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableSortedSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.DescriptorProtos.MessageOptions;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.FieldMask;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import com.google.protobuf.Message;
import com.google.protobuf.util.FieldMaskUtil;
import tools.elide.core.CollectionMode;
import tools.elide.core.Datamodel;
import tools.elide.core.DatapointType;
import tools.elide.core.FieldType;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Utility helper class, which is responsible for resolving metadata (based on the core framework annotations) from
 * arbitrary model definitions.
 *
 * <p>Model "metadata," in this case, refers to annotation-based declarations on the protocol buffer definitions
 * themselves. As such, the source for most (if not all) of the data provided by this helper is the {@link Descriptor}
 * that accompanies a Java-side protobuf model.</p>
 *
 * <p><b>Note:</b> Using this class, or the model layer writ-large, requires the full runtime Protobuf library (the lite
 * runtime for Protobuf in Java does not include descriptors at all, which this class relies on).</p>
 */
@ThreadSafe
@SuppressWarnings({"WeakerAccess", "unused"})
public final class ModelMetadata {
  private ModelMetadata() { /* Disallow construction. */ }

  /** Utility class that points to a specific field, in a specific context. */
  @Immutable
  @ThreadSafe
  public final static class FieldPointer implements Serializable, Comparable<FieldPointer> {
    /** Depth of this field, based on the number of dots in the path. */
    private final @Nonnull Integer depth;

    /** Access path to the field in some context. */
    private final @Nonnull String path;

    /** Base model type. */
    private final @Nonnull Descriptor base;

    /** Field descriptor for the field in question. */
    private final @Nonnull FieldDescriptor field;

    /**
     * Setup a new field pointer - generally kept private and resolved via {@link ModelMetadata}.
     *
     * @param base Base model type where {@code path} begins.
     * @param path Dotted-path to the field in question.
     * @param field Field descriptor for the field in question.
     */
    FieldPointer(@Nonnull Descriptor base,
                 @Nonnull String path,
                 @Nonnull FieldDescriptor field) {
      this.path = path;
      this.base = base;
      this.field = field;
      this.depth = CharMatcher.is('.').countIn(path);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      FieldPointer that = (FieldPointer) o;
      return this.depth.equals(that.depth)
        && com.google.common.base.Objects.equal(path, that.path)
        && com.google.common.base.Objects.equal(base.getFullName(), that.base.getFullName());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return com.google.common.base.Objects
        .hashCode(path, base.getFullName());
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(@Nonnull FieldPointer other) {
      return this.path.compareTo(other.path);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return "FieldPointer{" +
        "base='" + base.getName() + '\'' +
        ", path=" + path +
        '}';
    }

    /** @return Path to the specified field. */
    public @Nonnull String getPath() {
      return path;
    }

    /** @return Simple proto name for the field. */
    public @Nonnull String getName() {
      return field.getName();
    }

    /** @return Simple JSON name for the field. */
    public @Nonnull String getJsonName() {
      return field.getJsonName();
    }

    /** @return Base model type where the specified path begins. */
    public @Nonnull Descriptor getBase() {
      return base;
    }

    /** @return Descriptor for the targeted field. */
    public @Nonnull FieldDescriptor getField() {
      return field;
    }
  }

  /** Utility class that holds a {@link FieldPointer} and matching field value. */
  public final static class FieldContainer<V> implements Serializable, Comparable<FieldContainer<V>> {
    /** Pointer to the field which holds this value. */
    private final @Nonnull FieldPointer field;

    /** Value for the field, if found. */
    private final @Nonnull Optional<V> value;

    /**
     * Setup a new field pointer - generally kept private and resolved via {@link ModelMetadata}.
     *
     * @param field Pointer to the field that holds this value.
     * @param value Value extracted for the specified field.
     */
    FieldContainer(@Nonnull FieldPointer field,
                   @Nonnull Optional<V> value) {
      this.field = field;
      this.value = value;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      FieldContainer<?> that = (FieldContainer<?>) o;
      return field.equals(that.field)
        && (value.isPresent() == that.value.isPresent())
        && (value.equals(that.value));
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return com.google.common.base.Objects
        .hashCode(field, value);
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(@Nonnull FieldContainer<V> other) {
      return this.field.compareTo(other.field);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return "FieldContainer{" +
        "" + field.base.getName() +
        ", path=" + field.path +
        ", hasValue=" + value.isPresent() +
        '}';
    }

    /** Pointer to the field holding the specified value. */
    public @Nonnull FieldPointer getField() {
      return field;
    }

    /** Value associated with the specified field, or {@link Optional#empty()} if the field has no initialized value. */
    public @Nonnull Optional<V> getValue() {
      return value;
    }
  }

  // -- Internals -- //

  /**
   * Match an annotation to a field. If the field is not annotated as such, the method returns `false`.
   *
   * @param field Field to check for the provided annotation.
   * @param annotation Annotation to check for.
   * @return Whether the field is annotated with the provided annotation.
   */
  static boolean matchFieldAnnotation(@Nonnull FieldDescriptor field, @Nonnull FieldType annotation) {
    if (field.getOptions().hasExtension(Datamodel.field)) {
      var extension = field.getOptions().getExtension(Datamodel.field);
      return annotation.equals(extension.getType());
    }
    return false;
  }

  /**
   * Match a collection annotation. If the field or model is not annotated as such, the method returns `false`.
   *
   * @param field Field to check for the provided annotation.
   * @param mode Collection mode to check for.
   * @return Whether the field is annotated for the provided collection mode.
   */
  @SuppressWarnings("SameParameterValue")
  static boolean matchCollectionAnnotation(@Nonnull FieldDescriptor field, @Nonnull CollectionMode mode) {
    if (field.getOptions().hasExtension(Datamodel.collection)) {
      var extension = field.getOptions().getExtension(Datamodel.collection);
      return mode.equals(extension.getMode());
    }
    return false;
  }

  /**
   * Resolve a model field within the tree of {@code descriptor}, where an instance of annotation data of type
   * {@code ext} is affixed to the field. If the (optional) provided {@code filter} function agrees, the item is
   * returned to the caller in a {@link FieldPointer}.
   *
   * <p>If the field cannot be found, no exception is raised, and {@link Optional#empty()} is returned. The search may
   * also be conducted in {@code recursive} mode, which proceeds to examine sub-messages if the requested field cannot
   * be located on the top-level {@code descriptor}.</p>
   *
   * @param descriptor Descriptor where we should begin our search for the desired property.
   * @param ext Extension the field is annotated with. Only fields annotated with this extension are eligible.
   * @param recursive Whether to search recursively, or just on the top-level instance.
   * @param filter Filter function to dispatch per-found-field. The first one to return {@code true} wins.
   * @param stack Property stack, filled out as we recursively descend.
   * @param <E> Generic type of the model extension object.
   * @return Optional, containing either a resolved {@link FieldPointer}, or empty.
   */
  @VisibleForTesting
  static @Nonnull <E> Optional<FieldPointer> resolveAnnotatedField(@Nonnull Descriptor descriptor,
                                                                   @Nonnull GeneratedExtension<FieldOptions, E> ext,
                                                                   @Nonnull Boolean recursive,
                                                                   @Nonnull Optional<Function<E, Boolean>> filter,
                                                                   @Nonnull String stack) {
    Objects.requireNonNull(descriptor, "Cannot resolve field from `null` descriptor.");
    Objects.requireNonNull(ext, "Cannot resolve field from `null` descriptor.");
    Objects.requireNonNull(recursive, "Cannot pass `null` for `recursive` flag.");
    Objects.requireNonNull(filter, "Pass empty optional, not `null`, for field filter parameter.");
    Objects.requireNonNull(stack, "Recursive property stack should not be `null`.");

    for (FieldDescriptor field : descriptor.getFields()) {
      if (field.getOptions().hasExtension(ext)) {
        var extension = field.getOptions().getExtension(ext);
        if (filter.isEmpty() || filter.get().apply(extension))
          return Optional.of(new FieldPointer(
            descriptor,
            stack.isEmpty() ? field.getName() : stack + "." + field.getName(),
            field));
      }

      // should we recurse?
      if (recursive && field.getType() == FieldDescriptor.Type.MESSAGE) {
        // if so, append the current prop to the stack and give it a shot
        //noinspection ConstantConditions
        var sub = resolveAnnotatedField(
          field.getMessageType(),
          ext,
          recursive,
          filter,
          stack.isEmpty() ? field.getName() : stack + "." + field.getName());
        if (sub.isPresent())
          return sub;
      }
    }
    return Optional.empty();
  }

  /**
   * Resolve a model field within the tree of {@code descriptor}, identified by the specified deep {@code path}. If the
   * (optional) provided {@code filter} function agrees, the item is returned to the caller in a {@link FieldPointer}.
   *
   * <p>If the field cannot be found, no exception is raised, and {@link Optional#empty()} is returned. The search may
   * also be conducted in {@code recursive} mode, which proceeds to examine sub-messages if the requested field cannot
   * be located on the top-level {@code descriptor}.</p>
   *
   * @param original Top-level descriptor where we should begin our search for the desired property.
   * @param descriptor Current-level descriptor we are scanning (for recursion).
   * @param path Deep dotted-path to the field we are being asked to resolve.
   * @param remaining Remaining segments of {@code path} to follow/compute.
   * @return Optional, containing either a resolved {@link FieldPointer}, or empty.
   * @throws IllegalArgumentException If the provided path is syntactically invalid.
   * @throws IllegalArgumentException If an attempt is made to access a property on a primitive field.
   */
  @VisibleForTesting
  static @Nonnull Optional<FieldPointer> resolveArbitraryField(@Nonnull Descriptor original,
                                                               @Nonnull Descriptor descriptor,
                                                               @Nonnull String path,
                                                               @Nonnull String remaining) {
    Objects.requireNonNull(original, "Cannot resolve field from `null` descriptor.");
    Objects.requireNonNull(descriptor, "Cannot resolve field from `null` descriptor.");
    Objects.requireNonNull(path, "Cannot resolve field from `null` path.");
    Objects.requireNonNull(remaining, "Recursive remaining stack should not be `null`.");

    if (remaining.startsWith(".") || remaining.endsWith(".") || remaining.contains(" "))
      throw new IllegalArgumentException(String.format("Invalid deep-field path '%s'.", path));
    if (!remaining.contains(".")) {
      // maybe we're lucky and don't need to recurse
      for (FieldDescriptor field : descriptor.getFields()) {
        if (remaining.equals(field.getName())) {
          return Optional.of(new FieldPointer(
            original,
            path,
            field));
        }
      }
    } else {
      // need to recurse
      String segment = remaining.substring(0, remaining.indexOf('.'));
      var messageField = descriptor.findFieldByName(segment);
      if (messageField != null && messageField.getType() == FieldDescriptor.Type.MESSAGE) {
        // found the next tier
        var subType = messageField.getMessageType();
        String newRemainder = remaining.substring(remaining.indexOf('.') + 1);
        return resolveArbitraryField(
          original,
          subType,
          path,
          newRemainder);
      } else if (messageField != null) {
        // it's not a message :(
        throw new IllegalArgumentException(
          String.format(
            "Cannot access sub-field of primitive leaf field, at '%s' on model type '%s'.",
            path,
            original.getName()));
      }
    }
    // property not found
    return Optional.empty();
  }

  /**
   * Splice an arbitrary field {@code value} at {@code path} into the provided {@code builder}. If an empty value
   * ({@link Optional#empty()}) is provided, clear any existing value residing at {@code path}. In all cases, mutate the
   * existing {@code builder} rather than returning a copy.
   *
   * @param original Top-level builder, which we hand back at the end.
   * @param builder Builder to splice the value into and return.
   * @param path Path at which the target property resides.
   * @param value Value which we should set the target property to, or clear (if passed {@link Optional#empty()}).
   * @param remaining Remaining properties to recurse down to. Internal use only.
   * @param <Builder> Builder type which we are operating on for this splice.
   * @param <Value> Value type which we are operating with for this splice.
   * @return Provided {@code builder} after being mutated with the specified property value.
   */
  @VisibleForTesting
  static <Builder extends Message.Builder, Value> Builder spliceArbitraryField(@Nonnull Message.Builder original,
                                                                               @Nonnull Message.Builder builder,
                                                                               @Nonnull String path,
                                                                               @Nonnull Optional<Value> value,
                                                                               @Nonnull String remaining) {
    Objects.requireNonNull(builder, "Cannot splice field into `null` builder.");
    Objects.requireNonNull(path, "Cannot resolve field from `null` path.");
    Objects.requireNonNull(remaining, "Recursive remaining stack should not be `null`.");
    Objects.requireNonNull(value, "Pass an empty optional, not `null`, for value.");

    var descriptor = builder.getDescriptorForType();
    if (!remaining.isEmpty() && !remaining.contains(".")) {
      // thankfully, no need to recurse
      var field = Objects.requireNonNull(descriptor.findFieldByName(remaining));
      if (value.isPresent()) {
        try {
          builder.setField(field, value.get());
        } catch (IllegalArgumentException iae) {
          throw new ClassCastException(String.format(
            "Failed to set field '%s': value type mismatch.",
            path));
        }
      } else {
        builder.clearField(field);
      }
      //noinspection unchecked
      return (Builder)original;
    } else {
      // we have a sub-message that is initialized, so we need to recurse.
      String segment = remaining.substring(0, remaining.indexOf('.'));
      String newRemainder = remaining.substring(remaining.indexOf('.') + 1);
      return spliceArbitraryField(
        original,
        builder.getFieldBuilder(Objects.requireNonNull(descriptor.findFieldByName(segment))),
        path,
        value,
        newRemainder);
    }
  }

  @VisibleForTesting
  static @Nonnull <V> FieldContainer<V> pluckFieldRecursive(@Nonnull Message original,
                                                            @Nonnull Message instance,
                                                            @Nonnull String path,
                                                            @Nonnull String remaining) {
    Objects.requireNonNull(original, "Cannot resolve field from `null` descriptor.");
    Objects.requireNonNull(instance, "Cannot resolve field from `null` instance.");
    Objects.requireNonNull(path, "Cannot resolve field from `null` path.");
    Objects.requireNonNull(remaining, "Recursive remaining stack should not be `null`.");

    var descriptor = instance.getDescriptorForType();
    if (remaining.startsWith(".") || remaining.endsWith(".") || remaining.contains(" ")) {
      throw new IllegalArgumentException("Cannot begin or end model property path with `.`");
    } else if (!remaining.isEmpty() && !remaining.contains(".")) {
      // we got lucky, no need to recurse
      var field = descriptor.findFieldByName(remaining);
      if (field != null) {
        if (field.getType() == FieldDescriptor.Type.MESSAGE) {
          Message modelInstance = (Message)instance.getField(field);
          //noinspection unchecked
          return new FieldContainer<>(
            new FieldPointer(descriptor, path, field),
            !modelInstance.getAllFields().isEmpty() ? Optional.of((V)modelInstance) : Optional.empty());
        } else {
          //noinspection unchecked
          return new FieldContainer<>(
            new FieldPointer(descriptor, path, field),
            Optional.of((V) instance.getField(field)));
        }
      }
    } else {
      // find next segment
      String segment = remaining.substring(0, remaining.indexOf('.'));
      var messageField = descriptor.findFieldByName(segment);
      if (messageField != null && messageField.getType() == FieldDescriptor.Type.MESSAGE) {
        if (!instance.hasField(messageField)) {
          // there is a sub-message that is not initialized. so the field is technically empty.
          return new FieldContainer<>(
            new FieldPointer(original.getDescriptorForType(), path, messageField),
            Optional.empty());
        } else {
          // we have a sub-message that is initialized, so we need to recurse.
          String newRemainder = remaining.substring(remaining.indexOf('.') + 1);
          return pluckFieldRecursive(
            original,
            (Message)instance.getField(messageField),
            path,
            newRemainder);
        }
      } else if (messageField != null) {
        // it's not a message :(
        throw new IllegalArgumentException(
          String.format(
            "Cannot access sub-field of primitive leaf field, at '%s' on model type '%s'.",
            path,
            original.getDescriptorForType().getName()));
      }
    }
    throw new IllegalArgumentException(
      String.format("Failed to locate field '%s' on model type '%s'.", path, descriptor.getName()));
  }

  // -- Metadata: Qualified Names -- //

  /**
   * Resolve the fully-qualified type path, or name, for the provided datamodel type descriptor. This is essentially
   * syntactic sugar.
   *
   * @param descriptor Model descriptor to resolve a fully-qualified name for.
   * @return Fully-qualified model type name.
   */
  public static @Nonnull String fullyQualifiedName(@Nonnull Descriptor descriptor) {
    return descriptor.getFullName();
  }

  /**
   * Resolve the fully-qualified type path, or name, for the provided datamodel instance. This method is essentially
   * syntactic sugar for accessing the model instance's descriptor, and then grabbing the fully-qualified name.
   *
   * @param model Model instance to resolve a fully-qualified name for.
   * @return Fully-qualified model type name.
   */
  public static @Nonnull String fullyQualifiedName(@Nonnull Message model) {
    return fullyQualifiedName(model.getDescriptorForType());
  }

  // -- Metadata: Role Annotations -- //

  /**
   * Resolve the general type for a given datamodel type descriptor. The type is either set by default, or set by an
   * explicit annotation affixed to the protocol buffer definition that backs the model.
   *
   * <p>{@link DatapointType} annotations describe the general use case for a given model definition. Is it a database
   * model? A wire model? {@link DatapointType} will tell you.</p>
   *
   * @param descriptor Model descriptor to retrieve a type for.
   * @return Type of the provided datamodel.
   */
  public static @Nonnull DatapointType role(@Nonnull Descriptor descriptor) {
    return modelAnnotation(descriptor, Datamodel.role, false).orElse(DatapointType.OBJECT);
  }

  /**
   * Resolve the general type for a given datamodel. The type is either set by default, or set by an explicit annotation
   * affixed to the protocol buffer definition that backs the model.
   *
   * <p>{@link DatapointType} annotations describe the general use case for a given model definition. Is it a database
   * model? A wire model? {@link DatapointType} will tell you.</p>
   *
   * @param model Model to retrieve a type for.
   * @return Type of the provided datamodel.
   */
  public static @Nonnull DatapointType role(@Nonnull Message model) {
    Objects.requireNonNull(model, "Cannot resolve type for `null` model.");
    return role(model.getDescriptorForType());
  }

  /**
   * Enforce that a particular datamodel type matches <b>any</b> of the provided {@link DatapointType} annotations.
   *
   * <p>{@link DatapointType} annotations describe the general use case for a given model definition. Is it a database
   * model? A wire model? {@link DatapointType} will tell you, and this model will sweetly enforce membership amongst
   * a set of types for you.</p>
   *
   * @param model Model to validate against the provided set of types.
   * @param type Type to enforce for the provided model.
   * @return Whether the provided model is a <i>member-of</i> (annotated-by) any of the provided {@code types}.
   */
  public static boolean matchRole(@Nonnull Message model, @Nonnull DatapointType type) {
    Objects.requireNonNull(type, "Cannot match `null` model type.");
    return type.equals(role(model));
  }

  /**
   * Enforce that a particular datamodel schema {@code descriptor} matches <b>any</b> of the provided
   * {@link DatapointType} annotations.
   *
   * <p>{@link DatapointType} annotations describe the general use case for a given model definition. Is it a database
   * model? A wire model? {@link DatapointType} will tell you, and this model will sweetly enforce membership amongst
   * a set of types for you.</p>
   *
   * @param descriptor Schema descriptor to validate against the provided set of types.
   * @param type Type to enforce for the provided model.
   * @return Whether the provided model is a <i>member-of</i> (annotated-by) any of the provided {@code types}.
   */
  public static boolean matchRole(@Nonnull Descriptor descriptor, @Nonnull DatapointType type) {
    Objects.requireNonNull(type, "Cannot match `null` descriptor type.");
    return type.equals(role(descriptor));
  }

  /**
   * <b>Check</b> that a particular datamodel type matches <b>any</b> of the provided {@link DatapointType} annotations.
   *
   * <p>{@link DatapointType} annotations describe the general use case for a given model definition. Is it a database
   * model? A wire model? {@link DatapointType} will tell you, and this model will sweetly enforce membership amongst
   * a set of types for you.</p>
   *
   * @param model Model to validate against the provided set of types.
   * @param types Types to validate the model against. If <b>any</b> of the provided types match, the check passes.
   * @return Whether the provided model is a <i>member-of</i> (annotated-by) any of the provided {@code types}.
   */
  public static boolean matchAnyRole(@Nonnull Message model, @Nonnull DatapointType ...types) {
    Objects.requireNonNull(types, "Cannot match `null` model types.");
    return EnumSet.copyOf(Arrays.asList(types)).contains(role(model));
  }

  /**
   * <b>Check</b> that a particular schema {@code descriptor} matches <b>any</b> of the provided {@link DatapointType}
   * annotations.
   *
   * <p>{@link DatapointType} annotations describe the general use case for a given model definition. Is it a database
   * model? A wire model? {@link DatapointType} will tell you, and this model will sweetly enforce membership amongst
   * a set of types for you.</p>
   *
   * @param descriptor Schema descriptor to validate against the provided set of types.
   * @param types Types to validate the model against. If <b>any</b> of the provided types match, the check passes.
   * @return Whether the provided model is a <i>member-of</i> (annotated-by) any of the provided {@code types}.
   */
  public static boolean matchAnyRole(@Nonnull Descriptor descriptor, @Nonnull DatapointType ...types) {
    Objects.requireNonNull(types, "Cannot match `null` model types.");
    return EnumSet.copyOf(Arrays.asList(types)).contains(role(descriptor));
  }

  /**
   * <b>Enforce</b> that a particular {@code model} instance matches the provided {@link DatapointType} annotation.
   *
   * <p>{@link DatapointType} annotations describe the general use case for a given model definition. Is it a database
   * model? A wire model? {@link DatapointType} will tell you, and this model will sweetly enforce membership amongst
   * a set of types for you.</p>
   *
   * @param model Model to validate against the provided set of types.
   * @param type Types to validate the model against. If <b>any</b> of the provided types match, the check passes.
   * @throws InvalidModelType If the specified model's type is not included in {@code types}.
   */
  public static void enforceRole(@Nonnull Message model, @Nonnull DatapointType type) throws InvalidModelType {
    if (!matchRole(model, type)) throw InvalidModelType.from(model, EnumSet.of(type));
  }

  /**
   * <b>Enforce</b> that a particular datamodel schema {@code descriptor} matches the provided {@link DatapointType}
   * annotation.
   *
   * <p>{@link DatapointType} annotations describe the general use case for a given model definition. Is it a database
   * model? A wire model? {@link DatapointType} will tell you, and this model will sweetly enforce membership amongst
   * a set of types for you.</p>
   *
   * @param descriptor Descriptor to validate against the provided set of types.
   * @param type Types to validate the model against. If <b>any</b> of the provided types match, the check passes.
   * @throws InvalidModelType If the specified model's type is not included in {@code types}.
   */
  public static void enforceRole(@Nonnull Descriptor descriptor, @Nonnull DatapointType type) throws InvalidModelType {
    if (!matchRole(descriptor, type)) throw InvalidModelType.from(descriptor, EnumSet.of(type));
  }

  /**
   * <b>Enforce</b> that a particular datamodel type matches <b>any</b> of the provided {@link DatapointType}
   * annotations.
   *
   * <p>{@link DatapointType} annotations describe the general use case for a given model definition. Is it a database
   * model? A wire model? {@link DatapointType} will tell you, and this model will sweetly enforce membership amongst
   * a set of types for you.</p>
   *
   * @param model Model to validate against the provided set of types.
   * @param types Types to validate the model against. If <b>any</b> of the provided types match, the check passes.
   * @throws InvalidModelType If the specified model's type is not included in {@code types}.
   */
  public static void enforceAnyRole(@Nonnull Message model, @Nonnull DatapointType ...types) throws InvalidModelType {
    if (!matchAnyRole(model, types)) throw InvalidModelType.from(model, EnumSet.copyOf(Arrays.asList(types)));
  }

  /**
   * <b>Enforce</b> that a particular schema {@code descriptor} matches <b>any</b> of the provided {@link DatapointType}
   * annotations.
   *
   * <p>{@link DatapointType} annotations describe the general use case for a given model definition. Is it a database
   * model? A wire model? {@link DatapointType} will tell you, and this model will sweetly enforce membership amongst
   * a set of types for you.</p>
   *
   * @param descriptor Schema descriptor to validate against the provided set of types.
   * @param types Types to validate the model against. If <b>any</b> of the provided types match, the check passes.
   * @throws InvalidModelType If the specified model's type is not included in {@code types}.
   */
  public static void enforceAnyRole(@Nonnull Descriptor descriptor,
                                    @Nonnull DatapointType ...types) throws InvalidModelType {
    if (!matchAnyRole(descriptor, types)) throw InvalidModelType.from(descriptor, EnumSet.copyOf(Arrays.asList(types)));
  }

  // -- Metadata: Field Resolution -- //

  /**
   * Resolve an arbitrary field pointer from the provided model {@code instance}, specified by the given {@code path} to
   * the property. If the property cannot be found, {@link Optional#empty()} is returned.
   *
   * <p>This method is <b>safe</b>, in that, unlike other util methods for model metadata, it will not throw if the
   * provided {@code path} is invalid.</p>
   *
   * @param instance Model instance on which to resolve the specified field.
   * @param path Dotted deep-path to the desired field.
   * @return Resolved field pointer for the requested field, or {@link Optional#empty()}.
   */
  public static @Nonnull Optional<FieldPointer> resolveField(@Nonnull Message instance, @Nonnull String path) {
    return resolveField(instance.getDescriptorForType(), path);
  }

  /**
   * Resolve an arbitrary field pointer from the provided model type {@code escriptor}, specified by the given
   * {@code path} to the property. If the property cannot be found, {@link Optional#empty()} is returned.
   *
   * <p>This method is <b>safe</b>, in that, unlike other util methods for model metadata, it will not throw if the
   * provided {@code path} is invalid.</p>
   *
   * @param descriptor Model type descriptor on which to resolve the specified field.
   * @param path Dotted deep-path to the desired field.
   * @return Resolved field pointer for the requested field, or {@link Optional#empty()}.
   */
  public static @Nonnull Optional<FieldPointer> resolveField(@Nonnull Descriptor descriptor, @Nonnull String path) {
    return resolveArbitraryField(descriptor, descriptor, path, path);
  }

  // -- Metadata: Model Annotations -- //

  /**
   * Retrieve a model-level annotation, from {@code instance}, structured by {@code ext}. If no instance of the
   * requested model annotation can be found, {@link Optional#empty()} is returned. Search recursively is supported as
   * well, which descends the search to sub-messages to search for the desired annotation.
   *
   * @param instance Message instance to scan for the specified annotation.
   * @param ext Extension to fetch from the subject model, or any sub-model (if {@code recursive} is {@code true}).
   * @param recursive Whether to search recursively for the desired extension.
   * @param <E> Generic type of extension we are looking for.
   * @return Optional, either {@link Optional#empty()}, or wrapping the found extension data instance.
   */
  public static @Nonnull <E> Optional<E> modelAnnotation(@Nonnull Message instance,
                                                         @Nonnull GeneratedExtension<MessageOptions, E> ext,
                                                         @Nonnull Boolean recursive) {
    return modelAnnotation(instance.getDescriptorForType(), ext, recursive);
  }

  /**
   * Retrieve a model-level annotation, from the provided model schema {@code descriptor}, structured by {@code ext}. If
   * no instance of the requested model annotation can be found, {@link Optional#empty()} is returned. Search
   * recursively is supported as well, which descends the search to sub-messages to search for the desired annotation.
   *
   * @param descriptor Schema descriptor for a model type.
   * @param ext Extension to fetch from the subject model, or any sub-model (if {@code recursive} is {@code true}).
   * @param recursive Whether to search recursively for the desired extension.
   * @param <E> Generic type of extension we are looking for.
   * @return Optional, either {@link Optional#empty()}, or wrapping the found extension data instance.
   */
  public static @Nonnull <E> Optional<E> modelAnnotation(@Nonnull Descriptor descriptor,
                                                         @Nonnull GeneratedExtension<MessageOptions, E> ext,
                                                         @Nonnull Boolean recursive) {
    Objects.requireNonNull(descriptor, "Cannot resolve type for `null` descriptor.");
    if (descriptor.getOptions().hasExtension(ext))
      return Optional.of(descriptor.getOptions().getExtension(ext));
    if (recursive) {
      // loop through fields. gather any sub-messages, and check procedurally if any of them match. if we find one that
      // does, we return immediately.
      for (FieldDescriptor field : descriptor.getFields()) {
        if (field.getType() == FieldDescriptor.Type.MESSAGE) {
          //noinspection ConstantConditions
          var subresult = modelAnnotation(field.getMessageType(), ext, recursive);
          if (subresult.isPresent())
            return subresult;
        }
      }
    }
    return Optional.empty();
  }

  // -- Metadata: Field Annotations -- //

  /**
   * Resolve a {@link FieldPointer} within the scope of {@code instance}, that holds values for the specified metadata
   * annotation {@code ext}. By default, this method searches recursively.
   *
   * @see #annotatedField(Descriptor, GeneratedExtension) variant if a descriptor is on-hand
   * @see #annotatedField(Descriptor, GeneratedExtension, Boolean, Optional) full-spec variant.
   * @param instance Model instance to search for the specified annotated field on.
   * @param ext Extension (annotation) which should be affixed to the field we are searching for.
   * @param <E> Extension generic type.
   * @return Optional-wrapped field pointer, or {@link Optional#empty()}.
   */
  public static @Nonnull <E> Optional<FieldPointer> annotatedField(@Nonnull Message instance,
                                                                   @Nonnull GeneratedExtension<FieldOptions, E> ext) {
    return annotatedField(instance, ext, true);
  }

  /**
   * Resolve a {@link FieldPointer} within the scope of {@code instance}, that holds values for the specified metadata
   * annotation {@code ext}.
   *
   * <p>This method variant also allows specifying a <b>recursive</b> flag, which, if specified, causes the search to
   * proceed to sub-models (recursively) until a matching field is found. If <b>recursive</b> is passed as {@code false}
   * then the search will only occur at the top-level of {@code instance}.</p>
   *
   * @see #annotatedField(Message, GeneratedExtension, Boolean, Optional) Variant that supports a filter
   * @see #annotatedField(Descriptor, GeneratedExtension, Boolean, Optional) full-spec variant.
   * @param instance Model instance to search for the specified annotated field on.
   * @param ext Extension (annotation) which should be affixed to the field we are searching for.
   * @param recursive Whether to conduct this search recursively, or just at the top-level.
   * @param <E> Extension generic type.
   * @return Optional-wrapped field pointer, or {@link Optional#empty()}.
   */
  public static @Nonnull <E> Optional<FieldPointer> annotatedField(@Nonnull Message instance,
                                                                   @Nonnull GeneratedExtension<FieldOptions, E> ext,
                                                                   @Nonnull Boolean recursive) {
    return annotatedField(instance, ext, recursive, Optional.empty());
  }

  /**
   * Resolve a {@link FieldPointer} within the scope of {@code instance}, that holds values for the specified metadata
   * annotation {@code ext}.
   *
   * <p>This method variant also allows specifying a <b>filter</b>, which will be run for each property encountered with
   * the annotation present. If the filter returns {@code true}, the field will be selected, otherwise, the search
   * continues until all properties are exhausted (depending on {@code recursive}).</p>
   *
   * @param instance Model instance to search for the specified annotated field on.
   * @param ext Extension (annotation) which should be affixed to the field we are searching for.
   * @param recursive Whether to conduct this search recursively, or just at the top-level.
   * @param <E> Extension generic type.
   * @return Optional-wrapped field pointer, or {@link Optional#empty()}.
   */
  public static @Nonnull <E> Optional<FieldPointer> annotatedField(@Nonnull Message instance,
                                                                   @Nonnull GeneratedExtension<FieldOptions, E> ext,
                                                                   @Nonnull Boolean recursive,
                                                                   @Nonnull Optional<Function<E, Boolean>> filter) {
    return annotatedField(instance.getDescriptorForType(), ext, recursive, filter);
  }

  /**
   * Resolve a {@link FieldPointer} within the scope of the provided model {@code descriptor}, that holds values for the
   * specified metadata annotation {@code ext}. By default, this search occurs recursively, examining all nested sub-
   * models on the provided instance.
   *
   * @param descriptor Model object descriptor to search for the specified annotated field on.
   * @param ext Extension (annotation) which should be affixed to the field we are searching for.
   * @param <E> Extension generic type.
   * @return Optional-wrapped field pointer, or {@link Optional#empty()}.
   */
  public static @Nonnull <E> Optional<FieldPointer> annotatedField(@Nonnull Descriptor descriptor,
                                                                   @Nonnull GeneratedExtension<FieldOptions, E> ext) {
    return annotatedField(descriptor, ext, Optional.empty());
  }

  /**
   * Resolve a {@link FieldPointer} within the scope of the provided model {@code descriptor}, that holds values for the
   * specified metadata annotation {@code ext}. By default, this search occurs recursively, examining all nested sub-
   * models on the provided instance.
   *
   * <p>This method variant also allows specifying a <b>filter</b>, which will be run for each property encountered with
   * the annotation present. If the filter returns {@code true}, the field will be selected, otherwise, the search
   * continues until all properties are exhausted (depending on {@code recursive}).</p>
   *
   * @param descriptor Model object descriptor to search for the specified annotated field on.
   * @param ext Extension (annotation) which should be affixed to the field we are searching for.
   * @param <E> Extension generic type.
   * @return Optional-wrapped field pointer, or {@link Optional#empty()}.
   */
  public static @Nonnull <E> Optional<FieldPointer> annotatedField(@Nonnull Descriptor descriptor,
                                                                   @Nonnull GeneratedExtension<FieldOptions, E> ext,
                                                                   @Nonnull Optional<Function<E, Boolean>> filter) {
    return annotatedField(descriptor, ext, true, filter);
  }

  /**
   * Resolve a {@link FieldPointer} within the scope of the provided model {@code descriptor}, that holds values for the
   * specified metadata annotation {@code ext}. Using the {@code recursive} parameter, the invoking developer may opt to
   * search for the annotated field recursively.
   *
   * <p>This method variant also allows specifying a <b>filter</b>, which will be run for each property encountered with
   * the annotation present. If the filter returns {@code true}, the field will be selected, otherwise, the search
   * continues until all properties are exhausted (depending on {@code recursive}).</p>
   *
   * @param descriptor Model object descriptor to search for the specified annotated field on.
   * @param ext Extension (annotation) which should be affixed to the field we are searching for.
   * @param <E> Extension generic type.
   * @return Optional-wrapped field pointer, or {@link Optional#empty()}.
   */
  public static @Nonnull <E> Optional<FieldPointer> annotatedField(@Nonnull Descriptor descriptor,
                                                                   @Nonnull GeneratedExtension<FieldOptions, E> ext,
                                                                   @Nonnull Boolean recursive,
                                                                   @Nonnull Optional<Function<E, Boolean>> filter) {
    return resolveAnnotatedField(descriptor, ext, recursive, filter, "");
  }

  // -- Metadata: ID Fields -- //

  /**
   * Resolve a pointer to the provided model {@code instance}'s ID field, whether or not it has a value. If there is no
   * ID-annotated field at all, {@link Optional#empty()} is returned. Alternatively, if the model is not compatible with
   * ID fields, an exception is raised (see below).
   *
   * @param instance Model instance for which an ID field is being resolved.
   * @return Optional, either {@link Optional#empty()} or containing a {@link FieldPointer} to the resolved ID field.
   * @throws InvalidModelType If the specified model does not support IDs. Only objects of type {@code OBJECT} can be
   *         used with this interface.
   */
  public static @Nonnull Optional<FieldPointer> idField(@Nonnull Message instance) throws InvalidModelType {
    return idField(instance.getDescriptorForType());
  }

  /**
   * Resolve a pointer to the provided schema type {@code descriptor}'s ID field, whether or not it has a value. If
   * there is no ID-annotated field at all, {@link Optional#empty()} is returned. Alternatively, if the model is not
   * compatible with ID fields, an exception is raised (see below).
   *
   * @param descriptor Model instance for which an ID field is being resolved.
   * @return Optional, either {@link Optional#empty()} or containing a {@link FieldPointer} to the resolved ID field.
   * @throws InvalidModelType If the specified model does not support IDs. Only objects of type {@code OBJECT} can be
   *         used with this interface.
   */
  public static @Nonnull Optional<FieldPointer> idField(@Nonnull Descriptor descriptor) throws InvalidModelType {
    enforceAnyRole(Objects.requireNonNull(descriptor), DatapointType.OBJECT, DatapointType.OBJECT_KEY);
    var topLevelId = Objects.requireNonNull(annotatedField(
      descriptor,
      Datamodel.field,
      false,
      Optional.of((field) -> field.getType() == FieldType.ID)));

    if (topLevelId.isPresent()) {
      return topLevelId;
    } else {
      // okay. no top level ID. what about keys, which must be top-level?
      var keyBase = keyField(descriptor);
      if (keyBase.isPresent()) {
        // we found a key, so scan the key for an ID, which is required on keys.
        return Objects.requireNonNull(resolveAnnotatedField(
          keyBase.get().field.getMessageType(),
          Datamodel.field,
          false,
          Optional.of((field) -> field.getType() == FieldType.ID),
          keyBase.get().getField().getName()));
      }
    }
    // there's no top-level ID, and no top-level key, or the key had no ID. we're done here.
    return Optional.empty();
  }

  // -- Metadata: Key Fields -- //

  /**
   * Resolve a pointer to the provided schema type {@code descriptor}'s {@code KEY} field, whether or not it has a value
   * assigned. If there is no key-annotated field at all, {@link Optional#empty()} is returned. Alternatively, if the
   * model is not compatible with key fields, an exception is raised (see below).
   *
   * @param instance Model instance for which a key field is being resolved.
   * @return Optional, either {@link Optional#empty()} or containing a {@link FieldPointer} to the resolved key field.
   * @throws InvalidModelType If the specified model does not support keys. Only objects of type {@code OBJECT} can be
   *         used with this interface.
   */
  public static @Nonnull Optional<FieldPointer> keyField(@Nonnull Message instance) throws InvalidModelType {
    return keyField(instance.getDescriptorForType());
  }

  /**
   * Resolve a pointer to the provided schema type {@code descriptor}'s {@code KEY} field, whether or not it has a value
   * assigned. If there is no key-annotated field at all, {@link Optional#empty()} is returned. Alternatively, if the
   * model is not compatible with key fields, an exception is raised (see below).
   *
   * @param descriptor Model type descriptor for which a key field is being resolved.
   * @return Optional, either {@link Optional#empty()} or containing a {@link FieldPointer} to the resolved key field.
   * @throws InvalidModelType If the specified model does not support keys. Only objects of type {@code OBJECT} can be
   *         used with this interface.
   */
  public static @Nonnull Optional<FieldPointer> keyField(@Nonnull Descriptor descriptor) throws InvalidModelType {
    enforceAnyRole(Objects.requireNonNull(descriptor), DatapointType.OBJECT);
    return Objects.requireNonNull(annotatedField(
      Objects.requireNonNull(descriptor),
      Datamodel.field,
      false,
      Optional.of((field) -> field.getType() == FieldType.KEY)));
  }

  // -- Metadata: Value Pluck -- //

  /**
   * Pluck a field value, addressed by a {@link FieldPointer}, from the provided {@code instance}. If the referenced
   * field is a message, a message instance will be handed back only if there is an initialized value. Leaf fields
   * return their raw value, if set. In all cases, if there is no initialized value, {@link Optional#empty()} is
   * returned.
   *
   * @param instance Model instance from which to pluck the property.
   * @param fieldPointer Pointer to the field we wish to fetch.
   * @param <V> Generic type of data returned by this operation.
   * @return Optional wrapping the resolved value, or {@link Optional#empty()} if no value could be resolved.
   * @throws IllegalStateException If the referenced property is not found, despite witnessing matching types.
   * @throws IllegalArgumentException If the specified field does not have a matching base type with {@code instance}.
   */
  public static @Nonnull <V> FieldContainer<V> pluck(@Nonnull Message instance, @Nonnull FieldPointer fieldPointer) {
    return pluck(instance, fieldPointer.path);
  }

  /**
   * Return a single field value container, plucked from the specified deep {@code path}, in dot form, using the regular
   * protobuf-definition names for each field. If a referenced field is a message, a message instance will be returned
   * only if there is an initialized value. Leaf fields return their raw value, if set. In all cases, if there is no
   * initialized value, {@link Optional#empty()} is supplied in place.
   *
   * @param instance Model instance to pluck the specified property from.
   * @param path Deep path for the property value we wish to pluck.
   * @param <V> Expected type for the property. If types do not match, a {@link ClassCastException} will be raised.
   * @return Field container, either empty, or containing the plucked value.
   * @throws IllegalArgumentException If the provided path is syntactically invalid, or the field does not exist.
   */
  public static @Nonnull <V> FieldContainer<V> pluck(@Nonnull Message instance, @Nonnull String path) {
    return pluckFieldRecursive(instance, instance, path, path);
  }

  /**
   * Return an iterable containing plucked value containers for each field mentioned in {@code mask}, that is present on
   * {@code instance} with an initialized value. If a referenced field is a message, a message instance will be included
   * only if there is an initialized value. Leaf fields return their raw value, if set. In all cases, if there is no
   * initialized value, {@link Optional#empty()} is supplied in place.
   *
   * <p>If a field cannot be found, {@link Optional#empty()} is supplied in its place, so that the output order matches
   * path iteration order on the supplied {@code mask}. This method is therefore safe with regard to path access.</p>
   *
   * @param instance Model instance to pluck the specified properties from.
   * @param mask Mask of properties to pluck from the model instance.
   * @return Stream which emits each field container, with a generic {@code Object} for each value.
   */
  public static @Nonnull SortedSet<FieldContainer<Object>> pluckAll(@Nonnull Message instance, @Nonnull FieldMask mask) {
    return pluckAll(instance, mask, true);
  }

  /**
   * Return an iterable containing plucked value containers for each field mentioned in {@code mask}, that is present on
   * {@code instance} with an initialized value. If a referenced field is a message, a message instance will be included
   * only if there is an initialized value. Leaf fields return their raw value, if set. In all cases, if there is no
   * initialized value, {@link Optional#empty()} is supplied in place.
   *
   * <p>If a field cannot be found, {@link Optional#empty()} is supplied in its place, so that the output order matches
   * path iteration order on the supplied {@code mask}. This method is therefore safe with regard to path access. If
   * {@code normalize} is activated (the default for {@link #pluckAll(Message, FieldMask)}), the field mask will be
   * sorted and de-duplicated before processing.</p>
   *
   * <p>Sort order of the return value is based on the full path of properties selected - i.e. field containers are
   * returned in lexicographic sort order matching their underlying property paths.</p>
   *
   * @param instance Model instance to pluck the specified properties from.
   * @param mask Mask of properties to pluck from the model instance.
   * @param normalize Whether to normalize the field mask before plucking fields.
   * @return Stream which emits each field container, with a generic {@code Object} for each value.
   */
  public static @Nonnull SortedSet<FieldContainer<Object>> pluckAll(@Nonnull Message instance,
                                                                    @Nonnull FieldMask mask,
                                                                    @Nonnull Boolean normalize) {
    return ImmutableSortedSet.copyOfSorted(pluckStream(instance, mask, normalize)
      .collect(Collectors.toCollection(ConcurrentSkipListSet::new)));
  }

  /**
   * Return a stream which emits plucked value containers for each field mentioned in {@code mask}, that is present on
   * {@code instance} with an initialized value. If a referenced field is a message, a message instance will be emitted
   * only if there is an initialized value. Leaf fields return their raw value, if set. In all cases, if there is no
   * initialized value, {@link Optional#empty()} is supplied in place.
   *
   * <p>If a field cannot be found, {@link Optional#empty()} is supplied in its place, so that the output order matches
   * path iteration order on the supplied {@code mask}. This method is therefore safe with regard to path access.</p>
   *
   * <p><b>Performance note:</b> the {@link Stream} returned by this method is explicitly parallel-capable, because
   * reading descriptor schema is safely concurrent.</p>
   *
   * @param instance Model instance to pluck the specified properties from.
   * @param mask Mask of properties to pluck from the model instance.
   * @return Stream which emits each field container, with a generic {@code Object} for each value.
   */
  public static @Nonnull Stream<FieldContainer<Object>> pluckStream(@Nonnull Message instance,
                                                                    @Nonnull FieldMask mask) {
    return pluckStream(instance, mask, true);
  }

  /**
   * Return a stream which emits plucked value containers for each field mentioned in {@code mask}, that is present on
   * {@code instance} with an initialized value. If a referenced field is a message, a message instance will be emitted
   * only if there is an initialized value. Leaf fields return their raw value, if set. In all cases, if there is no
   * initialized value, {@link Optional#empty()} is supplied in place.
   *
   * <p>If a field cannot be found, {@link Optional#empty()} is supplied in its place, so that the output order matches
   * path iteration order on the supplied {@code mask}. This method is therefore safe with regard to path access. If
   * {@code normalize} is activated (the default for {@link #pluckStream(Message, FieldMask)}), the field mask will be
   * sorted and de-duplicated before processing.</p>
   *
   * <p><b>Performance note:</b> the {@link Stream} returned by this method is explicitly parallel-capable, because
   * reading descriptor schema is safely concurrent.</p>
   *
   * @param instance Model instance to pluck the specified properties from.
   * @param mask Mask of properties to pluck from the model instance.
   * @param normalize Whether to normalize the field mask before plucking fields.
   * @return Stream which emits each field container, with a generic {@code Object} for each value.
   */
  public static @Nonnull Stream<FieldContainer<Object>> pluckStream(@Nonnull Message instance,
                                                                    @Nonnull FieldMask mask,
                                                                    @Nonnull Boolean normalize) {
    return (new TreeSet<>((normalize ? FieldMaskUtil.normalize(mask) : mask).getPathsList()))
      .parallelStream()
      .map((fieldPath) -> pluck(instance, fieldPath));
  }

  // -- Metadata: ID/Key Value Pluck -- //

  /**
   * Resolve the provided model instance's assigned ID, by walking the property structure for the entity, and returning
   * either the first {@code id}-annotated field's value at the top-level, or the first {@code id}-annotated field value
   * on the first {@code key}-annotated message field at the top level of the provided message.
   *
   * <p>If no ID field <i>value</i> can be resolved, {@link Optional#empty()} is returned. On the other hand, if the
   * model is not a business object or does not have an ID annotation at all, an exception is raised (see below).</p>
   *
   * @param <ID> Type for the ID value we are resolving.
   * @param instance Model instance for which an ID value is desired.
   * @return Optional wrapping the value of the model instance's ID, or an empty optional if no value could be resolved.
   * @throws InvalidModelType If the supplied model is not a business object and/or does not have an ID field at all.
   */
  public static @Nonnull <ID> Optional<ID> id(@Nonnull Message instance) {
    var descriptor = instance.getDescriptorForType();
    enforceAnyRole(descriptor, DatapointType.OBJECT, DatapointType.OBJECT_KEY);
    Optional<FieldPointer> idField = idField(descriptor);
    if (idField.isEmpty())
      throw new MissingAnnotatedField(descriptor, FieldType.ID);
    return ModelMetadata.<ID>pluck(instance, idField.get()).getValue();
  }

  /**
   * Resolve the provided model instance's assigned {@code KEY} instance, by walking the property structure for the
   * entity, and returning the first {@code key}-annotated field's value at the top-level of the provided message.
   *
   * <p>If no key field <i>value</i> can be resolved, {@link Optional#empty()} is returned. On the other hand, if the
   * model is not a business object or does not support key annotations at all, an exception is raised (see below).</p>
   *
   * @param <Key> Type for the key we are resolving.
   * @param instance Model instance for which an key value is desired.
   * @return Optional wrapping the value of the model instance's key, or an empty optional if none could be resolved.
   * @throws InvalidModelType If the supplied model is not a business object and/or does not have an key field at all.
   */
  public static @Nonnull <Key> Optional<Key> key(@Nonnull Message instance) {
    Descriptor descriptor = instance.getDescriptorForType();
    enforceRole(descriptor, DatapointType.OBJECT);
    Optional<FieldPointer> keyField = annotatedField(
      descriptor,
      Datamodel.field,
      false,
      Optional.of((field) -> field.getType() == FieldType.KEY));

    if (keyField.isEmpty())
      throw new MissingAnnotatedField(descriptor, FieldType.KEY);
    //noinspection unchecked
    return (Optional<Key>)pluck(instance, keyField.get()).getValue();
  }

  // -- Metadata: Value Splice -- //

  /**
   * Splice the provided optional value (or clear any existing value) at the field {@code path} in the provided model
   * {@code instance}. Return a re-built message after the splice.
   *
   * <p>If {@link Optional#empty()} is passed for the {@code value} to set, any existing value placed in that field
   * will be cleared. This method works identically for primitive leaf fields and message fields.</p>
   *
   * @param instance Model instance to splice the value into.
   * @param path Deep path at which to splice the value.
   * @param val Value to splice into the model, or {@link Optional#empty()} to clear any existing value.
   * @param <Model> Model type which we are working with for this splice operation.
   * @param <Value> Value type which we are splicing in, if applicable.
   * @return Re-built model, after the splice operation.
   */
  public static @Nonnull <Model extends Message, Value> Model splice(@Nonnull Message instance,
                                                                     @Nonnull String path,
                                                                     @Nonnull Optional<Value> val) {

    return splice(
      instance,
      resolveField(instance, path)
        .orElseThrow(() -> new IllegalArgumentException(String.format(
          "Failed to resolve path '%s' on model instance of type '%s' for value splice.",
          path,
          instance.getDescriptorForType().getName()))),
      val);
  }

  /**
   * Splice the provided optional value (or clear any existing value) at the specified {@code field} pointer, in the
   * provided model {@code instance}. Return a re-built message after the splice.
   *
   * <p>If {@link Optional#empty()} is passed for the {@code value} to set, any existing value placed in that field
   * will be cleared. This method works identically for primitive leaf fields and message fields.</p>
   *
   * @param instance Model instance to splice the value into.
   * @param field Resolved and validated field pointer for the field to splice.
   * @param val Value to splice into the model, or {@link Optional#empty()} to clear any existing value.
   * @param <Model> Model type which we are working with for this splice operation.
   * @param <Value> Value type which we are splicing in, if applicable.
   * @return Re-built model, after the splice operation.
   */
  public static @Nonnull <Model extends Message, Value> Model splice(@Nonnull Message instance,
                                                                     @Nonnull FieldPointer field,
                                                                     @Nonnull Optional<Value> val) {
    //noinspection unchecked
    return (Model)spliceBuilder(instance.toBuilder(), field, val).build();
  }

  /**
   * Splice the provided optional value (or clear any existing value) at the specified {@code field} pointer, in the
   * provided model {@code instance}. Return the provided builder after the splice operation. The return value may be
   * ignored if the developer so wishes (the provided {@code builder} is mutated in place).
   *
   * <p>If {@link Optional#empty()} is passed for the {@code value} to set, any existing value placed in that field
   * will be cleared. This method works identically for primitive leaf fields and message fields.</p>
   *
   * @param builder Model builder to splice the value into.
   * @param field Resolved and validated field pointer for the field to splice.
   * @param val Value to splice into the model, or {@link Optional#empty()} to clear any existing value.
   * @param <Builder> Model builder type which we are working with for this splice operation.
   * @param <Value> Value type which we are splicing in, if applicable.
   * @return Model {@code builder}, after the splice operation.
   */
  @CanIgnoreReturnValue
  public static @Nonnull <Builder extends Message.Builder, Value> Builder spliceBuilder(
    @Nonnull Message.Builder builder,
    @Nonnull FieldPointer field,
    @Nonnull Optional<Value> val) {
    return spliceArbitraryField(
      builder,
      builder,
      field.path,
      val,
      field.path);
  }

  // -- Metadata: ID/Key Splice -- //

  /**
   * Splice the provided value at {@code val}, into the ID field value for {@code instance}. If an ID-annotated property
   * cannot be located, or the model is not of a suitable/type role for use with IDs, an exception is raised (see below
   * for more info).
   *
   * <p>If an existing value exists for the model's ID, <b>it will be replaced</b>. In most object-based storage engines
   * this will end up copying the object, rather than mutating an ID. Be careful of this behavior. Passing
   * {@link Optional#empty()} will clear any existing ID on the model.</p>
   *
   * @param instance Model instance to splice the value into. Because models are immutable, this involves converting the
   *                 model to a builder, splicing in the value, and then re-building the model. As such, the model
   *                 returned will be a <i>different object instance</i>, but will otherwise be untouched.
   * @param val Value we should splice-into the ID field for the record. It is expected that the generic type of this
   *            value will line up with the ID field type, otherwise a {@link ClassCastException} will be thrown.
   * @param <Model> Type of model we are splicing an ID value into.
   * @param <Value> Type of ID value we are splicing into the model.
   * @return Model instance, rebuilt, after splicing in the provided value, at the model's ID-annotated field.
   * @throws InvalidModelType If the specified model is not suitable for use with IDs at all.
   * @throws ClassCastException If the {@code Value} generic type does not match the ID field primitive type.
   * @throws MissingAnnotatedField If the provided {@code instance} is not of the correct type, or has no ID field.
   */
  public static @Nonnull <Model extends Message, Value> Model spliceId(@Nonnull Message instance,
                                                                       @Nonnull Optional<Value> val) {
    //noinspection unchecked
    return (Model)spliceIdBuilder(instance.toBuilder(), val).build();
  }

  /**
   * Splice the provided value at {@code val}, into the ID field value for the provided model {@code builder}. If an ID-
   * annotated property cannot be located, or the model is not of a suitable/type role for use with IDs, an exception is
   * raised (see below for more info).
   *
   * <p>If an existing value exists for the model's ID, <b>it will be replaced</b>. In most object-based storage engines
   * this will end up copying the object, rather than mutating an ID. Be careful of this behavior. Passing
   * {@link Optional#empty()} will clear any existing ID on the model.</p>
   *
   * @param builder Model instance builder to splice the value into. The builder provided is <i>mutated in place</i>, so
   *                it will be an identical object instance to the one provided, but with the ID property filled in.
   * @param val Value we should splice-into the ID field for the record. It is expected that the generic type of this
   *            value will line up with the ID field type, otherwise a {@link ClassCastException} will be thrown.
   * @param <Builder> Type of model builder we are splicing an ID value into.
   * @param <Value> Type of ID value we are splicing into the model.
   * @return Model builder, after splicing in the provided value, at the model's ID-annotated field.
   * @throws InvalidModelType If the specified model is not suitable for use with IDs at all.
   * @throws ClassCastException If the {@code Value} generic type does not match the ID field primitive type.
   * @throws MissingAnnotatedField If the provided {@code builder} is not of the correct type, or has no ID field.
   */
  public static @Nonnull <Builder extends Message.Builder, Value> Builder spliceIdBuilder(
    @Nonnull Message.Builder builder,
    @Nonnull Optional<Value> val) {
    // resolve descriptor and field
    if (val.isPresent() && val.get() instanceof Message)
      throw new IllegalArgumentException("Cannot set messages as ID values.");
    var descriptor = builder.getDescriptorForType();
    enforceAnyRole(descriptor, DatapointType.OBJECT, DatapointType.OBJECT_KEY);
    var fieldPath = idField(descriptor)
      .orElseThrow(() -> new MissingAnnotatedField(descriptor, FieldType.ID))
      .getPath();

    return spliceArbitraryField(
      builder,
      builder,
      fieldPath,
      val,
      fieldPath);
  }

  /**
   * Splice the provided value at {@code val}, into the key message value for {@code instance}. If a key-annotated
   * property cannot be located, or the model is not of a suitable/type role for use with keys, an exception is raised
   * (see below for more info).
   *
   * <p>If an existing value is set for the model's key, <b>it will be replaced</b>. In most object-based storage
   * engines this will end up copying the object, rather than mutating a key. Keys are usually immutable for this
   * reason, so use this method with care. Passing {@link Optional#empty()} will clear any existing key message
   * currently affixed to the model {@code instance}.</p>
   *
   * @param instance Model instance to splice the value into. Because models are immutable, this involves converting the
   *                 model to a builder, splicing in the value, and then re-building the model. As such, the model
   *                 returned will be a <i>different object instance</i>, but will otherwise be untouched.
   * @param val Value we should splice-into the ID field for the record. It is expected that the generic type of this
   *            value will line up with the ID field type, otherwise a {@link ClassCastException} will be thrown.
   * @param <Model> Type of model we are splicing an ID value into.
   * @param <Key> Type of key message we are splicing into the model.
   * @return Model instance, rebuilt, after splicing in the provided value, at the model's ID-annotated field.
   * @throws InvalidModelType If the specified model is not suitable for use with IDs at all.
   * @throws ClassCastException If the {@code Value} generic type does not match the ID field primitive type.
   * @throws MissingAnnotatedField If the provided {@code builder} is not of the correct type, or has no ID field.
   */
  public static @Nonnull <Model extends Message, Key extends Message> Model spliceKey(@Nonnull Message instance,
                                                                                      @Nonnull Optional<Key> val) {
    //noinspection unchecked
    return (Model)spliceKeyBuilder(instance.toBuilder(), val).build();
  }

  /**
   * Splice the provided value at {@code val}, into the key message value for the supplied {@code builder}. If a
   * key-annotated property cannot be located, or the model is not of a suitable/type role for use with keys, an
   * exception is raised (see below for more info).
   *
   * <p>If an existing value is set for the model's key, <b>it will be replaced</b>. In most object-based storage
   * engines this will end up copying the object, rather than mutating a key. Keys are usually immutable for this
   * reason, so use this method with care. Passing {@link Optional#empty()} will clear any existing key message
   * currently affixed to the model {@code instance}.</p>
   *
   * @param builder Model instance builder to splice the value into. The builder provided is <i>mutated in place</i>, so
   *                it will be an identical object instance to the one provided, but with the key property filled in.
   * @param val Value we should splice-into the key field for the record. It is expected that the generic type of this
   *            value will line up with the key message type, otherwise a {@link ClassCastException} will be thrown.
   * @param <Builder> Type of model builder we are splicing a key value into.
   * @param <Key> Type of key message we are splicing into the model.
   * @return Model builder, after splicing in the provided message, at the model's key-annotated field.
   * @throws InvalidModelType If the specified model is not suitable for use with keys at all.
   * @throws ClassCastException If the {@code Value} generic type does not match the key field primitive type.
   * @throws MissingAnnotatedField If the provided {@code builder} is not of the correct type, or has no key field.
   */
  public static @Nonnull <Builder extends Message.Builder, Key extends Message> Builder spliceKeyBuilder(
    @Nonnull Message.Builder builder,
    @Nonnull Optional<Key> val) {
    // resolve descriptor and key message field
    var descriptor = builder.getDescriptorForType();
    enforceRole(descriptor, DatapointType.OBJECT);
    var fieldPath = keyField(descriptor)
      .orElseThrow(() -> new MissingAnnotatedField(descriptor, FieldType.KEY))
      .getPath();

    return spliceArbitraryField(
      builder,
      builder,
      fieldPath,
      val,
      fieldPath);
  }

  /**
   * Crawl all fields, recursively, on the descriptor provided. This data may also be accessed via a Java stream via the
   * method variants listed below. Variants of this method also allow predicate-based filtering or control of recursion.
   *
   * @see #allFields(Descriptor, Optional) to provide an optional filtering predicate.
   * @see #allFields(Descriptor, Optional, Boolean) to provide an optional predicate, and/or control recursion.
   *
   * @param descriptor Schema descriptor to crawl model definitions on.
   * @return Iterable of all fields, recursively, from the descriptor.
   */
  public static @Nonnull Iterable<FieldDescriptor> allFields(@Nonnull Descriptor descriptor) {
    return allFields(descriptor, Optional.empty(), true);
  }

  /**
   * Crawl all fields, recursively, on the descriptor provided. For each field encountered, run `predicate` to determine
   * whether to include the field, filtering the returned iterable accordingly. This data may also be accessed via a
   * Java stream via the method variants listed below.
   *
   * @see #allFields(Descriptor, Optional, Boolean) to additionally control recursion.
   *
   * @param descriptor Schema descriptor to crawl model definitions on.
   * @param predicate Filter predicate function, if applicable.
   * @return Iterable of all fields, recursively, from the descriptor, filtered by `predicate`.
   */
  public static @Nonnull Iterable<FieldDescriptor> allFields(@Nonnull Descriptor descriptor,
                                                             @Nonnull Optional<Predicate<FieldDescriptor>> predicate) {
    return allFields(descriptor, predicate, true);
  }

  /**
   * Crawl all fields, recursively, on the descriptor provided. For each field encountered, run `predicate` to determine
   * whether to include the field, filtering the returned iterable accordingly. This data may also be accessed via a
   * Java stream via the method variants listed below.
   *
   * @see #streamFields(Descriptor, Optional, Boolean) to access a stream of fields instead.
   *
   * @param descriptor Schema descriptor to crawl model definitions on.
   * @param predicate Filter predicate function, if applicable.
   * @return Iterable of all fields, optionally recursively, from the descriptor, filtered by `predicate`.
   */
  public static @Nonnull Iterable<FieldDescriptor> allFields(@Nonnull Descriptor descriptor,
                                                             @Nonnull Optional<Predicate<FieldDescriptor>> predicate,
                                                             @Nonnull Boolean recursive) {
    return streamFields(
      descriptor,
      predicate,
      recursive
    ).collect(Collectors.toUnmodifiableList());
  }

  /**
   * Crawl all fields, recursively, on the descriptor associated with the provided model instance, and return them in
   * a stream.
   *
   * <p>This method crawls recursively by default, but this behavior can be customized via the alternate method variants
   * listed below. Other variants also allow applying a predicate to filter the returned fields.</p>
   *
   * @see #streamFields(Descriptor, Optional) for the opportunity to provide a filter predicate.
   * @see #streamFields(Descriptor, Optional, Boolean) for the opportunity to control recursive crawling, and provide a
   *      filter predicate.
   *
   * @param descriptor Schema descriptor to crawl model definitions on.
   * @return Stream of field descriptors, recursively, which match the `predicate`, if provided.
   */
  public static @Nonnull <M extends Message> Stream<FieldDescriptor> streamFields(@Nonnull Descriptor descriptor) {
    return streamFields(descriptor, Optional.empty());
  }

  /**
   * Crawl all fields, recursively, on the descriptor associated with the provided model instance. For each field
   * encountered, run `predicate` to determine whether to include the field, filtering the returned stream of fields
   * accordingly.
   *
   * <p>This method crawls recursively by default, but this behavior can be customized via the alternate method variants
   * listed below.</p>
   *
   * @see #streamFields(Descriptor, Optional, Boolean) for the opportunity to control recursive crawling.
   *
   * @param descriptor Schema descriptor to crawl model definitions on.
   * @param predicate Filter predicate function, if applicable.
   * @return Stream of field descriptors, recursively, which match the `predicate`, if provided.
   */
  public static @Nonnull Stream<FieldDescriptor> streamFields(@Nonnull Descriptor descriptor,
                                                              @Nonnull Optional<Predicate<FieldDescriptor>> predicate) {
    return streamFields(descriptor, predicate, true);
  }

  /**
   * Crawl all fields, recursively, on the descriptor associated with the provided model instance. For each field
   * encountered, run `predicate` to determine whether to include the field, filtering the returned stream of fields
   * accordingly. In this case, `predicate` is required.
   *
   * <p>This method crawls recursively by default, but this behavior can be customized via the alternate method variants
   * listed below.</p>
   *
   * @see #streamFields(Descriptor, Optional, Boolean) for the opportunity to control recursive crawling.
   *
   * @param descriptor Schema descriptor to crawl model definitions on.
   * @param predicate Filter predicate function, if applicable.
   * @return Stream of field descriptors, recursively, which match the `predicate`, if provided.
   */
  public static @Nonnull Stream<FieldDescriptor> streamFields(@Nonnull Descriptor descriptor,
                                                              @Nonnull Predicate<FieldDescriptor> predicate) {
    return streamFields(descriptor, Optional.of(predicate), true);
  }

  /**
   * Crawl all fields, recursively, on the provided descriptor for a model instance. For each field encountered, run
   * `predicate` to determine whether to include the field, filtering the returned stream of fields accordingly.
   *
   * <p>This method variant allows the user to restrict recursive crawling. If recursion is active, a depth-first search
   * is performed, with the `predicate` function invoked for every field encountered during the crawl. If no predicate
   * is provided, the entire set of recursive effective fields is returned from the provided descriptor.</p>
   *
   * @see #streamFields(Descriptor) for the cleanest invocation of this method.
   *
   * @param descriptor Schema descriptor to crawl model definitions on.
   * @param predicate Filter predicate function, if applicable.
   * @param recursive Whether to descend to sub-models recursively.
   * @return Stream of field descriptors, recursively, which match the `predicate`, if provided.
   */
  public static @Nonnull Stream<FieldDescriptor> streamFields(@Nonnull Descriptor descriptor,
                                                              @Nonnull Optional<Predicate<FieldDescriptor>> predicate,
                                                              @Nonnull Boolean recursive) {
    Objects.requireNonNull(descriptor, "cannot crawl fields on null descriptor");
    Objects.requireNonNull(predicate, "cannot pass `null` for optional predicate");
    Objects.requireNonNull(recursive, "cannot pass `null` for recursive switch");

    return descriptor.getFields().parallelStream().flatMap((field) -> {
      var branch = Stream.of(field);
      if (recursive && field.getType() == FieldDescriptor.Type.MESSAGE) {
        return Stream.concat(branch, field.getMessageType().getFields().parallelStream());
      }
      return branch;
    }).filter((field) ->
      predicate.map(fieldDescriptorPredicate -> fieldDescriptorPredicate.test(field)).orElse(true)
    );
  }
}
