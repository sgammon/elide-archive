package gust.backend.driver.spanner;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.grpc.GrpcTransportOptions;
import com.google.cloud.spanner.*;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import gust.backend.model.GenericPersistenceAdapterTest;
import gust.backend.model.PersistenceOperationFailed;
import gust.backend.model.PersonRecord;
import gust.backend.runtime.Logging;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.testcontainers.containers.*;
import org.testcontainers.containers.SpannerEmulatorContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


/** Tests for the {@link SpannerAdapter}. */
@Testcontainers
@SuppressWarnings("UnstableApiUsage")
public final class SpannerAdapterTest extends GenericPersistenceAdapterTest<
        SpannerAdapter<PersonRecord.PersonKey, PersonRecord.Person>> {
    private static final Logger logging = Logging.logger(SpannerAdapterTest.class);
    private static final String spannerVersion = System.getProperty("e2e.spannerVersion", "1.2.0");
    private static ListeningScheduledExecutorService executorService;
    private static SpannerAdapter<PersonRecord.PersonKey, PersonRecord.Person> personAdapter;
    private static SpannerAdapter<PersonRecord.TypeBuffet.SampleKey, PersonRecord.TypeBuffet> sampleAdapter;

    private static final String SPANNER_MODE = System.getProperty("e2e.spannerMode", "EMULATOR");
    private static final String PROJECT_ID = System.getProperty("e2e.spannerProject", "elide-ai");
    private static final String INSTANCE_ID = System.getProperty("e2e.spannerInstance", "testing");
    private static final String DATABASE_ID = System.getProperty("e2e.spannerDatabase", "testdb");

    private static final DatabaseId database = DatabaseId.of(
        PROJECT_ID,
        INSTANCE_ID,
        DATABASE_ID
    );

    Network network = Network.newNetwork();

    @Container
    public SpannerEmulatorContainer spanner = new SpannerEmulatorContainer(
        DockerImageName.parse("gcr.io/cloud-spanner-emulator/emulator:" + spannerVersion)
    ).withNetwork(network).withNetworkAliases("spanner");

    @BeforeEach
    void initSpannerTests() throws InterruptedException, ExecutionException {
        logging.info("Initializing executor service for Spanner tests...");
        executorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());

        TransportChannelProvider channelProvider;
        SpannerOptions.Builder optionsBuilder;
        SpannerOptions options;
        Spanner client;

        // step one: setup all the connections and access we need, based on whether the simulator is active or not.
        if ("EMULATOR".equals(SPANNER_MODE)) {
            logging.info("Running in EMULATOR mode. Setting up Spanner emulator...");

            optionsBuilder = SpannerOptions.newBuilder()
                    .setEmulatorHost(spanner.getEmulatorGrpcEndpoint())
                    .setCredentials(NoCredentials.getInstance())
                    .setProjectId(PROJECT_ID);

            ManagedChannel channel = ManagedChannelBuilder.forTarget(spanner.getEmulatorGrpcEndpoint())
                    .usePlaintext()
                    .build();

            channelProvider = FixedTransportChannelProvider.create(
                    GrpcTransportChannel.create(channel)
            );
            optionsBuilder.setChannelProvider(channelProvider);

            optionsBuilder.getSpannerStubSettingsBuilder()
                    .setTransportChannelProvider(channelProvider)
                    .setCredentialsProvider(NoCredentialsProvider.create());

            optionsBuilder.getDatabaseAdminStubSettingsBuilder()
                    .setTransportChannelProvider(channelProvider)
                    .setCredentialsProvider(NoCredentialsProvider.create());

            optionsBuilder.getInstanceAdminStubSettingsBuilder()
                    .setTransportChannelProvider(channelProvider)
                    .setCredentialsProvider(NoCredentialsProvider.create());

            options = optionsBuilder.build();
            client = options.getService();

            // step one: initialize a new emulated instance
            logging.info("Standing up emulated Spanner instance...");
            InstanceConfigId instanceConfig = InstanceConfigId.of(PROJECT_ID, "emulator-config");
            InstanceId instanceId = InstanceId.of(PROJECT_ID, INSTANCE_ID);
            InstanceAdminClient insAdminClient = client.getInstanceAdminClient();
            Instance instance = insAdminClient.createInstance(
                    InstanceInfo.newBuilder(instanceId)
                            .setNodeCount(1)
                            .setDisplayName("Test instance")
                            .setInstanceConfigId(instanceConfig)
                            .build()
            ).get();

            logging.info("Verifying new instance...");
            var newInstance = insAdminClient.listInstances().getValues().iterator().next();
            assertNotNull(newInstance, "new instance should not be null");
            assertEquals(INSTANCE_ID, newInstance.getId().getInstance(),
                    "instance ID should be expected value");
            assertEquals(InstanceInfo.State.READY, newInstance.getState(),
                    "new instance should be ready immediately");
            logging.info("New instance is READY: \n{}", newInstance.toString());

            var peopleTableDdlStatement = (
                "CREATE TABLE People (" +
                    "Key STRING(64), " +
                    "Name STRING(64), " +
                    "ContactInfo STRING(1024)" +
                ") PRIMARY KEY (Key)"
            );

            // step two: initialize a new database in the instance
            logging.info("Creating emulated test database with DDL statement: \n{}", peopleTableDdlStatement);
            DatabaseAdminClient dbAdminClient = client.getDatabaseAdminClient();
            dbAdminClient.createDatabase(
                    INSTANCE_ID,
                    DATABASE_ID,
                    Collections.singletonList(peopleTableDdlStatement)
            ).get();

            logging.info("Verifying new database...");
            var newDatabase = instance.getDatabase(DATABASE_ID);
            assertNotNull(newDatabase, "new database should not be null");
            assertEquals(DATABASE_ID, newDatabase.getId().getDatabase(), "database ID should be expected value");
            logging.info("New database is READY: \n{}", newDatabase.toString());

            logging.info("Emulator ready. Setting up Spanner adapter...");
            personAdapter = SpannerAdapter.acquire(
                options.toBuilder(),
                database,
                channelProvider,
                Optional.of(NoCredentialsProvider.create()),
                Optional.empty(),
                GrpcTransportOptions.newBuilder().build(),
                executorService,
                PersonRecord.PersonKey.getDefaultInstance(),
                PersonRecord.Person.getDefaultInstance(),
                SpannerDriverSettings.DEFAULTS,
                Optional.empty()
            );

            sampleAdapter = SpannerAdapter.acquire(
                options.toBuilder(),
                database,
                channelProvider,
                Optional.of(NoCredentialsProvider.create()),
                Optional.empty(),
                GrpcTransportOptions.newBuilder().build(),
                executorService,
                PersonRecord.TypeBuffet.SampleKey.getDefaultInstance(),
                PersonRecord.TypeBuffet.getDefaultInstance(),
                SpannerDriverSettings.DEFAULTS,
                Optional.empty()
            );
        } else if ("LIVE".equals(SPANNER_MODE)) {
            logging.info("Running in LIVE mode. Setting up Spanner emulator...");

            optionsBuilder = SpannerOptions.newBuilder()
                    .setProjectId(PROJECT_ID);

            // setup production adapter
            personAdapter = SpannerAdapter.acquire(
                PersonRecord.PersonKey.getDefaultInstance(),
                PersonRecord.Person.getDefaultInstance(),
                database,
                optionsBuilder,
                executorService
            );

            sampleAdapter = SpannerAdapter.acquire(
                PersonRecord.TypeBuffet.SampleKey.getDefaultInstance(),
                PersonRecord.TypeBuffet.getDefaultInstance(),
                database,
                optionsBuilder,
                executorService
            );

        } else {
            throw new IllegalArgumentException("Unrecognized Spanner test mode: '" + SPANNER_MODE + "'.");
        }
    }

    @AfterAll
    @SuppressWarnings("ResultOfMethodCallIgnored")
    static void shutdownExecutor() throws InterruptedException {
        executorService.shutdownNow();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
        executorService = null;
        personAdapter = null;
    }

    // -- Driver Tests: Overrides -- //

    /** {@inheritDoc} */
    @Override
    protected @Nonnull SpannerAdapter<PersonRecord.PersonKey, PersonRecord.Person> adapter() {
        return personAdapter;
    }

    /** {@inheritDoc} */
    @Override
    protected void acquireDriver() {
        SpannerAdapter<PersonRecord.PersonKey, PersonRecord.Person> personAdapter = SpannerAdapter.acquire(
                PersonRecord.PersonKey.getDefaultInstance(),
                PersonRecord.Person.getDefaultInstance(),
                database,
                executorService);
        assertNotNull(personAdapter, "should not get `null` for adapter acquire");
    }

    /** {@inheritDoc} */
    @Override
    protected @Nonnull Optional<List<String>> unsupportedDriverTests() {
        return Optional.of(Arrays.asList(
            "storeAndFetchEntityMasked",
            "storeEntityUpdateNotFound",
            "storeEntityCollission"
        ));
    }

    // -- Concrete Tests -- //

    @SuppressWarnings("ConstantConditions")
    @Test public void testNullchecks() {
        assertNotNull(personAdapter, "should not get `null` for adapter acquire");
        assertThrows(PersistenceOperationFailed.class, () -> personAdapter.fetch(null));
        assertThrows(NullPointerException.class, () -> personAdapter.retrieve(null, null));
        assertThrows(NullPointerException.class, () -> personAdapter.retrieve(
                PersonRecord.PersonKey.newBuilder().setId("test").build(), null));
        assertThrows(NullPointerException.class, () -> personAdapter.create(null));
        assertThrows(NullPointerException.class, () -> personAdapter.persist(null, null, null));
        assertThrows(NullPointerException.class, () -> personAdapter.persist(null,
                PersonRecord.Person.newBuilder().build(), null));
        assertThrows(NullPointerException.class, () -> personAdapter.delete(null));
        assertThrows(NullPointerException.class, () -> personAdapter.delete(null, null));
    }

    @Test public void testMustDeclareTableName() {
        assertNotNull(personAdapter, "should not get `null` for adapter acquire");
        assertThrows(IllegalArgumentException.class, () -> sampleAdapter.retrieve(
            PersonRecord.TypeBuffet.SampleKey.newBuilder()
                .setId(123L)
                .build(),
            SpannerDriver.SpannerFetchOptions.DEFAULTS
        ));
    }
}
