// booking/service/BookingServiceTest.java
package com.ticketing.booking.service;

import com.ticketing.booking.api.BookingRequest;
import com.ticketing.booking.cache.BookingSeatsChangedEvent;
import com.ticketing.booking.repository.BookingRepository;
import com.ticketing.booking.repository.BookingSeatRepository;
import com.ticketing.catalog.domain.Event;
import com.ticketing.catalog.domain.Seat;
import com.ticketing.catalog.domain.Venue;
import com.ticketing.catalog.repository.SeatRepository;
import com.ticketing.common.exception.InvalidRequestException;
import com.ticketing.user.domain.User;
import com.ticketing.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit-level tests for BookingService.create()'s request validation.
 * Deliberately NOT Testcontainers-backed, unlike BookingConcurrencyIT -
 * the behavior under test (rejecting seat IDs spanning multiple events) is
 * pure in-memory validation with no dependency on real transaction or
 * locking semantics, so mocked repositories are sufficient.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingSeatRepository bookingSeatRepository;
    @Mock private SeatRepository seatRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private BookingService bookingService;

    private User user;
    private Seat seatFromEventA;
    private Seat seatFromEventB;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                bookingRepository, bookingSeatRepository, seatRepository, userRepository, eventPublisher);

        user = new User("Alice", "alice@test.com");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        Venue venue = new Venue("Test Arena", "Pune", 500);

        Event eventA = new Event("Concert A", venue, Instant.now().plusSeconds(86_400));
        ReflectionTestUtils.setField(eventA, "id", UUID.randomUUID());

        Event eventB = new Event("Concert B", venue, Instant.now().plusSeconds(86_400));
        ReflectionTestUtils.setField(eventB, "id", UUID.randomUUID());

        seatFromEventA = new Seat(eventA, "A1", new BigDecimal("50.00"));
        ReflectionTestUtils.setField(seatFromEventA, "id", UUID.randomUUID());

        seatFromEventB = new Seat(eventB, "B1", new BigDecimal("50.00"));
        ReflectionTestUtils.setField(seatFromEventB, "id", UUID.randomUUID());
    }

    @Test
    void create_withSeatsSpanningMultipleEvents_throwsInvalidRequestException() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(seatRepository.findAllForUpdate(anyList()))
                .thenReturn(List.of(seatFromEventA, seatFromEventB));

        BookingRequest request = new BookingRequest(
                user.getId(), List.of(seatFromEventA.getId(), seatFromEventB.getId()));

        assertThrows(InvalidRequestException.class, () -> bookingService.create(request));

        // Validation should fail before any conflict check or persistence -
        // asserting this catches a regression where ordering drifts and the
        // request does real work before being rejected.
        verify(bookingSeatRepository, never()).findActiveHolds(anyList(), any());
        verify(bookingRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(BookingSeatsChangedEvent.class));
    }
    @Test
    void create_withSeatsFromSingleEvent_doesNotThrow() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(seatRepository.findAllForUpdate(anyList()))
                .thenReturn(List.of(seatFromEventA));
        when(bookingSeatRepository.findActiveHolds(anyList(), any()))
                .thenReturn(List.of());
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BookingRequest request = new BookingRequest(user.getId(), List.of(seatFromEventA.getId()));

        assertDoesNotThrow(() -> bookingService.create(request));
        verify(eventPublisher).publishEvent(any(BookingSeatsChangedEvent.class));
    }
}