package com.ksiracare.backend.service;

import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.entity.BookingEvent;
import com.ksiracare.backend.enums.BookingEventType;
import com.ksiracare.backend.repository.BookingEventRepository;
import com.ksiracare.backend.security.SessionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Writes a booking's history ({@code booking_events}): what changed, who did it, when. */
@Component
@RequiredArgsConstructor
public class BookingHistory {

    private final BookingEventRepository eventRepository;

    /**
     * @param previousAapta for allocation changes, who held the booking before (else null)
     * @param aapta         who holds it after the change (else null)
     * @param note          why, where the change needs explaining (else null)
     */
    public void record(Booking booking, BookingEventType type, SessionPrincipal actor,
                       Aapta previousAapta, Aapta aapta, LocalDateTime at, String note) {
        eventRepository.save(BookingEvent.builder()
                .booking(booking)
                .type(type)
                .actorRole(actor.role())
                .actorId(actor.accountId())
                .previousAapta(previousAapta)
                .aapta(aapta)
                .occurredAt(at)
                .note(note)
                .build());
    }
}
