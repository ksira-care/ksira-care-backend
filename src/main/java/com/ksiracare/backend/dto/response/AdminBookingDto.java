package com.ksiracare.backend.dto.response;

import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.enums.Language;

import java.util.Set;
import java.util.UUID;

/**
 * One row of the admin console's day view. Times are epoch milliseconds (UTC).
 *
 * @param clientLanguages what the client speaks (ISO 639-1); only aaptas sharing one can take it
 * @param aapta         who holds the session, or null if it still needs allocating
 * @param previousAapta who held it before the latest reallocation ("Was Aanya M."), or null
 * @param bookingStatus the outcome, as marked by the aapta in their portal; PENDING until then
 * @param markedAt      when the aapta marked it, or null
 * @param editable      whether the admin may change the aapta and BCC flag right now
 * @param editableFrom  when it becomes editable (24 h before the start); it stops at {@code startTime}
 * @param cancellable   whether the admin may cancel it now (any time while the aapta hasn't marked it)
 * @param cancelReason  why an admin cancelled it, or null
 * @param version       send back unchanged when saving, so concurrent edits are detected
 */
public record AdminBookingDto(
        UUID bookingId,
        Long startTime,
        Long endTime,
        ClientDto client,
        Set<Language> clientLanguages,
        AaptaRefDto aapta,
        AaptaRefDto previousAapta,
        boolean bccEmailSent,
        BookingStatus bookingStatus,
        Long markedAt,
        boolean editable,
        Long editableFrom,
        boolean cancellable,
        String cancelReason,
        Long version
) {
}
