// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: elide/model/model.proto

package tools.elide.model;

/**
 * <pre>
 * Defines options structures that relate to Google Cloud Spanner.
 * </pre>
 *
 * Protobuf type {@code model.SpannerOptions}
 */
public final class SpannerOptions extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:model.SpannerOptions)
    SpannerOptionsOrBuilder {
private static final long serialVersionUID = 0L;
  // Use SpannerOptions.newBuilder() to construct.
  private SpannerOptions(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private SpannerOptions() {
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new SpannerOptions();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private SpannerOptions(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          default: {
            if (!parseUnknownField(
                input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (com.google.protobuf.UninitializedMessageException e) {
      throw e.asInvalidProtocolBufferException().setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return tools.elide.model.Datamodel.internal_static_model_SpannerOptions_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return tools.elide.model.Datamodel.internal_static_model_SpannerOptions_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            tools.elide.model.SpannerOptions.class, tools.elide.model.SpannerOptions.Builder.class);
  }

  /**
   * <pre>
   * Specifies types applicable to Spanner property translation.
   * </pre>
   *
   * Protobuf enum {@code model.SpannerOptions.SpannerType}
   */
  public enum SpannerType
      implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <pre>
     * Unspecified type for Spanner fields.
     * </pre>
     *
     * <code>UNSPECIFIED_TYPE = 0 [deprecated = true];</code>
     */
    @java.lang.Deprecated
    UNSPECIFIED_TYPE(0),
    /**
     * <pre>
     * `STRING` type.
     * </pre>
     *
     * <code>STRING = 1;</code>
     */
    STRING(1),
    /**
     * <pre>
     * `NUMERIC` type.
     * </pre>
     *
     * <code>NUMERIC = 2;</code>
     */
    NUMERIC(2),
    /**
     * <pre>
     * `FLOAT64` type.
     * </pre>
     *
     * <code>FLOAT64 = 3;</code>
     */
    FLOAT64(3),
    /**
     * <pre>
     * `INT64` type.
     * </pre>
     *
     * <code>INT64 = 4;</code>
     */
    INT64(4),
    /**
     * <pre>
     * `BYTES` type.
     * </pre>
     *
     * <code>BYTES = 5;</code>
     */
    BYTES(5),
    /**
     * <pre>
     * `BOOL` type.
     * </pre>
     *
     * <code>BOOL = 6;</code>
     */
    BOOL(6),
    /**
     * <pre>
     * `DATE` type.
     * </pre>
     *
     * <code>DATE = 7;</code>
     */
    DATE(7),
    /**
     * <pre>
     * `TIMESTAMP` type.
     * </pre>
     *
     * <code>TIMESTAMP = 8;</code>
     */
    TIMESTAMP(8),
    /**
     * <pre>
     * `JSON` (special type).
     * </pre>
     *
     * <code>JSON = 9;</code>
     */
    JSON(9),
    UNRECOGNIZED(-1),
    ;

    /**
     * <pre>
     * Unspecified type for Spanner fields.
     * </pre>
     *
     * <code>UNSPECIFIED_TYPE = 0 [deprecated = true];</code>
     */
    @java.lang.Deprecated public static final int UNSPECIFIED_TYPE_VALUE = 0;
    /**
     * <pre>
     * `STRING` type.
     * </pre>
     *
     * <code>STRING = 1;</code>
     */
    public static final int STRING_VALUE = 1;
    /**
     * <pre>
     * `NUMERIC` type.
     * </pre>
     *
     * <code>NUMERIC = 2;</code>
     */
    public static final int NUMERIC_VALUE = 2;
    /**
     * <pre>
     * `FLOAT64` type.
     * </pre>
     *
     * <code>FLOAT64 = 3;</code>
     */
    public static final int FLOAT64_VALUE = 3;
    /**
     * <pre>
     * `INT64` type.
     * </pre>
     *
     * <code>INT64 = 4;</code>
     */
    public static final int INT64_VALUE = 4;
    /**
     * <pre>
     * `BYTES` type.
     * </pre>
     *
     * <code>BYTES = 5;</code>
     */
    public static final int BYTES_VALUE = 5;
    /**
     * <pre>
     * `BOOL` type.
     * </pre>
     *
     * <code>BOOL = 6;</code>
     */
    public static final int BOOL_VALUE = 6;
    /**
     * <pre>
     * `DATE` type.
     * </pre>
     *
     * <code>DATE = 7;</code>
     */
    public static final int DATE_VALUE = 7;
    /**
     * <pre>
     * `TIMESTAMP` type.
     * </pre>
     *
     * <code>TIMESTAMP = 8;</code>
     */
    public static final int TIMESTAMP_VALUE = 8;
    /**
     * <pre>
     * `JSON` (special type).
     * </pre>
     *
     * <code>JSON = 9;</code>
     */
    public static final int JSON_VALUE = 9;


    public final int getNumber() {
      if (this == UNRECOGNIZED) {
        throw new java.lang.IllegalArgumentException(
            "Can't get the number of an unknown enum value.");
      }
      return value;
    }

    /**
     * @param value The numeric wire value of the corresponding enum entry.
     * @return The enum associated with the given numeric wire value.
     * @deprecated Use {@link #forNumber(int)} instead.
     */
    @java.lang.Deprecated
    public static SpannerType valueOf(int value) {
      return forNumber(value);
    }

    /**
     * @param value The numeric wire value of the corresponding enum entry.
     * @return The enum associated with the given numeric wire value.
     */
    public static SpannerType forNumber(int value) {
      switch (value) {
        case 0: return UNSPECIFIED_TYPE;
        case 1: return STRING;
        case 2: return NUMERIC;
        case 3: return FLOAT64;
        case 4: return INT64;
        case 5: return BYTES;
        case 6: return BOOL;
        case 7: return DATE;
        case 8: return TIMESTAMP;
        case 9: return JSON;
        default: return null;
      }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<SpannerType>
        internalGetValueMap() {
      return internalValueMap;
    }
    private static final com.google.protobuf.Internal.EnumLiteMap<
        SpannerType> internalValueMap =
          new com.google.protobuf.Internal.EnumLiteMap<SpannerType>() {
            public SpannerType findValueByNumber(int number) {
              return SpannerType.forNumber(number);
            }
          };

    public final com.google.protobuf.Descriptors.EnumValueDescriptor
        getValueDescriptor() {
      if (this == UNRECOGNIZED) {
        throw new java.lang.IllegalStateException(
            "Can't get the descriptor of an unrecognized enum value.");
      }
      return getDescriptor().getValues().get(ordinal());
    }
    public final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptorForType() {
      return getDescriptor();
    }
    public static final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptor() {
      return tools.elide.model.SpannerOptions.getDescriptor().getEnumTypes().get(0);
    }

    private static final SpannerType[] VALUES = values();

    public static SpannerType valueOf(
        com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
      if (desc.getType() != getDescriptor()) {
        throw new java.lang.IllegalArgumentException(
          "EnumValueDescriptor is not for this type.");
      }
      if (desc.getIndex() == -1) {
        return UNRECOGNIZED;
      }
      return VALUES[desc.getIndex()];
    }

    private final int value;

    private SpannerType(int value) {
      this.value = value;
    }

    // @@protoc_insertion_point(enum_scope:model.SpannerOptions.SpannerType)
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof tools.elide.model.SpannerOptions)) {
      return super.equals(obj);
    }
    tools.elide.model.SpannerOptions other = (tools.elide.model.SpannerOptions) obj;

    if (!unknownFields.equals(other.unknownFields)) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static tools.elide.model.SpannerOptions parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static tools.elide.model.SpannerOptions parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static tools.elide.model.SpannerOptions parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static tools.elide.model.SpannerOptions parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static tools.elide.model.SpannerOptions parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static tools.elide.model.SpannerOptions parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static tools.elide.model.SpannerOptions parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static tools.elide.model.SpannerOptions parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static tools.elide.model.SpannerOptions parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static tools.elide.model.SpannerOptions parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static tools.elide.model.SpannerOptions parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static tools.elide.model.SpannerOptions parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(tools.elide.model.SpannerOptions prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * <pre>
   * Defines options structures that relate to Google Cloud Spanner.
   * </pre>
   *
   * Protobuf type {@code model.SpannerOptions}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:model.SpannerOptions)
      tools.elide.model.SpannerOptionsOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return tools.elide.model.Datamodel.internal_static_model_SpannerOptions_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return tools.elide.model.Datamodel.internal_static_model_SpannerOptions_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              tools.elide.model.SpannerOptions.class, tools.elide.model.SpannerOptions.Builder.class);
    }

    // Construct using tools.elide.model.SpannerOptions.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return tools.elide.model.Datamodel.internal_static_model_SpannerOptions_descriptor;
    }

    @java.lang.Override
    public tools.elide.model.SpannerOptions getDefaultInstanceForType() {
      return tools.elide.model.SpannerOptions.getDefaultInstance();
    }

    @java.lang.Override
    public tools.elide.model.SpannerOptions build() {
      tools.elide.model.SpannerOptions result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public tools.elide.model.SpannerOptions buildPartial() {
      tools.elide.model.SpannerOptions result = new tools.elide.model.SpannerOptions(this);
      onBuilt();
      return result;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof tools.elide.model.SpannerOptions) {
        return mergeFrom((tools.elide.model.SpannerOptions)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(tools.elide.model.SpannerOptions other) {
      if (other == tools.elide.model.SpannerOptions.getDefaultInstance()) return this;
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      tools.elide.model.SpannerOptions parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (tools.elide.model.SpannerOptions) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }
    @java.lang.Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:model.SpannerOptions)
  }

  // @@protoc_insertion_point(class_scope:model.SpannerOptions)
  private static final tools.elide.model.SpannerOptions DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new tools.elide.model.SpannerOptions();
  }

  public static tools.elide.model.SpannerOptions getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<SpannerOptions>
      PARSER = new com.google.protobuf.AbstractParser<SpannerOptions>() {
    @java.lang.Override
    public SpannerOptions parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new SpannerOptions(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<SpannerOptions> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<SpannerOptions> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public tools.elide.model.SpannerOptions getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}
