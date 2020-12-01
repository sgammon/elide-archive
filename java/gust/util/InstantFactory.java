package gust.util;


import com.google.protobuf.Timestamp;

import javax.annotation.Nonnull;
import java.time.Instant;


/**
 * Utilities to convert between different temporal instant records.
 */
public final class InstantFactory {
  /**
   * Convert a Protocol Buffers {@link Timestamp} record to a Java {@link Instant} record.
   *
   * @param subject Subject timestamp to convert.
   * @return Converted Java Instant.
   */
  public static @Nonnull Instant instant(@Nonnull Timestamp subject) {
    return Instant.ofEpochSecond(subject.getSeconds(), subject.getNanos() > 0 ? (long)subject.getNanos() : 0);
  }
}
