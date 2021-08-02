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

import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import gust.backend.runtime.Logging;
import gust.backend.runtime.ReactiveFuture;
import gust.backend.model.PersonRecord.Person;
import gust.backend.model.PersonRecord.PersonKey;
import gust.backend.model.PersonRecord.ContactInfo;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.DynamicTest;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;


/** Abstract test factory, which is responsible for testing/enforcing {@link PersistenceDriver} surfaces. */
@SuppressWarnings({"WeakerAccess", "DuplicatedCode", "CodeBlock2Expr", "unchecked", "rawtypes"})
public abstract class GenericPersistenceDriverTest<Driver extends PersistenceDriver> {
  /** Describes data keys touched in this test case. */
  protected final HashSet<Message> touchedKeys = new HashSet<>();

  /** Empty person instance, for testing. */
  private final static Person emptyInstance = Person.getDefaultInstance();

  /** Logger for the test suite. */
  private final static Logger logging = Logging.logger(GenericPersistenceDriverTest.class);

  @TestFactory
  protected final Iterable<DynamicTest> driverTests() {
    final String subcase = this.getClass().getSimpleName();
    List<DynamicTest> tests = this.supportedDriverTests();
    Set<String> unsupported = this.unsupportedDriverTests()
            .orElse(Collections.emptyList())
            .stream()
            .map((name) -> String.format("%s: `%s`", subcase, name))
            .collect(Collectors.toUnmodifiableSet());

    tests.addAll(subclassTests().orElse(Collections.emptyList()));

    return tests.stream().filter((test) ->
      // mark unsupported tests with ignore annotations
      unsupported.isEmpty() || !unsupported.contains(test.getDisplayName())
    ).collect(Collectors.toUnmodifiableList());
  }

  /**
   * @return Set of tests that are currently expected to be supported by this driver.
   */
  protected @Nonnull List<DynamicTest> supportedDriverTests() {
    final String subcase = this.getClass().getSimpleName();
    return Arrays.asList(
      dynamicTest(format("%s: `acquireDriver`", subcase), this::acquireDriver),
      dynamicTest(format("%s: `testDriverCodec`", subcase), this::testDriverCodec),
      dynamicTest(format("%s: `testDriverExecutor`", subcase), this::testDriverExecutor),
      dynamicTest(format("%s: `testGenerateKey`", subcase), this::testGenerateKey),
      dynamicTest(format("%s: `fetchNonExistentEntity`", subcase), this::fetchNonExistentEntity),
      dynamicTest(format("%s: `storeAndFetchEntity`", subcase), this::storeAndFetchEntity),
      dynamicTest(format("%s: `storeAndFetchEntityMasked`", subcase), this::storeAndFetchEntityMasked),
      dynamicTest(format("%s: `storeEntityUpdate`", subcase), this::storeEntityUpdate),
      dynamicTest(format("%s: `createEntityThenUpdate`", subcase), this::createEntityThenUpdate),
      dynamicTest(format("%s: `createUpdateWithInvalidOptions`", subcase), this::createUpdateWithInvalidOptions),
      dynamicTest(format("%s: `createEntityThenDelete`", subcase), this::createEntityThenDelete),
      dynamicTest(format("%s: `createEntityThenDeleteByRecord`", subcase), this::createEntityThenDeleteByRecord),
      dynamicTest(format("%s: `storeEntityUpdateNotFound`", subcase), this::storeEntityUpdateNotFound),
      dynamicTest(format("%s: `storeEntityCollission`", subcase), this::storeEntityCollission)
    );
  }

  /**
   * @return Set of tests that are not currently supported by this driver.
   */
  protected @Nonnull Optional<List<String>> unsupportedDriverTests() {
    return Optional.empty();
  }

  /**
   * @return Additional dynamic tests to add from the specific driver test implementation.
   */
  protected @Nonnull Optional<List<DynamicTest>> subclassTests() {
    return Optional.empty();
  }

  private @Nonnull Driver acquire() {
    return Objects.requireNonNull(driver(), "Cannot test `null` driver.");
  }

  /** Default timeout to apply to async operations. */
  protected @Nonnull Long timeout() {
    return 30L;
  }

  /** Default time unit to apply to async operations. */
  protected @Nonnull TimeUnit timeoutUnit() {
    return TimeUnit.SECONDS;
  }

  /**
   * Driver hook-point for the generic {@link PersistenceDriver} suite of unit compliance tests. Checks that the driver
   * exposes required components, and can perform all common driver tasks.
   *
   * <p>Automatically spawns tests via {@link TestFactory} (using JUnit 5). These tests may be customized on a per-
   * driver basis by overriding individual test methods, such as {@link #testDriverCodec()}. At runtime, during testing,
   * these cases are dynamically generated and run against each driver.</p>
   *
   * @return Persistence driver to execute tests against.
   */
  protected abstract @Nonnull Driver driver();

  // -- Abstract Tests -- //

  /** Implementation-specific driver acquisition test. */
  protected abstract void acquireDriver();

  /** Enforce that the driver returns a valid, usable codec. */
  protected void testDriverCodec() {
    assertNotNull(acquire().codec(), "should never get a null codec from a driver");
  }

  /** Enforce that the driver returns a valid, usable executor. */
  protected void testDriverExecutor() {
    assertNotNull(acquire().executorService(), "should never get a null executor from a driver");
  }

  /** Test random key generation for entity storage. */
  protected void testGenerateKey() {
    HashSet<String> checkedKeys = new HashSet<>();
    List<Message> generatedKeys = Arrays.asList(
      acquire().generateKey(emptyInstance), acquire().generateKey(emptyInstance),
      acquire().generateKey(emptyInstance), acquire().generateKey(emptyInstance),
      acquire().generateKey(emptyInstance), acquire().generateKey(emptyInstance),
      acquire().generateKey(emptyInstance), acquire().generateKey(emptyInstance),
      acquire().generateKey(emptyInstance), acquire().generateKey(emptyInstance));

    for (Message key : generatedKeys) {
      assertNotNull(key, "should not get `null` for generated key from driver");

      Optional<String> id = ModelMetadata.id(key);
      assertNotNull(id, "should not get `null` for `id`");
      assertTrue(id.isPresent(), "generated key should always have an ID");
      assertFalse(id.get().contains(" "), "generated key should not contain spaces");
      assertFalse(id.get().contains("\n"), "generated key should not contain new lines");
      assertTrue(checkedKeys.add(id.get()), "drivers should not generate duplicate keys");
    }
  }

  /** Fetch an entity that should not exist. */
  protected void fetchNonExistentEntity() {
    // setup suite
    HashSet<Object> checkedKeys = new HashSet<>();
    Supplier<Message> keyGenerator = () -> acquire().generateKey(emptyInstance);
    Consumer<Function<Message, ReactiveFuture<Optional<Message>>>> testsuite = (tester) -> {
      // generate a random key for this run, make sure it is not a duplicate
      Message randomKey = keyGenerator.get();
      if (!checkedKeys.add(randomKey))
        fail(format(
          "driver should not generate duplicate keys, but got duplicate: '%s'.", randomKey));

      // with the random key in hand, invoke the tester to produce a future
      ReactiveFuture<Optional<Message>> shouldNotExist = tester.apply(randomKey);

      // check the future
      assertNotNull(shouldNotExist, "should not get `null` from `retrieve` for async");
      assertFalse(shouldNotExist.isCancelled(), "futures should not immediately cancel");

      // resolve the future
      try {
        Optional<Message> record = shouldNotExist.get(timeout(), timeoutUnit());

        // check the resolution state
        assertTrue(shouldNotExist.isDone(), "future should present as done after a value is received");
        assertNotNull(record, "should not get null back for future-provided optional");
        assertFalse(record.isPresent(), "should not find a result for a non-existent record fetch");
      } catch (InterruptedException | ExecutionException | TimeoutException err) {
        fail(format("should not get exception waiting for simple retrieve, but got: '%s'", err.getMessage()));
      }
    };

    // thunk to async
    final Function<Message, ReactiveFuture<Optional<Message>>> asyncify = (message) ->
      ReactiveFuture.done(message != null ? Optional.of(message) : Optional.empty());

    // test against each fetch interface
    @SuppressWarnings("unchecked")
    List<Function<Message, ReactiveFuture<Optional<Message>>>> ops = Arrays.asList(
      (randomKey) -> acquire().retrieve(randomKey, FetchOptions.DEFAULTS),
      (randomKey) -> acquire().fetchAsync(randomKey),
      (randomKey) -> acquire().fetchAsync(randomKey, FetchOptions.DEFAULTS),
      (randomKey) -> (ReactiveFuture<Optional<Message>>)(acquire().fetchReactive(randomKey)),
      (randomKey) -> (ReactiveFuture<Optional<Message>>)(acquire().fetchReactive(randomKey, FetchOptions.DEFAULTS)),
      (randomKey) -> asyncify.apply(acquire().fetch(randomKey)),
      (randomKey) -> asyncify.apply(acquire().fetch(randomKey, FetchOptions.DEFAULTS)),
      (randomKey) -> ReactiveFuture.done(acquire().fetchSafe(randomKey)),
      (randomKey) -> ReactiveFuture.done(acquire().fetchSafe(randomKey, FetchOptions.DEFAULTS)));

    ops.forEach(testsuite);
  }

  /** Create a simple entity, store it, and then fetch it. */
  protected void storeAndFetchEntity() throws TimeoutException, ExecutionException, InterruptedException {
    // persist the record
    Person person = Person.newBuilder()
      .setKey(PersonKey.newBuilder()
        .setId("abc123test")
        .build())
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com")
        .setPhoneE164("+12345679001"))
      .build();

    logging.debug("Original model for storage: \n" + person.toString());
    ReactiveFuture<Person> op = acquire().create(person, WriteOptions.DEFAULTS);
    assertFalse(op.isCancelled(), "future from persist should not start in cancelled state");

    Person model = op.get(timeout(), timeoutUnit());
    logging.debug("Model post-storage:\n" + model.toString());

    assertTrue(op.isDone(), "future should report as done after store operation finishes");
    assertNotNull(model, "should get a model back from a persist operation");
    assertFalse(op.isCancelled(), "write future should not present as cancelled after completing");
    Optional<PersonKey> key = ModelMetadata.key(model);
    assertTrue(key.isPresent(), "key should be present on model after storing");
    touchedKeys.add(key.get());

    var keySpliced = ModelMetadata.spliceKey(model, key);

    // fetch the record
    ReactiveFuture<Optional<Person>> personFuture = acquire().retrieve(key.get(), FetchOptions.DEFAULTS);
    assertNotNull(personFuture, "should not get `null` future for retrieve operation");
    assertFalse(personFuture.isCancelled(), "future for retrieve should not start in cancelled state");

    Optional<Person> refetched = personFuture.get(timeout(), timeoutUnit());
    assertTrue(personFuture.isDone(), "future should present as done after a record is fetched");
    assertFalse(personFuture.isCancelled(), "read future should not present as cancelled after completing");
    assertNotNull(refetched, "should not get `null` for optional after record fetch");
    assertTrue(refetched.isPresent(), "should find record we just stored");
    assertEquals(keySpliced.toString(), refetched.get().toString(),
      "fetched person record should match identically, but with key");
    logging.debug("Model re-fetched:\n" + refetched.get().toString());

    // fetch the record a second time
    ReactiveFuture<Optional<Person>> personFuture2 = acquire().retrieve(key.get(), FetchOptions.DEFAULTS);
    assertNotNull(personFuture2, "should not get `null` future for retrieve operation");
    assertFalse(personFuture2.isCancelled(), "future for retrieve should not start in cancelled state");

    Optional<Person> refetched2 = personFuture2.get(timeout(), timeoutUnit());
    assertTrue(personFuture2.isDone(), "future should present as done after a record is fetched");
    assertFalse(personFuture2.isCancelled(), "read future should not present as cancelled after completing");
    assertNotNull(refetched2, "should not get `null` for optional after record fetch");
    assertTrue(refetched2.isPresent(), "should find record we just stored");
    assertEquals(keySpliced.toString(), refetched2.get().toString(),
      "fetched person record should match identically, but with key");

    var id = ModelMetadata.id(refetched2.get());
    assertTrue(id.isPresent(), "ID should be decoded on fetched models");
  }

  /** Create a simple entity, store it, and then fetch it, but with a field mask. */
  protected void storeAndFetchEntityMasked() throws TimeoutException, ExecutionException, InterruptedException {
    // persist the record
    Person person = Person.newBuilder()
      .setKey(PersonKey.newBuilder()
          .setId("abc123test-masked")
          .build())
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com")
        .setPhoneE164("+12345679001"))
      .build();

    ReactiveFuture<Person> op = acquire().persist(person.getKey(), person, WriteOptions.DEFAULTS);
    assertFalse(op.isCancelled(), "future from persist should not start in cancelled state");

    Person model = op.get(timeout(), timeoutUnit());
    assertTrue(op.isDone(), "future should report as done after store operation finishes");
    assertNotNull(model, "should get a model back from a persist operation");
    assertFalse(op.isCancelled(), "write future should not present as cancelled after completing");
    Optional<PersonKey> key = ModelMetadata.key(model);
    assertTrue(key.isPresent(), "key should be present on model after storing");
    touchedKeys.add(key.get());

    // fetch the record
    ReactiveFuture<Optional<Person>> personFuture = acquire().retrieve(key.get(), new FetchOptions() {
      @Override
      public @Nonnull Optional<FieldMask> fieldMask() {
        return Optional.of(FieldMask.newBuilder()
          .addPaths("key.id")
          .addPaths("name")
          .addPaths("contact_info.email_address")
          .build());
      }
    });

    assertNotNull(personFuture, "should not get `null` future for retrieve operation");
    assertFalse(personFuture.isCancelled(), "future for retrieve should not start in cancelled state");

    var fetchedPersonOptional = personFuture.get(timeout(), timeoutUnit());
    assertNotNull(fetchedPersonOptional, "should not get `null` from future resolve");
    assertTrue(fetchedPersonOptional.isPresent(), "fetched person should be present");

    var fetchedPerson = fetchedPersonOptional.get();
    assertNotNull(fetchedPerson, "fetched person result should not be null");
    assertEquals("John Doe", fetchedPerson.getName(), "name should be present on masked person");
    assertEquals("john@doe.com", fetchedPerson.getContactInfo().getEmailAddress(),
      "email should be present on masked person");
    assertEquals("", fetchedPerson.getContactInfo().getPhoneE164(), "phone number should be empty");
    assertFalse(fetchedPerson.getContactInfo().hasField(ContactInfo.getDescriptor().findFieldByName("phone_e164")),
      "phone should not be present on masked person");

    var id = ModelMetadata.id(fetchedPerson);
    assertTrue(id.isPresent(), "ID should be decoded on fetched models");
  }

  /** Create a simple entity, store it, and then try to store it again. */
  protected void storeEntityCollission() throws TimeoutException, ExecutionException, InterruptedException {
    // persist the record
    Person person1 = Person.newBuilder()
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com")
        .setPhoneE164("+12345679001"))
      .build();

    Person person2 = Person.newBuilder()
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com")
        .setPhoneE164("+12345679001"))
      .build();

    ReactiveFuture<Person> op1 = acquire().persist(null, person1, WriteOptions.DEFAULTS);
    ReactiveFuture<Person> op2 = acquire().persist(null, person2, WriteOptions.DEFAULTS);
    assertFalse(op1.isCancelled(), "future from persist should not start in cancelled state");
    assertFalse(op2.isCancelled(), "future from persist should not start in cancelled state");

    Person model1 = op1.get(timeout(), timeoutUnit());
    Person model2 = op2.get(timeout(), timeoutUnit());
    Optional<PersonKey> key1Op = ModelMetadata.key(model1);
    Optional<PersonKey> key2Op = ModelMetadata.key(model2);
    assertTrue(key1Op.isPresent(), "key should be present after persist");
    assertTrue(key2Op.isPresent(), "key should be present after persist");
    var key1 = key1Op.get();
    var key2 = key2Op.get();
    var keySpliced = ModelMetadata.spliceKey(person1, Optional.of(key1));
    assertNotEquals(key1, key2, "keys for two written entities should not collide");
    touchedKeys.add(key1);
    touchedKeys.add(key2);

    assertTrue(op1.isDone(), "future should report as done after store operation finishes");
    assertNotNull(key1, "should get a key back from a persist operation");
    assertFalse(op1.isCancelled(), "write future should not present as cancelled after completing");
    assertTrue(op2.isDone(), "future should report as done after store operation finishes");
    assertNotNull(key2, "should get a key back from a persist operation");
    assertFalse(op2.isCancelled(), "write future should not present as cancelled after completing");

    // fetch the record
    ReactiveFuture<Optional<Message>> personFuture = acquire().retrieve(key1, FetchOptions.DEFAULTS);
    assertFalse(personFuture.isCancelled(), "future for retrieve should not start in cancelled state");
    Optional<Message> refetched = personFuture.get(timeout(), timeoutUnit());
    assertTrue(personFuture.isDone(), "future should present as done after a record is fetched");
    assertFalse(personFuture.isCancelled(), "read future should not present as cancelled after completing");
    assertNotNull(refetched, "should not get `null` for optional after record fetch");
    assertTrue(refetched.isPresent(), "should find record we just stored");
    assertEquals(keySpliced.toString(), refetched.get().toString(),
      "fetched person record should match identically, but with key");

    var overwrite = acquire().persist(key1, person1, new WriteOptions() {
      @Override
      public @Nonnull Optional<WriteDisposition> writeMode() {
        return Optional.of(WriteDisposition.MUST_NOT_EXIST);
      }
    });

    assertThrows(ExecutionException.class, () -> {
      overwrite.get(timeout(), timeoutUnit());
    });
  }

  /** Create a simple entity, store it, and then try to store it again. */
  protected void storeEntityUpdate() throws TimeoutException, ExecutionException, InterruptedException {
    // persist the record
    Person person = Person.newBuilder()
      .setKey(PersonKey.newBuilder()
          .setId("abc123update")
          .build())
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com")
        .setPhoneE164("+12345679001"))
      .build();

    ReactiveFuture<Person> op = acquire().persist(person.getKey(), person, WriteOptions.DEFAULTS);
    assertFalse(op.isCancelled(), "future from persist should not start in cancelled state");

    Person record = op.get(timeout(), timeoutUnit());
    Optional<PersonKey> key = ModelMetadata.key(record);
    assertTrue(key.isPresent(), "key should be present on written record");
    touchedKeys.add(key.get());

    assertNotNull(key, "should get a key back from a persist operation");
    assertTrue(op.isDone(), "future should report as done after store operation finishes");
    assertFalse(op.isCancelled(), "write future should not present as cancelled after completing");

    Person updated = person.toBuilder()
      .setName("Jane Doe")
      .build();

    ReactiveFuture<Person> op2 = acquire().persist(person.getKey(), updated, new WriteOptions() {
      @Override
      public @Nonnull Optional<WriteDisposition> writeMode() {
        return Optional.of(WriteDisposition.MUST_EXIST);
      }
    });

    assertFalse(op2.isCancelled(), "second write future should not present as cancelled after completing");
    Person record2 = op2.get(timeout(), timeoutUnit());
    Optional<PersonKey> key2 = ModelMetadata.key(record2);

    assertTrue(key2.isPresent(), "resulting key should be present after write");
    assertTrue(op2.isDone(), "future should report as done after store operation finishes");
    assertNotNull(key2, "should get a key back from a persist operation");
    assertFalse(op2.isCancelled(), "write future should not present as cancelled after completing");
    touchedKeys.add(key2.get());

    var id = ModelMetadata.id(record2);
    assertTrue(id.isPresent(), "ID should be decoded on fetched models");
  }

  /** Create a simple entity, store it, and then try to update an entity that does not exist. */
  protected void storeEntityUpdateNotFound() throws TimeoutException, ExecutionException, InterruptedException {
    // persist the record
    Person person = Person.newBuilder()
      .setKey(PersonKey.newBuilder()
          .setId("abc123update")
          .build())
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com")
        .setPhoneE164("+12345679001"))
      .build();

    ReactiveFuture<Person> op = acquire().persist(null, person, WriteOptions.DEFAULTS);
    assertFalse(op.isCancelled(), "future from persist should not start in cancelled state");

    Person record = op.get(timeout(), timeoutUnit());
    Optional<PersonKey> key = ModelMetadata.key(record);
    assertTrue(key.isPresent(), "key should be present on written record");
    touchedKeys.add(key.get());

    // generate a key that does not match
    var genkey = acquire().generateKey(emptyInstance);
    ReactiveFuture<Person> op2 = acquire().persist(genkey, person, new WriteOptions() {
      @Override
      public @Nonnull Optional<WriteDisposition> writeMode() {
        return Optional.of(WriteDisposition.MUST_EXIST);
      }
    });

    touchedKeys.add(genkey);
    assertFalse(op2.isCancelled(), "second write future should not present as cancelled after completing");
    assertThrows(ExecutionException.class, () -> {
      op2.get(timeout(), timeoutUnit());
    });
  }

  /** Create a simple entity, and then use the `update`-based interfaces to update it. */
  protected void createEntityThenUpdate() throws TimeoutException, ExecutionException, InterruptedException {
    // persist the record
    Person person = Person.newBuilder()
      .setKey(PersonKey.newBuilder()
          .setId("abc123createThenUpdate")
          .build())
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com")
        .setPhoneE164("+12345679001"))
      .build();

    // create it
    ReactiveFuture<Person> op = acquire().create(person, WriteOptions.DEFAULTS);
    Person record = op.get(timeout(), timeoutUnit());
    Optional<PersonKey> recordKey = ModelMetadata.key(record);
    assertTrue(recordKey.isPresent(), "record key should be present after create");
    var key = recordKey.get();
    touchedKeys.add(key);

    // update it
    Person changed = record.toBuilder()
      .setName("John J. Doe")
      .build();

    // setup a reusable test function
    BiConsumer<ReactiveFuture<Person>, Person> tester = (personFuture, expectedPerson) -> {
      assertNotNull(personFuture, "should never get `null` from `update`");
      assertFalse(personFuture.isCancelled(), "update future should not be initially cancelled");

      try {
        var updatedRecord = personFuture.get(timeout(), timeoutUnit());
        assertNotNull(personFuture, "should never get `null` from update future");
        assertEquals(expectedPerson.toString(), updatedRecord.toString(),
          "updated person should match expected result record");

      } catch (Exception exc) {
        throw new RuntimeException(exc);
      }
    };

    // try a each update interface
    ReactiveFuture<Person> updatedOp = acquire().update(changed);
    tester.accept(updatedOp, changed);
    ReactiveFuture<Person> updatedOp2 = acquire().update(key, changed);
    tester.accept(updatedOp2, changed);
    ReactiveFuture<Person> updatedOp3 = acquire().update(key, changed, UpdateOptions.DEFAULTS);
    tester.accept(updatedOp3, changed);

    // try each with a sub-object update
    Person changed2 = changed.toBuilder()
      .setContactInfo(changed.getContactInfo().toBuilder()
        .setEmailAddress("john2@doe.com"))
      .build();

    ReactiveFuture<Person> updatedOp4 = acquire().update(changed2);
    tester.accept(updatedOp4, changed2);
    ReactiveFuture<Person> updatedOp5 = acquire().update(key, changed2);
    tester.accept(updatedOp5, changed2);
    ReactiveFuture<Person> updatedOp6 = acquire().update(key, changed2, UpdateOptions.DEFAULTS);
    tester.accept(updatedOp6, changed2);

    // try again with a cleared field
    Person changed3 = changed2.toBuilder()
      .clearName()
      .build();

    ReactiveFuture<Person> updatedOp7 = acquire().update(changed3);
    tester.accept(updatedOp7, changed3);
    ReactiveFuture<Person> updatedOp8 = acquire().update(key, changed3);
    tester.accept(updatedOp8, changed3);
    ReactiveFuture<Person> updatedOp9 = acquire().update(key, changed3, UpdateOptions.DEFAULTS);
    tester.accept(updatedOp9, changed3);

    // restore the name, clear the contact info
    Person changed4 = changed3.toBuilder()
      .clearName()
      .build();

    ReactiveFuture<Person> updatedOp10 = acquire().update(changed4);
    tester.accept(updatedOp10, changed4);
    ReactiveFuture<Person> updatedOp11 = acquire().update(key, changed4);
    tester.accept(updatedOp11, changed4);
    ReactiveFuture<Person> updatedOp12 = acquire().update(key, changed4, UpdateOptions.DEFAULTS);
    tester.accept(updatedOp12, changed4);
  }

  /** Create a simple entity, then delete it, then try to re-fetch to make sure it was deleted. */
  protected void createEntityThenDelete() throws TimeoutException, ExecutionException, InterruptedException {
    // persist the record
    Person person = Person.newBuilder()
      .setKey(PersonKey.newBuilder()
          .setId("abc123createThenDelete")
          .build())
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com")
        .setPhoneE164("+12345679001"))
      .build();

    // create it
    ReactiveFuture<Person> op = acquire().create(person);
    Person record = op.get(timeout(), timeoutUnit());
    Optional<PersonKey> recordKey = ModelMetadata.key(record);
    assertTrue(recordKey.isPresent(), "record key should be present after create");
    var key = recordKey.get();
    touchedKeys.add(key);

    // fetch it, to make sure it's there
    ReactiveFuture<Optional<Person>> refetchedOp = acquire().fetchAsync(key);
    assertNotNull(refetchedOp, "should never get `null` from `fetchAsync`");
    assertFalse(refetchedOp.isCancelled(), "re-fetch future should not immediately be cancelled");
    Optional<Person> refetched = refetchedOp.get(timeout(), timeoutUnit());
    assertNotNull(refetched, "should never get `null` from `get`");
    assertTrue(refetched.isPresent(), "re-fetched record should be present");
    assertEquals(record.toString(), refetched.get().toString(),
      "re-fetched person record should be identical");

    // should fail because it has no key
    assertThrows(InvalidModelType.class, () -> {
      acquire().delete(person.toBuilder().clearKey().build());
    });

    // delete it, mercilessly
    ReactiveFuture<PersonKey> deleteOp = acquire().delete(key);
    assertNotNull(deleteOp, "should not get `null` from `delete`");
    assertFalse(deleteOp.isCancelled(), "delete future should not immediately be cancelled");
    PersonKey deletedKey = deleteOp.get(timeout(), timeoutUnit());
    assertTrue(deleteOp.isDone(), "delete op should complete after `get()`");
    assertNotNull(deletedKey, "should not get `null` from delete future result");
    assertEquals(key.toString(), deletedKey.toString(),
      "deleted key should be identical to key provided for delete");

    // try to fetch it, we shouldn't be able to find it
    ReactiveFuture<Optional<Person>> fetchAfterDelete = acquire().fetchAsync(deletedKey);
    assertNotNull(fetchAfterDelete, "should not get `null` from `fetchAsync`");
    assertFalse(fetchAfterDelete.isCancelled(), "fetch after delete should not be immediately cancelled");
    Optional<Person> refetchedAfterDelete = fetchAfterDelete.get(timeout(), timeoutUnit());
    assertNotNull(refetchedAfterDelete, "refetched-after-delete optional should never be `null`");
    assertFalse(refetchedAfterDelete.isPresent(),
      "refetched-after-delete optional should present as not-present");
  }

  /** Create a simple entity, then delete it, then try to re-fetch to make sure it was deleted. */
  protected void createEntityThenDeleteByRecord() throws TimeoutException, ExecutionException, InterruptedException {
    // persist the record
    final Person person = Person.newBuilder()
      .setKey(PersonKey.newBuilder()
          .setId("abc123createThenDeleteByRecord")
          .build())
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com")
        .setPhoneE164("+12345679001"))
      .build();

    // create it
    ReactiveFuture<Person> op = acquire().create(person);
    Person record = op.get(timeout(), timeoutUnit());
    Optional<PersonKey> recordKey = ModelMetadata.key(record);
    assertTrue(recordKey.isPresent(), "record key should be present after create");
    var key = recordKey.get();
    touchedKeys.add(key);

    // fetch it, to make sure it's there
    ReactiveFuture<Optional<Person>> refetchedOp = acquire().fetchAsync(key);
    assertNotNull(refetchedOp, "should never get `null` from `fetchAsync`");
    assertFalse(refetchedOp.isCancelled(), "re-fetch future should not immediately be cancelled");
    Optional<Person> refetched = refetchedOp.get(timeout(), timeoutUnit());
    assertNotNull(refetched, "should never get `null` from `get`");
    assertTrue(refetched.isPresent(), "re-fetched record should be present");
    assertEquals(record.toString(), refetched.get().toString(),
      "re-fetched person record should be identical");

    // should fail because it has no key
    assertThrows(InvalidModelType.class, () -> {
      acquire().delete(person.toBuilder().clearKey().build());
    });

    // delete it, mercilessly
    ReactiveFuture<PersonKey> deleteOp = acquire().deleteRecord(refetched.get());
    assertNotNull(deleteOp, "should not get `null` from `delete`");
    assertFalse(deleteOp.isCancelled(), "delete future should not immediately be cancelled");
    PersonKey deletedKey = deleteOp.get(timeout(), timeoutUnit());
    assertTrue(deleteOp.isDone(), "delete op should complete after `get()`");
    assertNotNull(deletedKey, "should not get `null` from delete future result");
    assertEquals(key.toString(), deletedKey.toString(),
      "deleted key should be identical to key provided for delete");

    // try to fetch it, we shouldn't be able to find it
    ReactiveFuture<Optional<Person>> fetchAfterDelete = acquire().fetchAsync(deletedKey);
    assertNotNull(fetchAfterDelete, "should not get `null` from `fetchAsync`");
    assertFalse(fetchAfterDelete.isCancelled(), "fetch after delete should not be immediately cancelled");
    Optional<Person> refetchedAfterDelete = fetchAfterDelete.get(timeout(), timeoutUnit());
    assertNotNull(refetchedAfterDelete, "refetched-after-delete optional should never be `null`");
    assertFalse(refetchedAfterDelete.isPresent(),
      "refetched-after-delete optional should present as not-present");
  }

  /** Test that invalid/incompatible options are disallowed properly. */
  protected void createUpdateWithInvalidOptions() {
    // persist the record
    final Person person = Person.newBuilder()
      .setKey(PersonKey.newBuilder().setId("abc123test-invalidOptions").build())
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com")
        .setPhoneE164("+12345679001"))
      .build();

    assertThrows(IllegalArgumentException.class, () -> {
      acquire().create(person, new WriteOptions() {
        @Nonnull
        @Override
        public Optional<WriteDisposition> writeMode() {
          return Optional.of(WriteDisposition.MUST_EXIST);
        }
      });
    });

    assertThrows(IllegalArgumentException.class, () -> {
      acquire().update(person, new UpdateOptions() {
        @Nonnull
        @Override
        public Optional<WriteDisposition> writeMode() {
          return Optional.of(WriteDisposition.MUST_NOT_EXIST);
        }
      });
    });
  }
}
