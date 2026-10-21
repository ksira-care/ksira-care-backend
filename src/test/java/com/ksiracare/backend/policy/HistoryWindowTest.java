package com.ksiracare.backend.policy;

import com.ksiracare.backend.config.AdminProperties;
import com.ksiracare.backend.exception.HistoryMonthNotAvailableException;
import com.ksiracare.backend.support.TestClocks;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HistoryWindowTest {

    /** 00:30 IST on 1 Oct is still 30 Sep in UTC — the window must follow IST. */
    private final HistoryWindow window = new HistoryWindow(
            TestClocks.calendarAt("2026-10-01T00:30"), new AdminProperties(Duration.ofHours(24), 2));

    @Test
    void coversThisMonthAndLastInIst() {
        assertThat(window.months()).containsExactly(YearMonth.of(2026, 10), YearMonth.of(2026, 9));
        assertThat(window.oldestMonth()).isEqualTo(YearMonth.of(2026, 9));
    }

    @Test
    void exportsLastMonth() {
        assertThat(window.exportMonth()).isEqualTo(YearMonth.of(2026, 9));
    }

    @Test
    void refusesMonthsOutsideTheWindow() {
        assertThatCode(() -> window.checkShown(YearMonth.of(2026, 9))).doesNotThrowAnyException();
        assertThatThrownBy(() -> window.checkShown(YearMonth.of(2026, 8))).isInstanceOf(HistoryMonthNotAvailableException.class);
        assertThatThrownBy(() -> window.checkShown(YearMonth.of(2026, 11))).isInstanceOf(HistoryMonthNotAvailableException.class);
    }
}
