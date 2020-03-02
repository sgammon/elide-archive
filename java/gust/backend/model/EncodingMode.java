package gust.backend.model;


/** Wire format mode to apply when serializing or deserializing. */
public enum EncodingMode {
  /** Use Protobuf binary serialization. */
  BINARY,

  /** Use Protobuf JSON serialization. */
  JSON
}
