package com.ksiracare.backend.time;

import com.ksiracare.backend.exception.InvalidRangeException;
import com.ksiracare.backend.support.TestClocks;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.ksiracare.backend.support.TestClocks.utc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueryRangesTest {

    private final QueryRanges ranges = new QueryRanges(TestClocks.PORTAL);
    private final long oct1 = EpochTime.toMillis(utc("2026-10-01T00:00"));

    @Test
    void convertsEpochMillisToAUtcRange() {
        long nov1 = EpochTime.toMillis(utc("2026-11-01T00:00"));

        assertThat(ranges.of(oct1, nov1)).isEqualTo(new UtcRange(utc("2026-10-01T00:00"), utc("2026-11-01T00:00")));
    }

    @Test
    void rejectsEmptyOrReversedRanges() {
        assertThatThrownBy(() -> ranges.of(oct1, oct1)).isInstanceOf(InvalidRangeException.class);
        assertThatThrownBy(() -> ranges.of(oct1, oct1 - 1)).isInstanceOf(InvalidRangeException.class);
    }

    @Test
    void rejectsRangesLongerThanTheConfiguredMaximum() {
        long maxEnd = oct1 + Duration.ofDays(92).toMillis();

        assertThat(ranges.of(oct1, maxEnd)).isNotNull();
        assertThatThrownBy(() -> ranges.of(oct1, maxEnd + 1))
                .isInstanceOf(InvalidRangeException.class)
                .hasMessageContaining("92 days");
    }
}
