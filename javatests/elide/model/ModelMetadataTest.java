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

import com.google.protobuf.FieldMask;
import elide.model.PersonRecord.Person;
import elide.model.PersonRecord.PersonKey;
import elide.model.PersonRecord.ContactInfo;
import elide.model.PersonRecord.EnrollEvent;
import org.junit.jupiter.api.Test;
import tools.elide.core.CollectionMode;
import tools.elide.core.Datamodel;
import tools.elide.core.DatapointType;
import tools.elide.core.FieldType;

import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;


/** Tests that model metadata can be acquired with {@link ModelMetadata}. */
@SuppressWarnings({"CodeBlock2Expr", "DuplicatedCode"})
public final class ModelMetadataTest {
  @Test void testResolveQualifiedName() {
    final String expectedQualifiedName = "elide.model.Person";
    final String expectedJavaName = "elide.model.PersonRecord$Person";
    assertEquals(expectedJavaName, Person.class.getName(),
      "fully-qualified Java class name should be expected value");

    // resolve fully-qualified name via `ModelMetadata`
    assertEquals(expectedQualifiedName, ModelMetadata.fullyQualifiedName(Person.getDefaultInstance()),
      "fully-qualified Protobuf type name should be expected value");
  }

  @Test void testResolveModelRole() {
    DatapointType type = ModelMetadata.role(Person.getDefaultInstance());
    DatapointType type2 = ModelMetadata.role(Person.getDescriptor());
    assertEquals(DatapointType.OBJECT, type, "role should be expected value");
    assertEquals(type, type2, "role should be identical if resolved from model or descriptor");
  }

  @Test void testResolveRoleDefaut() {
    DatapointType type = ModelMetadata.role(ContactInfo.getDefaultInstance());
    DatapointType type2 = ModelMetadata.role(Person.getDescriptor());
    assertEquals(DatapointType.OBJECT, type, "role should be expected value");
    assertEquals(type, type2, "role should be identical if resolved from model or descriptor");
  }

  @Test void testMatchRoleExpected() {
    assertTrue(ModelMetadata.matchRole(Person.getDefaultInstance(), DatapointType.OBJECT),
      "role should be expected value for known model");

    assertTrue(ModelMetadata.matchAnyRole(Person.getDefaultInstance(), DatapointType.OBJECT, DatapointType.EVENT),
      "role should be expected value for known model");
  }

  @Test void testMatchRoleNotExpected() {
    assertFalse(ModelMetadata.matchRole(Person.getDefaultInstance(), DatapointType.EVENT),
      "role should be expected value for known model");
    assertFalse(ModelMetadata.matchAnyRole(Person.getDefaultInstance(), DatapointType.TABLE, DatapointType.EVENT),
      "role should be expected value for known model");
  }

  @Test void testMatchRoleEvent() {
    assertEquals(ModelMetadata.role(EnrollEvent.getDescriptor()),
      DatapointType.EVENT,
      "`role` should resolve the correct model role annotation");
    assertTrue(ModelMetadata.matchRole(EnrollEvent.getDescriptor(), DatapointType.EVENT),
      "`role` should resolve the correct model role annotation");
  }

  @Test void testEnforceRoleFail() {
    assertThrows(InvalidModelType.class, () -> {
      ModelMetadata.enforceRole(EnrollEvent.getDescriptor(), DatapointType.OBJECT);
    });
    assertThrows(InvalidModelType.class, () -> {
      ModelMetadata.enforceRole(EnrollEvent.getDefaultInstance(), DatapointType.OBJECT);
    });
    assertThrows(InvalidModelType.class, () -> {
      ModelMetadata.enforceAnyRole(EnrollEvent.getDefaultInstance(), DatapointType.OBJECT, DatapointType.TABLE);
    });
  }

  @Test void testEnforceRole() {
    assertDoesNotThrow(() -> {
      ModelMetadata.enforceRole(EnrollEvent.getDescriptor(), DatapointType.EVENT);
      ModelMetadata.enforceRole(EnrollEvent.getDefaultInstance(), DatapointType.EVENT);
      ModelMetadata.enforceAnyRole(EnrollEvent.getDefaultInstance(), DatapointType.EVENT, DatapointType.TABLE);
      ModelMetadata.enforceAnyRole(EnrollEvent.getDefaultInstance(), DatapointType.TABLE, DatapointType.EVENT);
    });
  }

  @Test void testResolveModelAnnotation() {
    var type = ModelMetadata.modelAnnotation(
      Person.getDefaultInstance(),
      Datamodel.db,
      false);
    assertNotNull(type, "should never get `null` from `modelAnnotation`");
    assertFalse(type.isPresent(), "should not find `db` annotation on top-level model");

    var type2 = ModelMetadata.modelAnnotation(
      Person.getDefaultInstance(),
      Datamodel.db,
      true);
    assertNotNull(type2, "should never get `null` from `modelAnnotation`");
    assertTrue(type2.isPresent(), "should find `db` annotation on sub-model");
    assertEquals(type2.get().getMode(),
      CollectionMode.COLLECTION,
      "anotation resolved recursively should report accurately");

    var role = ModelMetadata.modelAnnotation(
      Person.getDefaultInstance(),
      Datamodel.role,
      true);
    assertNotNull(type, "should never get `null` from `modelAnnotation`");
    assertTrue(role.isPresent(), "should find `role` annotation on model");
  }

  @Test void testResolveAnnotatedField() {
    // manually resolve an ID field (should fail)
    var missingId = ModelMetadata.annotatedField(
      Person.getDefaultInstance(),
      Datamodel.field,
      false,
      Optional.of((field) -> field.getType().equals(FieldType.ID)));
    var missingId2 = ModelMetadata.annotatedField(
      Person.getDescriptor(),
      Datamodel.field,
      false,
      Optional.of((field) -> field.getType().equals(FieldType.ID)));

    assertNotNull(missingId, "should never get `null` from `annotatedField`");
    assertFalse(missingId.isPresent(), "`missingId` should report as not-present");
    assertNotNull(missingId2, "should never get `null` from `annotatedField`");
    assertFalse(missingId2.isPresent(), "`missingId` should report as not-present");

    // manually resolve a key field (should succeed)
    var keyField = ModelMetadata.annotatedField(
      Person.getDefaultInstance(),
      Datamodel.field,
      false,
      Optional.of((field) -> field.getType().equals(FieldType.KEY)));
    var keyField2 = ModelMetadata.annotatedField(
      Person.getDescriptor(),
      Datamodel.field,
      false,
      Optional.of((field) -> field.getType().equals(FieldType.KEY)));

    assertNotNull(keyField, "should never get `null` from `annotatedField`");
    assertTrue(keyField.isPresent(), "`keyField` should report as present");
    assertNotNull(keyField2, "should never get `null` from `annotatedField`");
    assertTrue(keyField2.isPresent(), "`keyField` should report as present");

    // manually resolve an ID field (recursively, should succeed)
    var foundId = ModelMetadata.annotatedField(
      Person.getDefaultInstance(),
      Datamodel.field,
      true,
      Optional.of((field) -> field.getType().equals(FieldType.ID)));
    var foundId2 = ModelMetadata.annotatedField(
      Person.getDescriptor(),
      Datamodel.field,
      true,
      Optional.of((field) -> field.getType().equals(FieldType.ID)));

    assertNotNull(foundId, "should never get `null` from `annotatedField`");
    assertTrue(foundId.isPresent(), "`foundId` should report as present");
    assertNotNull(foundId2, "should never get `null` from `annotatedField`");
    assertTrue(foundId2.isPresent(), "`foundId2` should report as present");

    // first collection field: should fail
    var firstCollectionAnno = ModelMetadata.annotatedField(
      Person.getDefaultInstance(),
      Datamodel.collection);

    assertNotNull(firstCollectionAnno, "should never get `null` from `annotatedField`");
    assertFalse(firstCollectionAnno.isPresent(), "`firstCollectionAnno` should report as not-present");

    // first collection field: should fail
    Optional<ModelMetadata.FieldPointer> foundSpecialField = ModelMetadata.annotatedField(
      Person.getDescriptor(),
      Datamodel.field);

    assertNotNull(foundSpecialField, "should never get `null` from `annotatedField`");
    assertTrue(foundSpecialField.isPresent(), "`foundSpecialField` should report as present");

    assertEquals(foundSpecialField.get().getPath(), "key",
      "resolved path for key should be `key`");
    assertEquals(foundSpecialField.get().getBase().getName(), "Person",
      "resolved base type should be `Person`");
    assertEquals(foundSpecialField.get().getField().getName(), "key",
      "resolved field name should be `key`");
  }

  @Test void testKeyFieldResolve() {
    // resolve a known-to-exist key field
    var keyField = ModelMetadata.keyField(Person.getDefaultInstance());
    var keyField2 = ModelMetadata.keyField(Person.getDescriptor());

    assertNotNull(keyField, "should never get `null` from `keyField`");
    assertNotNull(keyField2, "should never get `null` from `keyField`");
    assertTrue(keyField.isPresent(), "`keyField` should report as present");
    assertTrue(keyField2.isPresent(), "`keyField` should report as present");

    assertThrows(InvalidModelType.class, () -> {
      ModelMetadata.keyField(EnrollEvent.getDescriptor());
    });
    assertThrows(InvalidModelType.class, () -> {
      ModelMetadata.keyField(EnrollEvent.getDefaultInstance());
    });
  }

  @Test void testIdFieldResolve() {
    // resolve a known-to-exist key field
    var idField = ModelMetadata.idField(Person.getDefaultInstance());
    var idField2 = ModelMetadata.idField(Person.getDescriptor());
    var idField3 = ModelMetadata.idField(PersonKey.getDescriptor());
    var idField4 = ModelMetadata.idField(ContactInfo.getDescriptor());

    assertNotNull(idField, "should never get `null` from `idField`");
    assertNotNull(idField2, "should never get `null` from `idField`");
    assertNotNull(idField3, "should never get `null` from `idField`");
    assertNotNull(idField4, "should never get `null` from `idField`");
    assertTrue(idField.isPresent(), "`idField` should report as present");
    assertTrue(idField2.isPresent(), "`idField` should report as present");
    assertTrue(idField3.isPresent(), "`idField` should report as present");
    assertFalse(idField4.isPresent(), "`idField` for missing ID should report as not-present");
  }

  @Test void testPluckArbitraryField() {
    var model = Person.newBuilder()
      .setName("Jane Doe")
      .build();

    assertTrue(ModelMetadata.pluck(model, "name").getValue().isPresent(),
      "should be able to pluck arbitrary present field");
    assertEquals(model.getName(), ModelMetadata.pluck(model, "name").getValue().get(),
      "should be able to pluck arbitrary present field value");
    assertEquals("name", ModelMetadata.pluck(model, "name").getField().getPath(),
      "should be able to resolve field path from plucked field");

    assertThrows(IllegalArgumentException.class, () -> {
      ModelMetadata.pluck(model, ".name");
    });
    assertThrows(IllegalArgumentException.class, () -> {
      ModelMetadata.pluck(model, "name.");
    });
    assertThrows(IllegalArgumentException.class, () -> {
      ModelMetadata.pluck(model, "name ");
    });
  }

  @Test void testPluckArbitraryFieldRecursive() {
    var model = Person.newBuilder()
      .setName("Jane Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("jane@doe.com"))
      .build();

    assertTrue(ModelMetadata.pluck(model, "name").getValue().isPresent(),
      "should be able to pluck arbitrary present field");
    assertTrue(ModelMetadata.pluck(model, "contact_info.email_address").getValue().isPresent(),
      "should be able to pluck arbitrary present field, recursively");
    assertFalse(ModelMetadata.pluck(model, "contact_info.address.first_line").getValue().isPresent(),
      "should be able to pluck arbitrary present field that is in an empty message, recursively");

    assertThrows(IllegalArgumentException.class, () -> {
      ModelMetadata.pluck(model, "does_not_exist");
    });
    assertThrows(IllegalArgumentException.class, () -> {
      ModelMetadata.pluck(model, "name.does_not_exist");
    });
    assertThrows(IllegalArgumentException.class, () -> {
      ModelMetadata.pluck(model, "contact_info.does_not_exist");
    });
  }

  @Test void testPluckAfterResolve() {
    var model = Person.newBuilder()
      .setName("Jane Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("jane@doe.com"))
      .build();

    assertTrue(ModelMetadata.pluck(model, "name").getValue().isPresent(),
      "should be able to pluck arbitrary present field");
    assertTrue(ModelMetadata.pluck(model, "contact_info.email_address").getValue().isPresent(),
      "should be able to pluck arbitrary present field, recursively");
    assertFalse(ModelMetadata.pluck(model, "contact_info.address.first_line").getValue().isPresent(),
      "should be able to pluck arbitrary present field that is in an empty message, recursively");
  }

  @Test void testResolveArbitraryField() {
    var model = Person.newBuilder()
      .setName("Jane Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("jane@doe.com"))
      .build();

    Optional<ModelMetadata.FieldPointer> name = ModelMetadata.resolveField(model, "name");
    Optional<ModelMetadata.FieldPointer> phone = ModelMetadata.resolveField(model, "contact_info.phone_e164");
    Optional<ModelMetadata.FieldPointer> address = ModelMetadata.resolveField(model, "contact_info.address");
    Optional<ModelMetadata.FieldPointer> nope = ModelMetadata.resolveField(model, "contact_info.blablabla");
    Optional<ModelMetadata.FieldPointer> nuhuh = ModelMetadata.resolveField(model, "yoyoyoyoy");

    assertNotNull(name, "should never get `null` from `resolveField`");
    assertNotNull(phone, "should never get `null` from `resolveField`");
    assertNotNull(address, "should never get `null` from `resolveField`");
    assertNotNull(nope, "should never get `null` from `resolveField`");
    assertNotNull(nuhuh, "should never get `null` from `resolveField`");
    assertTrue(name.isPresent(), "name field should be found");
    assertTrue(phone.isPresent(), "phone field should be found (despite being on sub-record)");
    assertTrue(address.isPresent(), "phone field should be found (despite being a message field)");
    assertFalse(nope.isPresent(), "missing sub-field should return empty optional");
    assertFalse(nuhuh.isPresent(), "missing top-field should return empty optional");

    var descriptor = model.getDescriptorForType();
    Optional<ModelMetadata.FieldPointer> name2 = ModelMetadata.resolveField(descriptor, "name");
    Optional<ModelMetadata.FieldPointer> phone2 = ModelMetadata.resolveField(descriptor, "contact_info.phone_e164");
    Optional<ModelMetadata.FieldPointer> address2 = ModelMetadata.resolveField(descriptor, "contact_info.address");
    Optional<ModelMetadata.FieldPointer> nope2 = ModelMetadata.resolveField(descriptor, "contact_info.blablabla");
    Optional<ModelMetadata.FieldPointer> nuhuh2 = ModelMetadata.resolveField(descriptor, "yoyoyoyoy");

    assertNotNull(name2, "should never get `null` from `resolveField`");
    assertNotNull(phone2, "should never get `null` from `resolveField`");
    assertNotNull(address2, "should never get `null` from `resolveField`");
    assertNotNull(nope2, "should never get `null` from `resolveField`");
    assertNotNull(nuhuh2, "should never get `null` from `resolveField`");
    assertTrue(name2.isPresent(), "name field should be found");
    assertTrue(phone2.isPresent(), "phone field should be found (despite being on sub-record)");
    assertTrue(address2.isPresent(), "phone field should be found (despite being a message field)");
    assertFalse(nope2.isPresent(), "missing sub-field should return empty optional");
    assertFalse(nuhuh2.isPresent(), "missing top-field should return empty optional");
    assertEquals(name.get(), name2.get(), "should resolve identical field regardless of method");
    assertEquals(phone.get(), phone2.get(), "should resolve identical field regardless of method");
    assertEquals(address.get(), address2.get(), "should resolve identical field regardless of method");
  }

  @Test void testFieldPointer() {
    var model = Person.newBuilder()
      .setName("Jane Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("jane@doe.com"))
      .build();

    var descriptor = model.getDescriptorForType();
    Optional<ModelMetadata.FieldPointer> name = ModelMetadata.resolveField(model, "name");
    Optional<ModelMetadata.FieldPointer> phone = ModelMetadata.resolveField(model, "contact_info.phone_e164");
    Optional<ModelMetadata.FieldPointer> name2 = ModelMetadata.resolveField(descriptor, "name");
    Optional<ModelMetadata.FieldPointer> phone2 = ModelMetadata.resolveField(descriptor, "contact_info.phone_e164");

    assertNotNull(name, "should never get `null` from `resolveField`");
    assertNotNull(phone, "should never get `null` from `resolveField`");
    assertNotNull(name2, "should never get `null` from `resolveField`");
    assertNotNull(phone2, "should never get `null` from `resolveField`");
    assertTrue(name.isPresent(), "name field should be found");
    assertTrue(phone.isPresent(), "phone field should be found (despite being on sub-record)");
    assertTrue(name2.isPresent(), "name field should be found");
    assertTrue(phone2.isPresent(), "phone field should be found (despite being on sub-record)");

    // test `equals` for field pointers
    assertEquals(name.get(), name2.get(), "field pointers to the same field should be considered equal");
    assertEquals(phone.get(), phone2.get(), "field pointers to the same field should be considered equal");

    // test `toString` for field pointers
    assertTrue(name.get().toString().contains("FieldPointer"), "field pointer should mention its own class");
    assertTrue(name.get().toString().contains("name"), "field pointer should mention pointed-to field");
    assertTrue(name.get().toString().contains("Person"), "field pointer should mention base type");

    assertDoesNotThrow(() -> {
      HashSet<ModelMetadata.FieldPointer> fields = new HashSet<>(2);
      fields.add(name.get());
      fields.add(phone.get());
      assertEquals(2, fields.size(), "should be able to use `hashCode` on field pointers");
      assertFalse(fields.add(name.get()), "hash code should be stable for a given field pointer");
    });

    assertThrows(IllegalArgumentException.class, () -> {
      ModelMetadata.resolveField(model, ".name");
    });
    assertThrows(IllegalArgumentException.class, () -> {
      ModelMetadata.resolveField(model, "name.");
    });
    assertThrows(IllegalArgumentException.class, () -> {
      ModelMetadata.resolveField(model, "name ");
    });
    assertThrows(IllegalArgumentException.class, () -> {
      ModelMetadata.resolveField(model, "name.yoyoyoy");
    });
    assertThrows(IllegalArgumentException.class, () -> {
      ModelMetadata.resolveField(model, "contact_info.phone_e164.yoyoyoy");
    });
  }

  @Test void testPluckFromPointer() {
    var testVal = "Jane Doe";
    var model = Person.newBuilder()
      .setName(testVal)
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("jane@doe.com"))
      .build();

    var descriptor = model.getDescriptorForType();
    Optional<ModelMetadata.FieldPointer> name = ModelMetadata.resolveField(descriptor, "name");
    assertNotNull(name, "should never get `null` from `resolveField`");
    assertTrue(name.isPresent(), "should be able to find top-level `name` record");

    // try to pluck the field value
    ModelMetadata.FieldContainer<String> pluckedName = ModelMetadata.pluck(model, "name");
    ModelMetadata.FieldContainer<String> pluckedName2 = ModelMetadata.pluck(model, name.get());

    assertNotNull(pluckedName, "should never get `null` from `pluck` with path");
    assertNotNull(pluckedName2, "should never get `null` from `pluck` with field pointer");
    assertEquals(pluckedName.getField().getPath(), pluckedName2.getField().getPath(),
      "resolved field paths should be identical for the same field");
    assertTrue(pluckedName.getValue().isPresent(), "plucked field by path with value should be present");
    assertTrue(pluckedName2.getValue().isPresent(), "plucked field by pointer with value should be present");
    assertEquals(testVal, pluckedName.getValue().get(), "plucked field value should be expected value");
    assertEquals(testVal, pluckedName2.getValue().get(), "plucked field value should be expected value");
  }

  @Test void testPluckMultiple() {
    var testVal = "Jane Doe";
    var model = Person.newBuilder()
      .setName(testVal)
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("jane@doe.com"))
      .build();

    var mask = FieldMask.newBuilder()
      .addPaths("name")
      .addPaths("contact_info.email_address")
      .addPaths("contact_info.phone_e164")
      .build();

    var listFromStream = ModelMetadata.pluckStream(model, mask).collect(Collectors.toList());
    assertEquals(3, listFromStream.size(), "should be able to find all 3 properties");

    var emailField = listFromStream.get(0);
    var phoneField = listFromStream.get(1);
    var nameField = listFromStream.get(2);
    assertEquals("email_address", emailField.getField().getName(), "first field should be email");
    assertEquals("phone_e164", phoneField.getField().getName(), "phone should be sorted second");
    assertEquals("name", nameField.getField().getPath(), "name should be sorted last");

    var fieldset = ModelMetadata.pluckAll(model, mask);
    assertEquals(3, fieldset.size(), "should be able to find all 3 properties");

    var emailField2 = fieldset.first();
    var nameField2 = fieldset.last();
    assertEquals("email_address", emailField2.getField().getName(), "first field should be email");
    assertEquals("name", nameField2.getField().getName(), "last field should be name");
    assertEquals("emailAddress", emailField2.getField().getJsonName(),
      "JSON name should be sensible for field");
  }

  @Test void testFieldContainer() {
    var testVal = "Jane Doe";
    var model = Person.newBuilder()
      .setName(testVal)
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("jane@doe.com"))
      .build();

    ModelMetadata.FieldContainer<String> nameField = ModelMetadata.pluck(model, "name");
    ModelMetadata.FieldContainer<String> nameField2 = ModelMetadata.pluck(model, "name");
    assertNotNull(nameField, "should never get null from `pluck`");
    assertTrue(nameField.getValue().isPresent(), "should have a value present for an initialized field");
    assertEquals(testVal, nameField.getValue().get(), "name field value should be expected value");
    assertEquals(nameField.getField(), nameField2.getField(), "identical fields should be equal");
    assertEquals(nameField, nameField2, "identical field containers should be equal");
    assertEquals(nameField.getField().hashCode(), nameField2.getField().hashCode(),
      "identical fields' hash codes should be equal");
    assertEquals(nameField.hashCode(), nameField2.hashCode(),
      "identical field containers' hash codes should be equal");
    assertTrue(nameField.toString().contains("FieldContainer"), "field container should mention what it is");
    assertTrue(nameField.toString().contains("name"), "field container should mention the field name");
    assertTrue(nameField.toString().contains("Person"), "field container should mention the base type");
  }

  @Test void testResolveId() {
    var testVal = "abc123";
    var model = Person.newBuilder()
      .setKey(PersonKey.newBuilder()
        .setId(testVal))
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com"))
      .build();

    var modelNoId = Person.newBuilder()
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com"))
      .build();

    Optional<String> personId = ModelMetadata.id(model);
    assertNotNull(personId, "should never get `null` from `id`");
    assertTrue(personId.isPresent(), "filled-in ID should be present");
    assertEquals(testVal, personId.get(), "filled-in ID should be provided test value");

    Optional<String> missingId = ModelMetadata.id(modelNoId);
    assertNotNull(personId, "should never get `null` from `id`");
    assertFalse(missingId.isPresent(), "missing ID should not be present");

    assertThrows(MissingAnnotatedField.class, () -> {
      ModelMetadata.id(ContactInfo.getDefaultInstance());
    });
    assertDoesNotThrow(() -> {
      ModelMetadata.id(Person.getDefaultInstance());
    });
  }

  @Test void testResolveKey() {
    var testId = "abc123";
    var testVal = PersonKey.newBuilder()
      .setId(testId)
      .build();
    var model = Person.newBuilder()
      .setKey(testVal)
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com"))
      .build();

    var modelNoKey = Person.newBuilder()
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com"))
      .build();

    Optional<PersonKey> personKey = ModelMetadata.key(model);
    assertNotNull(personKey, "should never get `null` from `key`");
    assertTrue(personKey.isPresent(), "filled-in key should be present");
    assertEquals(testId, personKey.get().getId(), "filled-in ID should be provided test value");

    Optional<String> missingKey = ModelMetadata.key(modelNoKey);
    assertNotNull(missingKey, "should never get `null` from `key`");
    assertFalse(missingKey.isPresent(), "missing key should not be present");

    assertThrows(MissingAnnotatedField.class, () -> {
      ModelMetadata.key(ContactInfo.getDefaultInstance());
    });
    assertDoesNotThrow(() -> {
      ModelMetadata.key(Person.getDefaultInstance());
    });
  }

  @Test void testSpliceArbitrary() {
    var model = Person.newBuilder()
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com"))
      .build();

    Person model2 = ModelMetadata.splice(model, "name", Optional.of("Jane Doe"));
    assertNotNull(model2, "should not get `null` from `splice`");
    assertEquals("Jane Doe", model2.getName(), "new name should be affixed after splice");

    Person model3 = ModelMetadata.splice(model, "name", Optional.empty());
    assertNotNull(model3, "should not get `null` from `splice`");
    assertEquals("", model3.getName(), "name should be empty after clearing with splice");

    Person model4 = ModelMetadata.splice(model3, "contact_info.email_address", Optional.empty());
    assertNotNull(model4, "should not get `null` from `splice`");
    assertEquals("", model4.getName(),
      "name should stil be empty after clearing with splice");
    assertEquals("", model4.getContactInfo().getEmailAddress(),
      "email should be empty after clearing with splice");

    Person model5 = ModelMetadata.splice(model4, "contact_info.email_address", Optional.of("hello@hello.com"));
    assertEquals("hello@hello.com", model5.getContactInfo().getEmailAddress(),
      "email should be updated after setting with splice");

    assertThrows(IllegalArgumentException.class, () -> {
      ModelMetadata.splice(model, ".name", Optional.of("Jane Doe"));
    });
    assertThrows(IllegalArgumentException.class, () -> {
      ModelMetadata.splice(model, "name.", Optional.of("Jane Doe"));
    });
    assertThrows(IllegalArgumentException.class, () -> {
      ModelMetadata.splice(model, "name ", Optional.of("Jane Doe"));
    });
    assertThrows(IllegalArgumentException.class, () -> {
      ModelMetadata.splice(model, "name.yoyoyoy", Optional.of("Jane Doe"));
    });
    assertThrows(IllegalArgumentException.class, () -> {
      ModelMetadata.splice(model, "yoyoyoy", Optional.of("Jane Doe"));
    });
  }

  @Test void testSpliceId() {
    var testId = "abc123";
    var testVal = PersonKey.newBuilder()
      .setId(testId)
      .build();

    var modelWithId = Person.newBuilder()
      .setName("John Doe")
      .setKey(testVal)
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com"))
      .build();

    var modelNoId = Person.newBuilder()
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com"))
      .build();

    Person personIdAdded = ModelMetadata.spliceId(modelNoId, Optional.of(testId));
    assertNotNull(personIdAdded, "should not get `null` from `spliceId`");
    assertEquals(testId, personIdAdded.getKey().getId(), "spliced-in ID value should be expected value");

    Person personIdSame = ModelMetadata.spliceId(modelWithId, Optional.of(testId));
    assertNotNull(personIdSame, "should not get `null` from `spliceId`");
    assertEquals(testId, personIdSame.getKey().getId(), "spliced-in ID value should be expected value");

    Person personIdOverwrite = ModelMetadata.spliceId(modelWithId, Optional.of("yoyoyoy"));
    assertNotNull(personIdOverwrite, "should not get `null` from `spliceId`");
    assertEquals("yoyoyoy", personIdOverwrite.getKey().getId(),
      "spliced-in ID value overwrite should be expected value");

    Person personIdClear = ModelMetadata.spliceId(modelWithId, Optional.empty());
    assertNotNull(personIdClear, "should not get `null` from `spliceId`");
    assertEquals("", personIdClear.getKey().getId(),
      "splice-cleared ID from model should be empty");

    Person personIdSameClear = ModelMetadata.spliceId(modelNoId, Optional.empty());
    assertNotNull(personIdSameClear, "should not get `null` from `spliceId`");
    assertEquals("", personIdSameClear.getKey().getId(),
      "already-cleared ID from model should be empty");

    assertThrows(MissingAnnotatedField.class, () -> {
      ModelMetadata.spliceId(ContactInfo.getDefaultInstance(), Optional.of("hello"));
    });
    assertThrows(InvalidModelType.class, () -> {
      ModelMetadata.spliceId(EnrollEvent.getDefaultInstance(), Optional.of("hello"));
    });
  }

  @Test void testSpliceKey() {
    var testId = "abc123";
    var testVal = PersonKey.newBuilder()
      .setId(testId)
      .build();

    var modelWithKey = Person.newBuilder()
      .setName("John Doe")
      .setKey(testVal)
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com"))
      .build();

    var modelNoKey = Person.newBuilder()
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com"))
      .build();

    var overwriteKey = PersonKey.newBuilder()
      .setId("yoyoyoy")
      .build();

    Person personKeyAdded = ModelMetadata.spliceKey(modelNoKey, Optional.of(testVal));
    assertNotNull(personKeyAdded, "should not get `null` from `spliceKey`");
    assertEquals(testId, personKeyAdded.getKey().getId(),
      "spliced-in key ID value should be expected value");

    Person personKeySame = ModelMetadata.spliceKey(modelWithKey, Optional.of(testVal));
    assertNotNull(personKeySame, "should not get `null` from `spliceKey`");
    assertEquals(testId, personKeySame.getKey().getId(), "spliced-in key ID value should be expected value");

    Person personKeyOverwrite = ModelMetadata.spliceKey(modelWithKey, Optional.of(overwriteKey));
    assertNotNull(personKeyOverwrite, "should not get `null` from `spliceKey`");
    assertEquals("yoyoyoy", personKeyOverwrite.getKey().getId(),
      "spliced-in overwritten key ID value overwrite should be expected value");

    Person personKeyClear = ModelMetadata.spliceKey(modelWithKey, Optional.empty());
    assertNotNull(personKeyClear, "should not get `null` from `spliceKey`");
    assertFalse(personKeyClear.hasKey(),
      "splice-cleared key means a model should not have a key");

    Person personKeySameClear = ModelMetadata.spliceKey(modelNoKey, Optional.empty());
    assertNotNull(personKeySameClear, "should not get `null` from `spliceKey`");
    assertFalse(personKeySameClear.hasKey(), "already-cleared key from model should be empty");

    assertThrows(IllegalArgumentException.class, () -> {
      ModelMetadata.spliceId(modelWithKey, Optional.of(overwriteKey));
    });
    assertThrows(ClassCastException.class, () -> {
      ModelMetadata.spliceKey(modelWithKey, Optional.of(ContactInfo.newBuilder().build()));
    });
    assertThrows(MissingAnnotatedField.class, () -> {
      ModelMetadata.spliceKey(
        ContactInfo.getDefaultInstance(),
        Optional.of(PersonKey.newBuilder().setId("hi").build()));
    });
    assertThrows(InvalidModelType.class, () -> {
      ModelMetadata.spliceKey(
        EnrollEvent.getDefaultInstance(),
        Optional.of(PersonKey.newBuilder().setId("hi").build()));
    });
  }
}
