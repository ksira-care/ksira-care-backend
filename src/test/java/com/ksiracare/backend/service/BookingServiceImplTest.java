package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.response.BookingItemDto;
import com.ksiracare.backend.dto.response.BookingsResponseDto;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.entity.LanguageEntity;
import com.ksiracare.backend.entity.Therapist;
import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.enums.Language;
import com.ksiracare.backend.exception.ResourceNotFoundException;
import com.ksiracare.backend.mapper.BookingMapper;
import com.ksiracare.backend.mapper.TherapistMapper;
import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.repository.TherapistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private TherapistRepository therapistRepository;

    @Mock
    private BookingRepository bookingRepository;

    private final BookingMapper bookingMapper = Mappers.getMapper(BookingMapper.class);
    private final TherapistMapper therapistMapper = Mappers.getMapper(TherapistMapper.class);

    private BookingServiceImpl bookingService;

    private UUID therapistId;
    private Therapist mockTherapist;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl(
                therapistRepository,
                bookingRepository,
                bookingMapper,
                therapistMapper
        );

        therapistId = UUID.randomUUID();
        mockTherapist = mock(Therapist.class);
        lenient().when(mockTherapist.getFirstName()).thenReturn("Anya");
        lenient().when(mockTherapist.getLastName()).thenReturn("M.");
    }

    private Long toEpochMilli(LocalDateTime ldt) {
        return ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when therapist is not found")
    void testGetBookingsTherapistNotFound() {
        Long start = toEpochMilli(LocalDateTime.of(2026, 9, 27, 0, 0));
        Long end = toEpochMilli(LocalDateTime.of(2026, 9, 27, 23, 59));

        when(therapistRepository.findById(therapistId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                bookingService.getBookings(therapistId, start, end, null));
    }

    @Test
    @DisplayName("Should return BookingsResponseDto with stats and booking list")
    void testGetBookingsSuccess() {
        LocalDateTime startDT = LocalDateTime.of(2026, 9, 27, 9, 0);
        LocalDateTime endDT = LocalDateTime.of(2026, 9, 27, 18, 0);
        LocalDateTime assignedAtDT = LocalDateTime.of(2026, 9, 9, 14, 30);
        LocalDateTime prevStartDT = LocalDateTime.of(2026, 9, 26, 16, 0);
        Long start = toEpochMilli(startDT);
        Long end = toEpochMilli(endDT);

        UUID bookingId = UUID.randomUUID();
        Booking mockBooking = Booking.builder()
                .id(bookingId)
                .therapist(mockTherapist)
                .customerName("Sara L.")
                .customerEmail("sara@example.com")
                .customerCountry("United Kingdom")
                .customerPreferredLanguages(Set.of(new LanguageEntity(Language.ENGLISH, "ENGLISH")))
                .startTime(startDT)
                .endTime(startDT.plusHours(1))
                .bookingReason("Need to discuss stress")
                .status(BookingStatus.PENDING)
                .therapistFee(BigDecimal.valueOf(600.00))
                .assignedAt(assignedAtDT)
                .rescheduled(true)
                .previousStartTime(prevStartDT)
                .rescheduleReason("Client requested change by email >24h ahead")
                .build();

        when(therapistRepository.findById(therapistId)).thenReturn(Optional.of(mockTherapist));
        when(bookingRepository.findByTherapistIdAndStartTimeBetween(therapistId, startDT, endDT))
                .thenReturn(List.of(mockBooking));
        when(bookingRepository.countByTherapistIdAndStartTimeBetween(eq(therapistId), any(), any()))
                .thenReturn(14L);
        when(bookingRepository.countByTherapistIdAndStatus(therapistId, BookingStatus.COMPLETED))
                .thenReturn(50L);

        BookingsResponseDto response = bookingService.getBookings(therapistId, start, end, null);

        assertNotNull(response);
        assertEquals("Anya M.", response.getTherapistName());
        assertEquals(14L, response.getSessionsThisMonth());
        assertEquals(50L, response.getCompletedSessions());
        assertEquals(1, response.getBookings().size());

        BookingItemDto item = response.getBookings().getFirst();
        assertEquals(bookingId, item.getBookingId());
        assertEquals("Sara L.", item.getCustomerName());
        assertEquals("United Kingdom", item.getCustomerCountry());
        assertEquals(BookingStatus.PENDING, item.getBookingStatus());
        assertEquals(toEpochMilli(assignedAtDT), item.getAssignedAt());
        assertTrue(item.isRescheduled());
        assertEquals(toEpochMilli(prevStartDT), item.getPreviousStartTime());
        assertEquals("Client requested change by email >24h ahead", item.getRescheduleReason());
    }

    @Test
    @DisplayName("Should update booking status successfully when session start time passed")
    void testUpdateBookingStatusSuccess() {
        UUID bookingId = UUID.randomUUID();
        LocalDateTime pastStartTime = LocalDateTime.now(ZoneOffset.UTC).minusHours(2);

        Booking booking = Booking.builder()
                .id(bookingId)
                .therapist(mockTherapist)
                .customerName("Sara L.")
                .startTime(pastStartTime)
                .endTime(pastStartTime.plusHours(1))
                .status(BookingStatus.PENDING)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingItemDto result = bookingService.updateBookingStatus(bookingId, BookingStatus.COMPLETED);

        assertNotNull(result);
        assertEquals(BookingStatus.COMPLETED, result.getBookingStatus());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when marking complete before session start time")
    void testUpdateBookingStatusBeforeStartTimeFails() {
        UUID bookingId = UUID.randomUUID();
        LocalDateTime futureStartTime = LocalDateTime.now(ZoneOffset.UTC).plusDays(1);

        Booking booking = Booking.builder()
                .id(bookingId)
                .therapist(mockTherapist)
                .customerName("Sara L.")
                .startTime(futureStartTime)
                .endTime(futureStartTime.plusHours(1))
                .status(BookingStatus.PENDING)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThrows(IllegalArgumentException.class, () ->
                bookingService.updateBookingStatus(bookingId, BookingStatus.COMPLETED));
    }
}
