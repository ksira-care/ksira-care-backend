package com.ksiracare.backend.service;

import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.entity.BookingEvent;
import com.ksiracare.backend.enums.Actor;
import com.ksiracare.backend.enums.BookingEventType;
import com.ksiracare.backend.repository.BookingEventRepository;
import com.ksiracare.backend.security.SessionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** A booking's history ({@code booking_events}): what changed, who did it, when — written and read here. */
@Component
@RequiredArgsConstructor
public class BookingHistory {

    /** What lists of bookings show from their history. */
    private static final Set<BookingEventType> SUMMARISED =
            EnumSet.of(BookingEventType.AAPTA_REALLOCATED, BookingEventType.AAPTA_UNALLOCATED, BookingEventType.BOOKING_CANCELLED);

    private final BookingEventRepository eventRepository;

    /**
     * The parts of a booking's history that lists show.
     *
     * @param previousAapta who held it before it was last moved or unallocated ("Was Aanya M.")
     * @param cancelReason  why it was cancelled
     */
    public record Summary(Aapta previousAapta, String cancelReason) {
        public static final Summary NONE = new Summary(null, null);
    }

    /**
     * Records a change made by a signed-in aapta or admin.
     *
     * @param previousAapta for allocation changes, who held the booking before (else null)
     * @param aapta         who holds it after the change (else null)
     * @param note          why, where the change needs explaining (else null)
     */
    public void record(Booking booking, BookingEventType type, SessionPrincipal actor,
                       Aapta previousAapta, Aapta aapta, LocalDateTime at, String note) {
        save(booking, type, Actor.valueOf(actor.role().name()), actor.accountId(), previousAapta, aapta, at, note);
    }

    /** Records a change the system made on its own, e.g. at a deadline. */
    public void recordBySystem(Booking booking, BookingEventType type, LocalDateTime at, String note) {
        save(booking, type, Actor.SYSTEM, null, null, booking.getAapta(), at, note);
    }

    /** Booking id → its summary, from one query. Bookings with nothing to show map to {@link Summary#NONE}. */
    public Map<UUID, Summary> summarise(Collection<UUID> bookingIds) {
        Map<UUID, Summary> summaries = new HashMap<>();
        bookingIds.forEach(id -> summaries.put(id, Summary.NONE));
        if (bookingIds.isEmpty()) {
            return summaries;
        }
        // Newest first, so the first event of each type seen per booking is its latest.
        for (BookingEvent event : eventRepository.findForBookings(bookingIds, SUMMARISED)) {
            UUID bookingId = event.getBooking().getId();
            Summary seen = summaries.get(bookingId);
            summaries.put(bookingId, switch (event.getType()) {
                case AAPTA_REALLOCATED, AAPTA_UNALLOCATED -> seen.previousAapta() != null ? seen
                        : new Summary(event.getPreviousAapta(), seen.cancelReason());
                case BOOKING_CANCELLED -> seen.cancelReason() != null ? seen
                        : new Summary(seen.previousAapta(), event.getNote());
                default -> seen;
            });
        }
        return summaries;
    }

    private void save(Booking booking, BookingEventType type, Actor actor, UUID actorId,
                      Aapta previousAapta, Aapta aapta, LocalDateTime at, String note) {
        eventRepository.save(BookingEvent.builder()
                .booking(booking)
                .type(type)
                .actor(actor)
                .actorId(actorId)
                .previousAapta(previousAapta)
                .aapta(aapta)
                .occurredAt(at)
                .note(note)
                .build());
    }
}
