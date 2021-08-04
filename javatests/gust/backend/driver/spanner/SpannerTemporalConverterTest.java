package gust.backend.driver.spanner;

import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static org.junit.jupiter.api.Assertions.*;


/** Tests for the {@link SpannerTemporalConverter}. */
public final class SpannerTemporalConverterTest {
    private final Instant now = Instant.now();

    // Well-formed PB timestamp.
    private final com.google.protobuf.Timestamp pbTimestamp = com.google.protobuf.Timestamp.newBuilder()
            .setSeconds(now.getEpochSecond())
            .setNanos(now.getNano())
            .build();

    // Well-formed cloud timestamp.
    private final Timestamp cloudTimestamp = Timestamp.fromProto(pbTimestamp);

    // Well-formed PB date.
    private final com.google.type.Date pbDate = com.google.type.Date.newBuilder()
            .setDay(29)
            .setMonth(7)
            .setYear(2021)
            .build();

    // Well-formed cloud date.
    private final Date cloudDate = Date.fromYearMonthDay(2021, 7, 29);

    @Test public void testConvertTimestampToCloud() {
        assertThat(pbTimestamp).hasAllRequiredFields();
        assertThat(SpannerTemporalConverter.cloudTimestampFromProto(pbTimestamp).toProto())
                .isEqualTo(cloudTimestamp.toProto());
    }

    @Test public void testConvertCloudToTimestamp() {
        assertThat(cloudTimestamp.toProto()).hasAllRequiredFields();
        assertThat(SpannerTemporalConverter.protoTimestampFromCloud(cloudTimestamp))
                .isEqualTo(pbTimestamp);
    }

    @Test public void testConvertDateToCloud() {
        assertThat(pbDate).hasAllRequiredFields();

        var cloudDate = SpannerTemporalConverter.cloudDateFromProto(pbDate);
        assertNotNull(cloudDate, "should not get `null` when converting to cloud date from proto");
        assertEquals(pbDate.getYear(), cloudDate.getYear(), "year value should copy properly");
        assertEquals(pbDate.getMonth(), cloudDate.getMonth(), "month value should copy properly");
        assertEquals(pbDate.getDay(), cloudDate.getDayOfMonth(), "day value should copy properly");
    }

    @Test public void testConvertCloudToDate() {
        var convertedPbDate = SpannerTemporalConverter.protoDateFromCloud(cloudDate);
        assertNotNull(convertedPbDate, "should not get `null` when converting to PB date from cloud structure");
        assertThat(convertedPbDate).isEqualTo(pbDate);
    }
}
