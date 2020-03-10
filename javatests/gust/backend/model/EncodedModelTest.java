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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import gust.backend.model.PersonRecord.Person;
import gust.backend.model.PersonRecord.ContactInfo;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;


/** Tests for {@link EncodedModel}, which is a container object for encoded models. */
@SuppressWarnings("DuplicatedCode")
public final class EncodedModelTest {
  /** Construct a model instance, then wrap it in {@link EncodedModel} via factory methods. */
  @Test void testConstructFromMessage() {
    Person person = Person.newBuilder()
      .setName("Jane Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("jane@doe.com")
        .setPhoneE164("+12345678901"))
      .build();

    // wrap directly using factories
    EncodedModel one = EncodedModel.from(person);
    EncodedModel two = EncodedModel.from(person, person.getDescriptorForType());
    EncodedModel three = EncodedModel.wrap(
      person.getDescriptorForType().getFullName(),
      EncodingMode.BINARY,
      person.toByteArray());

    // all of them should be identical
    assertNotNull(one, "should not get null object from `EncodedModel.from`");
    assertNotNull(two, "should not get null object from `EncodedModel.from`");
    assertNotNull(three, "should not get null object from `EncodedModel.from`");
    assertEquals(one, two, "`EncodedModel.from` with identical params should produce identical models");
    assertEquals(two, three, "`EncodedModel.wrap` with identical params should produce identical models");
  }

  /** Construct a model instance, then encode it and wrap the encoded output in {@link EncodedModel} manually. */
  @Test void testConstructWrapped() throws InvalidProtocolBufferException {
    Person person = Person.newBuilder()
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com")
        .setPhoneE164("+12345678901"))
      .build();

    EncodedModel one = EncodedModel.wrap(
      person.getDescriptorForType().getFullName(),
      EncodingMode.BINARY,
      person.toByteArray());

    EncodedModel two = EncodedModel.wrap(
      person.getDescriptorForType().getFullName(),
      EncodingMode.JSON,
      "{}".getBytes(StandardCharsets.UTF_8));

    assertNotNull(one, "should not get null object from `EncodedModel.from`");
    assertNotNull(two, "should not get null object from `EncodedModel.from`");
    assertEquals(one.getType(), person.getDescriptorForType().getFullName(),
      "encoded model should properly surface fully-qualified type name");
    assertEquals(one.getDataMode(), EncodingMode.BINARY, "mode should properly show through");
    assertEquals(two.getType(), person.getDescriptorForType().getFullName(),
      "encoded model should properly surface fully-qualified type name");
    assertEquals(two.getDataMode(), EncodingMode.JSON, "mode should properly show through");

    Person reinflated = Person.parseFrom(one.getRawBytes());
    assertNotNull(reinflated, "should not get `null` from `parseFrom`");
    assertEquals(person.toString(), reinflated.toString(),
      "re-inflated encoded model should be identical to original");
  }

  @Test void testEqualsHashcode() {
    Person person = Person.newBuilder()
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com")
        .setPhoneE164("+12345678901"))
      .build();

    EncodedModel one = EncodedModel.wrap(
      person.getDescriptorForType().getFullName(),
      EncodingMode.BINARY,
      person.toByteArray());

    EncodedModel two = EncodedModel.wrap(
      person.getDescriptorForType().getFullName(),
      EncodingMode.BINARY,
      person.toByteArray());

    assertEquals(one, two,
      "constructing identical encoded models should produce identical encoded models");
    assertEquals(one.hashCode(), two.hashCode(),
      "constructing identical encoded models should yield identical hashCode return values");
  }

  @Test void testStringRepresentation() {
    Person person = Person.newBuilder()
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com")
        .setPhoneE164("+12345678901"))
      .build();

    EncodedModel one = EncodedModel.wrap(
      person.getDescriptorForType().getFullName(),
      EncodingMode.BINARY,
      person.toByteArray());

    EncodedModel two = EncodedModel.wrap(
      person.getDescriptorForType().getFullName(),
      EncodingMode.BINARY,
      person.toByteArray());

    Consumer<EncodedModel> enforcer = (item) -> {
      assertTrue(item.toString().contains("EncodedModel"),
        "EncodedModel `.toString()` should mention it is encoded");
      assertTrue(item.toString().contains("BINARY"),
        "EncodedModel `.toString()` should mention its wire format");
      assertTrue(item.toString().contains(person.getDescriptorForType().getFullName()),
        "EncodedModel `.toString()` should mention the type of proto model");
    };
    enforcer.accept(one);
    enforcer.accept(two);
  }

  /** Make sure an {@link EncodedModel} instance is fully serializable and de-serializable by Java. */
  @Test void testEncodedModelJavaSerialization() throws IOException, ClassNotFoundException {
    Person person = Person.newBuilder()
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com")
        .setPhoneE164("+12345678901"))
      .build();

    EncodedModel one = EncodedModel.wrap(
      person.getDescriptorForType().getFullName(),
      EncodingMode.BINARY,
      person.toByteArray());

    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      try (ObjectOutputStream dump = new ObjectOutputStream(out)) {
        dump.writeObject(one);
      }

      byte[] rawBytes = out.toByteArray();
      try (ByteArrayInputStream in = new ByteArrayInputStream(rawBytes)) {
        try (ObjectInputStream load = new ObjectInputStream(in)) {
          EncodedModel fresh = (EncodedModel)load.readObject();

          assertEquals(one, fresh,
            "re-inflated models should equal each other");
          assertEquals(one.toString(), fresh.toString(),
            "re-inflated models should produce identical representation strings");
          assertEquals(one.hashCode(), fresh.hashCode(),
            "re-inflated models should produce identical hash codes");
          assertEquals(one.getType(), fresh.getType(),
            "re-inflated models should produce identical type strings");
          assertEquals(one.getDataMode(), fresh.getDataMode(),
            "re-inflated models should produce identical data model enumerations");
        }
      }
    }
  }

  /** Make sure an {@link EncodedModel} can re-inflate into a full model instance. */
  @Test void testReinflateEncodedModel() throws InvalidProtocolBufferException {
    Person person = Person.newBuilder()
      .setName("Jane Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("jane@doe.com")
        .setPhoneE164("+12345678901"))
      .build();

    EncodedModel one = EncodedModel.wrap(
      person.getDescriptorForType().getFullName(),
      EncodingMode.BINARY,
      person.toByteArray());
    assertNotNull(one, "should not get `null` from `EncodedModel.wrap`");

    Person reinflated = one.inflate(Person.getDefaultInstance());
    assertNotNull(reinflated, "should not get `null` from `EncodedModel.inflate`");

    assertEquals(person.toString(), reinflated.toString(),
      "re-inflated person record should be identical");
  }

  /** Test {@link EncodedModel} instances that store JSON data. */
  @Test void testEncodedModelJSON() throws InvalidProtocolBufferException {
    Person person = Person.newBuilder()
      .setName("Jane Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("jane@doe.com")
        .setPhoneE164("+12345678901"))
      .build();

    EncodedModel one = EncodedModel.wrap(
      person.getDescriptorForType().getFullName(),
      EncodingMode.JSON,
      JsonFormat.printer().omittingInsignificantWhitespace().print(person).getBytes(StandardCharsets.UTF_8));

    assertNotNull(one, "should not get `null` from `EncodedModel.wrap` with JSON");
    assertEquals(one.getDataMode(), EncodingMode.JSON, "data mode should report JSON when using JSON");

    Person reinflated = one.inflate(Person.getDefaultInstance());
    assertNotNull(reinflated, "should not get `null` from `EncodedModel.inflate`");
    assertEquals(person.toString(), reinflated.toString(),
      "proto records reinflated from JSON should be identical");
  }
}
