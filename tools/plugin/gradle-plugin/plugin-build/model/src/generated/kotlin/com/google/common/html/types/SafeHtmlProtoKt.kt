//Generated by the protocol buffer compiler. DO NOT EDIT!
// source: webutil/html/types/html.proto

package com.google.common.html.types;

@kotlin.jvm.JvmName("-initializesafeHtmlProto")
public inline fun safeHtmlProto(block: com.google.common.html.types.SafeHtmlProtoKt.Dsl.() -> kotlin.Unit): com.google.common.html.types.SafeHtmlProto =
  com.google.common.html.types.SafeHtmlProtoKt.Dsl._create(com.google.common.html.types.SafeHtmlProto.newBuilder()).apply { block() }._build()
public object SafeHtmlProtoKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.google.common.html.types.SafeHtmlProto.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.google.common.html.types.SafeHtmlProto.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.google.common.html.types.SafeHtmlProto = _builder.build()

    /**
     * <pre>
     * IMPORTANT: Never set or read this field, even from tests, it is private.
     * See documentation at the top of .proto file for programming language
     * packages with which to create or read this message.
     * </pre>
     *
     * <code>optional string private_do_not_access_or_else_safe_html_wrapped_value = 2 [ctype = CORD];</code>
     */
    public var privateDoNotAccessOrElseSafeHtmlWrappedValue: kotlin.String
      @JvmName("getPrivateDoNotAccessOrElseSafeHtmlWrappedValue")
      get() = _builder.getPrivateDoNotAccessOrElseSafeHtmlWrappedValue()
      @JvmName("setPrivateDoNotAccessOrElseSafeHtmlWrappedValue")
      set(value) {
        _builder.setPrivateDoNotAccessOrElseSafeHtmlWrappedValue(value)
      }
    /**
     * <pre>
     * IMPORTANT: Never set or read this field, even from tests, it is private.
     * See documentation at the top of .proto file for programming language
     * packages with which to create or read this message.
     * </pre>
     *
     * <code>optional string private_do_not_access_or_else_safe_html_wrapped_value = 2 [ctype = CORD];</code>
     */
    public fun clearPrivateDoNotAccessOrElseSafeHtmlWrappedValue() {
      _builder.clearPrivateDoNotAccessOrElseSafeHtmlWrappedValue()
    }
    /**
     * <pre>
     * IMPORTANT: Never set or read this field, even from tests, it is private.
     * See documentation at the top of .proto file for programming language
     * packages with which to create or read this message.
     * </pre>
     *
     * <code>optional string private_do_not_access_or_else_safe_html_wrapped_value = 2 [ctype = CORD];</code>
     * @return Whether the privateDoNotAccessOrElseSafeHtmlWrappedValue field is set.
     */
    public fun hasPrivateDoNotAccessOrElseSafeHtmlWrappedValue(): kotlin.Boolean {
      return _builder.hasPrivateDoNotAccessOrElseSafeHtmlWrappedValue()
    }
  }
}
@kotlin.jvm.JvmSynthetic
public inline fun com.google.common.html.types.SafeHtmlProto.copy(block: com.google.common.html.types.SafeHtmlProtoKt.Dsl.() -> kotlin.Unit): com.google.common.html.types.SafeHtmlProto =
  com.google.common.html.types.SafeHtmlProtoKt.Dsl._create(this.toBuilder()).apply { block() }._build()
