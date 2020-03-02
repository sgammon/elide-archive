package gust.backend.model;

import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import gust.backend.runtime.ReactiveFuture;
import gust.backend.model.PersonRecord.Person;
import gust.backend.model.PersonRecord.PersonKey;
import gust.backend.model.PersonRecord.ContactInfo;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.DynamicTest;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;


/** Abstract test factory, which is responsible for testing/enforcing {@link PersistenceDriver} surfaces. */
@SuppressWarnings({"WeakerAccess", "DuplicatedCode", "CodeBlock2Expr", "unchecked"})
public abstract class GenericPersistenceDriverTest<Driver extends PersistenceDriver> {
  /** Describes data keys touched in this test case. */
  protected final HashSet<Object> touchedKeys = new HashSet<>();

  /** Empty person instance, for testing. */
  private final static Person emptyInstance = Person.getDefaultInstance();

  @TestFactory
  protected final Iterable<DynamicTest> driverTests() {
    final String subcase = this.getClass().getSimpleName();
    List<DynamicTest> tests = Arrays.asList(
      dynamicTest(format("%s: `acquireDriver`", subcase), this::acquireDriver),
      dynamicTest(format("%s: `testDriverCodec`", subcase), this::testDriverCodec),
      dynamicTest(format("%s: `testDriverExecutor`", subcase), this::testDriverExecutor),
      dynamicTest(format("%s: `testGenerateKey`", subcase), this::testGenerateKey),
      dynamicTest(format("%s: `fetchNonExistentEntity`", subcase), this::fetchNonExistentEntity),
      dynamicTest(format("%s: `storeAndFetchEntity`", subcase), this::storeAndFetchEntity),
      dynamicTest(format("%s: `storeAndFetchEntityMasked`", subcase), this::storeAndFetchEntityMasked),
      dynamicTest(format("%s: `storeEntityCollission`", subcase), this::storeEntityCollission),
      dynamicTest(format("%s: `storeEntityUpdate`", subcase), this::storeEntityUpdate),
      dynamicTest(format("%s: `storeEntityUpdateNotFound`", subcase), this::storeEntityUpdateNotFound)
    );

    tests.addAll(subclassTests().orElse(Collections.emptyList()));
    return tests;
  }

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
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com")
        .setPhoneE164("+12345679001"))
      .build();

    ReactiveFuture<Person> op = acquire().persist(null, person, WriteOptions.DEFAULTS);
    assertFalse(op.isCancelled(), "future from persist should not start in cancelled state");

    Person model = op.get(timeout(), timeoutUnit());
    assertTrue(op.isDone(), "future should report as done after store operation finishes");
    assertNotNull(model, "should get a model back from a persist operation");
    assertFalse(op.isCancelled(), "write future should not present as cancelled after completing");
    Optional<PersonKey> key = ModelMetadata.key(model);
    assertTrue(key.isPresent(), "key should be present on model after storing");
    touchedKeys.add(key);

    // fetch the record
    ReactiveFuture<Optional<Person>> personFuture = acquire().retrieve(key.get(), FetchOptions.DEFAULTS);
    assertNotNull(personFuture, "should not get `null` future for retrieve operation");
    assertFalse(personFuture.isCancelled(), "future for retrieve should not start in cancelled state");

    Optional<Person> refetched = personFuture.get(timeout(), timeoutUnit());
    assertTrue(personFuture.isDone(), "future should present as done after a record is fetched");
    assertFalse(personFuture.isCancelled(), "read future should not present as cancelled after completing");
    assertNotNull(refetched, "should not get `null` for optional after record fetch");
    assertTrue(refetched.isPresent(), "should find record we just stored");
    assertEquals(person.toString(), refetched.get().toString(),
      "fetched person record should match identically");

    // fetch the record a second time
    ReactiveFuture<Optional<Person>> personFuture2 = acquire().retrieve(key.get(), FetchOptions.DEFAULTS);
    assertNotNull(personFuture2, "should not get `null` future for retrieve operation");
    assertFalse(personFuture2.isCancelled(), "future for retrieve should not start in cancelled state");

    Optional<Person> refetched2 = personFuture2.get(timeout(), timeoutUnit());
    assertTrue(personFuture2.isDone(), "future should present as done after a record is fetched");
    assertFalse(personFuture2.isCancelled(), "read future should not present as cancelled after completing");
    assertNotNull(refetched2, "should not get `null` for optional after record fetch");
    assertTrue(refetched2.isPresent(), "should find record we just stored");
    assertEquals(person.toString(), refetched2.get().toString(),
      "fetched person record should match identically");
  }

  /** Create a simple entity, store it, and then fetch it, but with a field mask. */
  protected void storeAndFetchEntityMasked() throws TimeoutException, ExecutionException, InterruptedException {
    // persist the record
    Person person = Person.newBuilder()
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com")
        .setPhoneE164("+12345679001"))
      .build();

    ReactiveFuture<Person> op = acquire().persist(null, person, WriteOptions.DEFAULTS);
    assertFalse(op.isCancelled(), "future from persist should not start in cancelled state");

    Person model = op.get(timeout(), timeoutUnit());
    assertTrue(op.isDone(), "future should report as done after store operation finishes");
    assertNotNull(model, "should get a model back from a persist operation");
    assertFalse(op.isCancelled(), "write future should not present as cancelled after completing");
    Optional<PersonKey> key = ModelMetadata.key(model);
    assertTrue(key.isPresent(), "key should be present on model after storing");
    touchedKeys.add(key);

    // fetch the record
    ReactiveFuture<Optional<Person>> personFuture = acquire().retrieve(key.get(), new FetchOptions() {
      @Override
      public @Nonnull Optional<FieldMask> fieldMask() {
        return Optional.of(FieldMask.newBuilder()
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

    var key1 = op1.get(timeout(), timeoutUnit()).getKey();
    var key2 = op2.get(timeout(), timeoutUnit()).getKey();
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
    assertEquals(person1.toString(), refetched.get().toString(),
      "fetched person record should match identically");

    var overwrite = acquire().persist(key1, person1, new WriteOptions() {
      @Override
      public @Nonnull WriteDisposition writeMode() {
        return WriteDisposition.MUST_NOT_EXIST;
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
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com")
        .setPhoneE164("+12345679001"))
      .build();

    var op = acquire().persist(null, person, WriteOptions.DEFAULTS);
    assertFalse(op.isCancelled(), "future from persist should not start in cancelled state");

    Object key = op.get(timeout(), timeoutUnit());
    touchedKeys.add(key);

    assertNotNull(key, "should get a key back from a persist operation");
    assertTrue(op.isDone(), "future should report as done after store operation finishes");
    assertFalse(op.isCancelled(), "write future should not present as cancelled after completing");

    Person updated = person.toBuilder()
      .setName("Jane Doe")
      .build();

    var op2 = acquire().persist(null, updated, new WriteOptions() {
      @Override
      public @Nonnull WriteDisposition writeMode() {
        return WriteDisposition.MUST_EXIST;
      }
    });

    assertFalse(op2.isCancelled(), "second write future should not present as cancelled after completing");
    Object key2 = op2.get(timeout(), timeoutUnit());

    assertTrue(op2.isDone(), "future should report as done after store operation finishes");
    assertNotNull(key2, "should get a key back from a persist operation");
    assertFalse(op2.isCancelled(), "write future should not present as cancelled after completing");
    touchedKeys.add(key2);
  }

  /** Create a simple entity, store it, and then try to update an entity that does not exist. */
  protected void storeEntityUpdateNotFound() throws TimeoutException, ExecutionException, InterruptedException {
    // persist the record
    Person person = Person.newBuilder()
      .setName("John Doe")
      .setContactInfo(ContactInfo.newBuilder()
        .setEmailAddress("john@doe.com")
        .setPhoneE164("+12345679001"))
      .build();

    ReactiveFuture<?> op = acquire().persist(null, person, WriteOptions.DEFAULTS);
    assertFalse(op.isCancelled(), "future from persist should not start in cancelled state");

    Object key = op.get(timeout(), timeoutUnit());
    touchedKeys.add(key);

    // generate a key that does not match
    var genkey = acquire().generateKey(emptyInstance);
    var op2 = acquire().persist(genkey, person, new WriteOptions() {
      @Override
      public @Nonnull WriteDisposition writeMode() {
        return WriteDisposition.MUST_EXIST;
      }
    });

    touchedKeys.add(genkey);
    assertFalse(op2.isCancelled(), "second write future should not present as cancelled after completing");
    assertThrows(ExecutionException.class, () -> {
      op2.get(timeout(), timeoutUnit());
    });
  }
}
