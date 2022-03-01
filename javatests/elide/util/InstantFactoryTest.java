package elide.util;

import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/** Test the hex-encode utility class. */
public final class InstantFactoryTest {
  @Test void testInstantFactory() {
    Instant now = Instant.now();
    Instant inflated = InstantFactory.instant(Timestamp.newBuilder()
            .setSeconds(now.getEpochSecond())
            .build());

    assertEquals(
        now.getEpochSecond(),
        inflated.getEpochSecond(),
        "inflated instant from factory (via proto) should match at seconds resolution"
    );
  }
}
