/*
 * Copyright © 2022, The Elide Framework Authors. All rights reserved.
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
package elide.model;

import elide.model.PersonRecord.Person;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;


/** Tests for {@link ProtoModelCodec}, which encodes/decodes model instances to/from Protocol Buffers wire formats. */
@SuppressWarnings("DuplicatedCode")
public final class ProtoModelCodecTest {
  /** Check for interface compliance regarding {@link ModelCodec}. */
  @Test void testCodecInterface() {
    ModelCodec<Person, EncodedModel, EncodedModel> personCodec = ProtoModelCodec.forModel(Person.getDefaultInstance());
    assertNotNull(personCodec.serializer(), "codec should produce a serializer");
    assertNotNull(personCodec.deserializer(), "codec should produce a de-serializer");
  }

  /** Test that the codec can encode models in the default format. */
  @Test void testCodecDefault() throws IOException {
    ModelCodec<Person, EncodedModel, EncodedModel> personCodec = ProtoModelCodec.forModel(Person.getDefaultInstance());
    Person person = Person.newBuilder().setName("Jane Doe").build();

    EncodedModel encodedPerson = personCodec.serialize(person);
    assertNotNull(encodedPerson, "should not get `null` from serialize");

    EncodedModel encodedPerson2 = personCodec.serializer().deflate(person);
    assertNotNull(encodedPerson2, "should not get `null` from deflate");
    assertEquals(encodedPerson, encodedPerson2, "deflating same model should produce same encoded model");

    // load model
    Person reloaded = encodedPerson.inflate(Person.getDefaultInstance());
    Person reloaded2 = personCodec.deserialize(encodedPerson);
    Person reloaded3 = personCodec.deserializer().inflate(encodedPerson);
    assertNotNull(reloaded, "re-loaded record should not be `null`");
    assertEquals(person.toString(), reloaded.toString(), "re-loaded record should be identical");
    assertEquals(person.toString(), reloaded2.toString(), "re-loaded record 2 should be identical");
    assertEquals(person.toString(), reloaded3.toString(), "re-loaded record 3 should be identical");
  }

  /** Test that the codec can encode models in binary format. */
  @Test void testCodecBinary() throws IOException {
    ModelCodec<Person, EncodedModel, EncodedModel> personCodec = ProtoModelCodec.forModel(
      Person.getDefaultInstance(), EncodingMode.BINARY);
    Person person = Person.newBuilder().setName("Jane Doe").build();

    EncodedModel encodedPerson = personCodec.serialize(person);
    assertNotNull(encodedPerson, "should not get `null` from serialize");

    EncodedModel encodedPerson2 = personCodec.serializer().deflate(person);
    assertNotNull(encodedPerson2, "should not get `null` from deflate");
    assertEquals(encodedPerson, encodedPerson2, "deflating same model should produce same encoded model");

    // load model
    Person reloaded = encodedPerson.inflate(Person.getDefaultInstance());
    Person reloaded2 = personCodec.deserialize(encodedPerson);
    Person reloaded3 = personCodec.deserializer().inflate(encodedPerson);
    assertNotNull(reloaded, "re-loaded record should not be `null`");
    assertEquals(person.toString(), reloaded.toString(), "re-loaded record should be identical");
    assertEquals(person.toString(), reloaded2.toString(), "re-loaded record 2 should be identical");
    assertEquals(person.toString(), reloaded3.toString(), "re-loaded record 3 should be identical");
  }

  /** Test that the codec can encode models in ProtoJSON format. */
  @Test void testCodecJSON() throws IOException {
    ModelCodec<Person, EncodedModel, EncodedModel> personCodec = ProtoModelCodec.forModel(
      Person.getDefaultInstance(), EncodingMode.JSON);
    Person person = Person.newBuilder().setName("Jane Doe").build();

    EncodedModel encodedPerson = personCodec.serialize(person);
    assertNotNull(encodedPerson, "should not get `null` from serialize");

    EncodedModel encodedPerson2 = personCodec.serializer().deflate(person);
    assertNotNull(encodedPerson2, "should not get `null` from deflate");
    assertEquals(encodedPerson, encodedPerson2, "deflating same model should produce same encoded model");

    // load model
    Person reloaded = encodedPerson.inflate(Person.getDefaultInstance());
    Person reloaded2 = personCodec.deserialize(encodedPerson);
    Person reloaded3 = personCodec.deserializer().inflate(encodedPerson);
    assertNotNull(reloaded, "re-loaded record should not be `null`");
    assertEquals(person.toString(), reloaded.toString(), "re-loaded record should be identical");
    assertEquals(person.toString(), reloaded2.toString(), "re-loaded record 2 should be identical");
    assertEquals(person.toString(), reloaded3.toString(), "re-loaded record 3 should be identical");
  }
}
