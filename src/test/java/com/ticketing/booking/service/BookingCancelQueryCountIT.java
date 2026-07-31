// booking/service/BookingCancelQueryCountIT.java
package com.ticketing.booking.service;

import com.redis.testcontainers.RedisContainer;
import com.ticketing.booking.api.BookingRequest;
import com.ticketing.booking.api.BookingResponse;
import com.ticketing.catalog.domain.Event;
import com.ticketing.catalog.domain.Seat;
import com.ticketing.catalog.domain.Venue;
import com.ticketing.catalog.repository.EventRepository;
import com.ticketing.catalog.repository.SeatRepository;
import com.ticketing.catalog.repository.VenueRepository;
import com.ticketing.user.domain.User;
import com.ticketing.user.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Proves BookingRepository.findById's @EntityGraph (attributePaths =
 * {"seats", "seats.seat", "seats.seat.event"}) actually prevents the N+1
 * that cancel() would otherwise trigger walking
 * booking.getSeats() -> bs.getSeat() -> .getEvent() per seat.
 *
 * Methodology: rather than asserting a fixed query count (brittle against
 * unrelated Hibernate/version changes), this compares the query count for
 * cancelling a 1-seat booking against a 4-seat booking. If the fetch is
 * genuinely eager via the entity graph, the counts should be equal - flat
 * regardless of seat count. If N+1 regressed, the 4-seat case would show
 * additional queries proportional to the extra seats.
 */
@Testcontainers
@SpringBootTest
class BookingCancelQueryCountIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ticketing")
            .withUsername("ticketing")
            .withPassword("ticketing");

    @Container
    static RedisContainer redis =
            new RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG));

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // Required for Hibernate Statistics (query execution counts) to be
        // tracked at all - off by default, since it has real overhead and
        // should never be enabled in production.
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    }

    @Autowired
    private BookingService bookingService;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private SeatRepository seatRepository;

    @Test
    void cancel_queryCountDoesNotScaleWithSeatCount() {
        Statistics statistics = entityManagerFactory
                .unwrap(SessionFactory.class)
                .getStatistics();

        long queriesForOneSeat = createAndCancelBooking(1, statistics);
        long queriesForFourSeats = createAndCancelBooking(4, statistics);

        assertEquals(queriesForOneSeat, queriesForFourSeats,
                "cancel() should issue the same number of queries regardless of "
                        + "seat count - a difference here indicates the "
                        + "@EntityGraph on BookingRepository.findById stopped "
                        + "preventing the seats -> seat -> event N+1");
    }

    private long createAndCancelBooking(int seatCount, Statistics statistics) {
        User user = userRepository.save(new User("Alice", "alice-" + UUID.randomUUID() + "@test.com"));
        Venue venue = venueRepository.save(new Venue("Test Arena", "Pune", 500));
        Event event = eventRepository.save(new Event("Test Concert", venue, Instant.now().plusSeconds(86_400)));

        List<UUID> seatIds = IntStream.range(0, seatCount)
                .mapToObj(i -> seatRepository.save(
                                new Seat(event, "S" + i, new BigDecimal("50.00")))
                        .getId())
                .toList();

        BookingResponse booking = bookingService.create(new BookingRequest(user.getId(), seatIds));

        // Only measure cancel() - create()'s own query shape isn't what
        // we're verifying here, and including it would muddy the comparison
        // with unrelated variance (e.g. seat lookup queries scale with
        // seatIds regardless of the N+1 fix, which is expected and not a bug).
        statistics.clear();
        bookingService.cancel(booking.id());

        return statistics.getQueryExecutionCount();
    }
}