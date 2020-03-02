package gust.backend.model;

import gust.backend.model.WriteOptions.WriteDisposition;
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
