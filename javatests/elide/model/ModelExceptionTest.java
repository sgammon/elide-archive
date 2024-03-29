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

import elide.model.WriteOptions.WriteDisposition;
import org.junit.jupiter.api.Test;
import tools.elide.core.DatapointType;
import tools.elide.core.FieldType;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;


/** Tests exceptions defined in the backend model layer. */
public class ModelExceptionTest {
  @Test void testBasicModelExceptions() {
    assertNotNull(new InvalidModelType(PersonRecord.Person.getDescriptor(),
        EnumSet.of(DatapointType.TABLE)).getViolatingSchema(),
      "`InvalidModelType` violating schema should be passed through");
    assertNotNull(new InvalidModelType(PersonRecord.Person.getDescriptor(),
        EnumSet.of(DatapointType.TABLE)).getDatapointTypes(),
        "`InvalidModelType` datapoint types should be passed through");
    assertNotNull(InvalidModelType.from(PersonRecord.Person.getDescriptor(),
          EnumSet.of(DatapointType.TABLE)).getViolatingSchema(),
        "`InvalidModelType` violating schema should be passed through");
    assertNotNull(InvalidModelType.from(PersonRecord.Person.newBuilder().build(),
        EnumSet.of(DatapointType.TABLE)).getDatapointTypes(),
      "`InvalidModelType` datapoint types should be passed through");
    assertEquals("sample", new ModelInflateException("sample", new IllegalStateException("hi")).getMessage(),
      "`ModelInflateException` message should be passed through");
    assertEquals("hi", new ModelInflateException(new IllegalStateException("hi")).getCause().getMessage(),
      "`ModelInflateException` cause-only message should be passed through");
    assertEquals("sample", new ModelDeflateException("sample", new IllegalStateException("hi")).getMessage(),
      "`ModelDeflateException` message should be passed through");
    assertEquals("hi", new ModelDeflateException(new IllegalStateException("hi")).getCause().getMessage(),
      "`ModelDeflateException` cause-only message should be passed through");
    assertEquals("hi", new ModelDeflateException("sample", new IllegalStateException("hi"))
        .getCause().getMessage(),
      "`ModelDeflateException` cause should be passed through");
    assertEquals(PersistenceFailure.CANCELLED,
      PersistenceOperationFailed.forErr(PersistenceFailure.CANCELLED).getFailure(),
      "`PersistenceOperationFailed` should pass error cases through");
    assertEquals(PersistenceFailure.CANCELLED.getMessage(),
      PersistenceOperationFailed.forErr(PersistenceFailure.CANCELLED).getMessage(),
      "`PersistenceOperationFailed` should pass error case messages through");
    assertEquals(PersistenceFailure.CANCELLED.getMessage(),
      PersistenceOperationFailed.forErr(PersistenceFailure.CANCELLED).getMessage(),
      "`PersistenceOperationFailed` should pass error case messages through");
    assertEquals("example",
      PersistenceOperationFailed.forErr(
        PersistenceFailure.CANCELLED,
        new IllegalStateException("example")).getCause().getMessage(),
      "`PersistenceOperationFailed` should pass cause through");
    assertNotNull(new MissingAnnotatedField(PersonRecord.Person.getDescriptor(),
      FieldType.KEY).getViolatingSchema(),
      "`MissingAnnotatedField` should provide schema that violated the constraint");
    assertEquals(new MissingAnnotatedField(PersonRecord.Person.getDescriptor(),
        FieldType.KEY).getRequiredField(),
      FieldType.KEY,
      "`MissingAnnotatedField` should provide field that was missing");
  }

  @Test void testModelWriteFailure() {
    PersonRecord.Person person = PersonRecord.Person.newBuilder()
      .setName("John Doe")
      .build();
    assertEquals(person.toString(), new ModelWriteFailure("hi", person).getModel().toString(),
      "`ModelWriteFailure` should accurately expose failed model");
    assertEquals("hi", new ModelWriteFailure("hi", person).getKey(),
      "`ModelWriteFailure` should accurately expose failed key");
    assertEquals("errrr", new ModelWriteFailure("hi", person, "errrr").getMessage(),
      "`ModelWriteFailure` should pass through message");
    assertEquals("cause", new ModelWriteFailure("hi", person, new IllegalStateException("cause"))
        .getCause().getMessage(),
      "`ModelWriteFailure` should pass through cause");
    assertEquals("cause",
      new ModelWriteFailure("cause", person, new IllegalStateException("cause"), "errrr")
        .getCause().getMessage(),
      "`ModelWriteFailure` should pass through cause and message");
  }

  @Test void testModelWriteConflict() {
    PersonRecord.Person person = PersonRecord.Person.newBuilder()
      .setName("John Doe")
      .build();
    assertEquals(WriteDisposition.MUST_EXIST,
      new ModelWriteConflict("hi i am a key", person, WriteDisposition.MUST_EXIST).getFailedExpectation(),
      "`ModelWriteConflict` should expose the expectation that was violated");
  }

  @Test void testPersistenceFailureEnum() {
    for (PersistenceFailure failureCase : PersistenceFailure.values()) {
      assertNotNull(failureCase.name(),
        "each known model-layer failure case should have a name");
      assertFalse(failureCase.name().isEmpty(),
        "each known model-layer failure case should have a non-empty name");
      assertNotNull(failureCase.getMessage(),
        "each known model-layer failure case should have a message");
      assertFalse(failureCase.getMessage().isEmpty(),
        "each known model-layer failure case should have a non-empty message");
    }
  }
}
