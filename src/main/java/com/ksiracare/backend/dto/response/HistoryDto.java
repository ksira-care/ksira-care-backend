package com.ksiracare.backend.dto.response;

import com.ksiracare.backend.enums.BookingStatus;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * One month of History: every booking, whatever its status, newest first.
 *
 * @param month    the month shown, e.g. "2026-10"
 * @param months   the months History offers (the tabs), newest first
 * @param summary  counts for the tiles
 * @param sessions the bookings
 * @param olderMonthsNotExported months older than History's window that are still kept because
 *                               nobody exported them (a warning to show); usually empty
 */
public record HistoryDto(YearMonth month, List<YearMonth> months, Summary summary, List<Row> sessions,
                         List<YearMonth> olderMonthsNotExported) {

    /**
     * @param refunded aapta no-shows: the client gets their money back
     * @param pending  not marked yet (or still to happen)
     */
    public record Summary(int total, int completed, int clientNoShow, int refunded, int cancelled, int pending) {
    }

    /**
     * One booking. Times are epoch milliseconds (UTC).
     *
     * @param aapta    who held it, or null if it was never allocated
     * @param notes    what the client hoped for ("What they expected")
     * @param refunded whether the client is refunded (the aapta didn't show)
     */
    public record Row(
            UUID bookingId,
            Long startTime,
            Long endTime,
            ClientDto client,
            AaptaRefDto aapta,
            BookingStatus bookingStatus,
            boolean refunded,
            String notes
    ) {
    }
}
