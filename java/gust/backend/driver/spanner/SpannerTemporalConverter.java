package gust.backend.driver.spanner;

import com.google.cloud.Date;
import com.google.cloud.Timestamp;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;


/**
 * Serialize back and forth between Google Cloud / Protocol Buffer standard TIMESTAMP and DATE records.
 */
@ThreadSafe
public final class SpannerTemporalConverter {
    private SpannerTemporalConverter() { /* Disallow construction. */ }

    /**
     * Convert a regular/standard Protocol Buffers timestamp into a Google Cloud timestamp without loss of resolution.
     *
     * @param timestamp Standard timestamp to convert.
     * @return Cloud timestamp value.
     */
    public static @Nonnull Timestamp cloudTimestampFromProto(com.google.protobuf.Timestamp timestamp) {
        return Timestamp.fromProto(timestamp);
    }

    /**
     * Convert a Google Cloud timestamp into a standard Protocol Buffers timestamp without loss of resolution.
     *
     * @param timestamp Cloud timestamp to convert.
     * @return Protocol buffers timestamp value.
     */
    public static @Nonnull com.google.protobuf.Timestamp protoTimestampFromCloud(Timestamp timestamp) {
        return com.google.protobuf.Timestamp.newBuilder()
            .setSeconds(timestamp.getSeconds())
            .setNanos(timestamp.getNanos())
            .build();
    }

    /**
     * Convert a regular/standard Protocol Buffers date into a Google Cloud date without loss of resolution.
     *
     * @param date Standard date to convert.
     * @return Cloud date value.
     */
    public static @Nonnull Date cloudDateFromProto(com.google.type.Date date) {
        return Date.fromYearMonthDay(
            date.getYear(),
            date.getMonth(),
            date.getDay()
        );
    }

    /**
     * Convert a Google Cloud date into a standard Protocol Buffers date without loss of resolution.
     *
     * @param date Cloud date to convert.
     * @return Protocol buffers date value.
     */
    public static @Nonnull com.google.type.Date protoDateFromCloud(Date date) {
        return com.google.type.Date.newBuilder()
            .setYear(date.getYear())
            .setMonth(date.getMonth())
            .setDay(date.getDayOfMonth())
            .build();
    }
}
