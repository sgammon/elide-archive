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

import tools.elide.core.CollectionMode;
import com.google.protobuf.Message;
import com.google.protobuf.Descriptors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Optional;


/**
 * Describes a generic, collapsed message (i.e. serialized for storage). When using the universal model layer (based on
 * Protocol Buffer messages), objects can be seamlessly serialized or deserialized to/from key value storage based on
 * this layer / set of objects.
 *
 * <p>In particular, collapsed messages are useful with the Firestore adapter - and other data storage adapters which
 * adopt parent-child style hierarchy. This object is not needed for in-memory storage.</p>
 */
public final class CollapsedMessage {
  /** Describes a generic collapsed data store operation. */
  public interface Operation {
    /** @return Path for this write. */
    @Nonnull String getPath();

    /** @return Parent operation for this write, if applicable. */
    @Nonnull Optional<Operation> getParent();

    /**
     * Execute the underlying operation against the provided write proxy. If there is an additional runtime path
     * prefix, to apply, it is supplied here. Calling this method is sufficient to see change in underlying storage.
     *
     * @param prefix Additional path prefix to apply before executing the operation.
     * @param proxy Write proxy to send our write invocations to.
     * @param scope Scope of the write, which dictates parent context.
     */
    void execute(@Nullable String prefix, @Nonnull WriteProxy<Object> proxy, @Nonnull Optional<Parent> scope);
  }

  /**
   * Wrap a set of pre-built operations in a collapsed message record.
   *
   * @param operations Set of operations to execute against underlying storage.
   * @return Pre-filled collapsed message.
   */
  public static @Nonnull CollapsedMessage of(@Nonnull List<Operation> operations) {
    return new CollapsedMessage(operations);
  }

  /** Base operation implementation class. */
  private abstract static class BaseOperation implements Operation {
    /** Path to prefix all writes with. */
    protected final @Nonnull String path;

    /**
     * Initialize a new base key-value storage operation.
     *
     * @param path Path prefix to apply to the implementing write.
     */
    BaseOperation(@Nonnull String path) {
      this.path = path;
    }
  }

  /** Parent placeholder for non-root writes to key value stores. */
  final static class Parent extends BaseOperation {
    /** Message backing a write for a parent (optional). */
    protected final @Nonnull Optional<Message> message;

    /**
     * Construct a write parent placeholder.
     *
     * @param path Path to prefix all writes with.
     * @param message Optional message attached to this parent, if held.
     */
    Parent(@Nonnull String path, @Nonnull Optional<Message> message) {
      super(path);
      this.message = message;
    }

    /** @return Path prefix for this parent placeholder. */
    @Override public String getPath() {
      return this.path;
    }

    /** @return `empty` - parent write placeholders never have parents themselves. */
    @Nonnull @Override public Optional<Operation> getParent() {
      return Optional.empty();
    }

    /** @inheritDoc */
    @Override public void execute(@Nullable String prefix,
                                  @Nonnull WriteProxy<Object> proxy,
                                  @Nonnull Optional<Parent> scope) {
      // no-op (we skip this write symbol, because it's just a placeholder)
    }
  }

  /** Describes a generic write operation with a key value store. */
  final static class Write extends BaseOperation {
    /** Specifies the disposition (strategy) for the write. */
    private final @Nonnull ModelSerializer.WriteDisposition disposition;

    /** Operational parent to this write, as applicable. */
    private final @Nullable Operation parent;

    /** Collection sub-write mode. */
    private final @Nonnull CollectionMode collectionMode;

    /** Field which this write is satisfying, if applicable. */
    private final @Nullable Descriptors.FieldDescriptor field;

    /** Serialized object data to write during this operation. */
    protected final @Nonnull SerializedModel data;

    /**
     * Construct a write operation from scratch.
     *
     * @param path Path prefix to apply to this write operation.
     * @param disposition Disposition (strategy) to apply to this write operation.
     * @param collectionMode Collection mode to apply for this write operation.
     * @param parentOperation Parent operation for this write, if applicable.
     * @param field Field this write is satisfying, if applicable.
     * @param data Data payload to apply with this write operation.
     */
    Write(@Nonnull String path,
          @Nonnull ModelSerializer.WriteDisposition disposition,
          @Nonnull CollectionMode collectionMode,
          @Nonnull Optional<Operation> parentOperation,
          @Nonnull Optional<Descriptors.FieldDescriptor> field,
          @Nonnull SerializedModel data) {
      super(path);
      this.disposition = disposition;
      this.collectionMode = collectionMode;
      this.parent = parentOperation.orElse(null);
      this.field = field.orElse(null);
      this.data = data;
    }

    /** @return Disposition for this write. */
    public @Nonnull ModelSerializer.WriteDisposition getDisposition() {
      return disposition;
    }

    /** @return Collection sub-write mode. */
    public @Nonnull CollectionMode getCollectionMode() {
      return collectionMode;
    }

    /** @return Field this write is satisfying, if applicable. */
    public @Nonnull Optional<Descriptors.FieldDescriptor> getField() {
      return field != null ? Optional.of(field) : Optional.empty();
    }

    public @Nonnull SerializedModel getData() {
      return data;
    }

    /** @return Path prefix for this parent placeholder. */
    @Override public String getPath() {
      return this.path;
    }

    /** @return Parent operation for this parent write. */
    @Override public Optional<Operation> getParent() {
      return this.parent == null ? Optional.empty() : Optional.of(this.parent);
    }

    /** @inheritDoc */
    @Override public void execute(@Nullable String prefix,
                                  @Nonnull WriteProxy<Object> proxy,
                                  @Nonnull Optional<Parent> scope) {
      switch (this.disposition) {
        case BLIND:
          proxy.put(proxy.ref(this.path, prefix), this.data);
          return;
        case CREATE:
          proxy.create(proxy.ref(this.path, prefix), this.data);
          return;
        case UPDATE:
          proxy.update(proxy.ref(this.path, prefix), this.data);
      }
    }
  }

  /** Operations held by this collapsed message. */
  private final List<Operation> operations;

  /**
   * Create a collapsed message from scratch.
   *
   * @param operations Set of operations to wrap.
   */
  private CollapsedMessage(List<Operation> operations) {
    this.operations = operations;
  }

  /**
   * Execute the collapsed message against the provided {@link WriteProxy}, which creates it in underlying storage (once
   * any wrapping transaction finishes).
   *
   * @param prefix Path prefix to apply to all sub-writes.
   * @param proxy Write proxy to employ.
   */
  public void persist(@Nullable String prefix, @Nonnull WriteProxy<?> proxy) {
    operations.forEach(operation -> {
      //noinspection unchecked
      operation.execute(prefix, (WriteProxy<Object>)proxy, Optional.empty());
    });
  }
}
