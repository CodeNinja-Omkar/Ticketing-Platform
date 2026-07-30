package com.ticketing.booking.service;

import com.ticketing.booking.api.BookingRequest;
import com.ticketing.booking.api.BookingResponse;
import com.ticketing.booking.cache.BookingSeatsChangedEvent;
import com.ticketing.booking.domain.Booking;
import com.ticketing.booking.domain.BookingSeat;
import com.ticketing.booking.repository.BookingRepository;
import com.ticketing.booking.repository.BookingSeatRepository;
import com.ticketing.catalog.domain.Seat;
import com.ticketing.catalog.repository.SeatRepository;
import com.ticketing.common.exception.ConflictException;
import com.ticketing.common.exception.InvalidRequestException;
import com.ticketing.common.exception.ResourceNotFoundException;
import com.ticketing.user.domain.User;
import com.ticketing.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BookingService {

    private static final Duration HOLD_DURATION = Duration.ofMinutes(10);

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BookingService(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            SeatRepository seatRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public BookingResponse create(BookingRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userId()));

        List<Seat> seats = seatRepository.findAllForUpdate(request.seatIds());
        if (seats.size() != request.seatIds().size()) {
            throw new ResourceNotFoundException("One or more seats do not exist");
        }

        UUID eventId = requireSingleEvent(seats);

        List<BookingSeat> activeHolds = bookingSeatRepository.findActiveHolds(
                request.seatIds(), Instant.now());

        if (!activeHolds.isEmpty()) {
            String takenLabels = activeHolds.stream()
                    .map(bs -> bs.getSeat().getSeatLabel())
                    .collect(Collectors.joining(", "));
            throw new ConflictException("Seat(s) already held or booked: " + takenLabels);
        }

        Booking booking = new Booking(user);
        Instant holdExpiresAt = Instant.now().plus(HOLD_DURATION);
        for (Seat seat : seats) {
            booking.addSeat(new BookingSeat(seat, holdExpiresAt));
        }

        Booking saved = bookingRepository.save(booking);
        eventPublisher.publishEvent(new BookingSeatsChangedEvent(eventId));
        return BookingResponse.from(saved);
    }

    public BookingResponse getById(UUID id) {
        Booking booking = findBookingOrThrow(id);
        return BookingResponse.from(booking);
    }

    @Transactional
    public void cancel(UUID id) {
        Booking booking = findBookingOrThrow(id);
        booking.cancel();

        Set<UUID> eventIds = booking.getSeats().stream()
                .map(bs -> bs.getSeat().getEvent().getId())
                .collect(Collectors.toSet());
        eventIds.forEach(eventId ->
                eventPublisher.publishEvent(new BookingSeatsChangedEvent(eventId)));
    }

    private UUID requireSingleEvent(List<Seat> seats) {
        Set<UUID> eventIds = seats.stream()
                .map(seat -> seat.getEvent().getId())
                .collect(Collectors.toSet());
        if (eventIds.size() != 1) {
            throw new InvalidRequestException(
                    "All seats in a booking must belong to the same event");
        }
        return eventIds.iterator().next();
    }

    private Booking findBookingOrThrow(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
    }
}