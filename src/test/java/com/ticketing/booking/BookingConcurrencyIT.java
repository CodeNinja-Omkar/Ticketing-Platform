// Target location in repo: src/test/java/com/ticketing/booking/BookingConcurrencyIT.java
package com.ticketing.booking;

import com.ticketing.booking.api.BookingRequest;
import com.ticketing.booking.api.BookingResponse;
import com.ticketing.booking.domain.BookingSeat;
import com.ticketing.booking.repository.BookingSeatRepository;
import com.ticketing.booking.service.BookingService;
import com.ticketing.catalog.domain.Event;
import com.ticketing.catalog.domain.Seat;
import com.ticketing.catalog.domain.Venue;
import com.ticketing.catalog.repository.EventRepository;
import com.ticketing.catalog.repository.SeatRepository;
import com.ticketing.catalog.repository.VenueRepository;
import com.ticketing.common.exception.ConflictException;
import com.ticketing.user.domain.User;
import com.ticketing.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Proves (and, after the Phase 2 fix, disproves) the double-booking race condition
 * in BookingService.create(). Deliberately bypasses the HTTP layer and drives the
 * service method directly, since the race lives inside the transaction boundary,
 * not in MVC/serialization.

 * IMPORTANT: this test class must NOT be @Transactional. Each simulated caller needs
 * its own transaction — that is the entire point of the test.
 */
@Testcontainers
@SpringBootTest
class BookingConcurrencyIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ticketing")
            .withUsername("ticketing")
            .withPassword("ticketing");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private BookingService bookingService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private BookingSeatRepository bookingSeatRepository;

    private UUID seatId;
    private UUID userAId;
    private UUID userBId;

    @BeforeEach
    void setUp() {
        User userA = userRepository.save(new User("Alice", "alice-" + UUID.randomUUID() + "@test.com"));
        User userB = userRepository.save(new User("Bob", "bob-" + UUID.randomUUID() + "@test.com"));
        userAId = userA.getId();
        userBId = userB.getId();

        Venue venue = venueRepository.save(new Venue("Test Arena", "Pune", 500));
        Event event = eventRepository.save(new Event("Test Concert", venue, Instant.now().plusSeconds(86_400)));
        Seat seat = seatRepository.save(new Seat(event, "A1", new java.math.BigDecimal("99.99")));
        seatId = seat.getId();
    }

    @RepeatedTest(20)
    void concurrentBookingsForSameSeat_onlyOneShouldSucceed() throws InterruptedException {
        int callers = 2;
        ExecutorService executor = Executors.newFixedThreadPool(callers);
        CountDownLatch readyLatch = new CountDownLatch(callers);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<UUID> callerIds = List.of(userAId, userBId);

        List<Future<BookingResponse>> futures = callerIds.stream()
                .map(userId -> executor.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                        BookingResponse response = bookingService.create(
                                new BookingRequest(userId, List.of(seatId)));
                        successCount.incrementAndGet();
                        return response;
                    } catch (ConflictException expected) {
                        conflictCount.incrementAndGet();
                        return null;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }))
                .toList();

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<?> future : futures) {
            try {
                future.get(10, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException e) {
                throw new RuntimeException("Booking task failed unexpectedly", e);
            }
        }
        executor.shutdown();

        List<BookingSeat> activeHolds = bookingSeatRepository.findActiveHolds(List.of(seatId), Instant.now());

        assertEquals(1, successCount.get(),
                "Exactly one of the two concurrent bookings should have succeeded");
        assertEquals(1, conflictCount.get(),
                "Exactly one of the two concurrent bookings should have been rejected with ConflictException");
        assertEquals(1, activeHolds.size(),
                "Exactly one active (HELD/CONFIRMED) booking_seats row should exist for this seat, "
                        + "but found " + activeHolds.size() + " - this is the double-booking bug");
    }
}