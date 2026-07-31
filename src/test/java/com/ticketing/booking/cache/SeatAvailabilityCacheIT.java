// src/test/java/com/ticketing/booking/cache/SeatAvailabilityCacheIT.java
package com.ticketing.booking.cache;

import com.redis.testcontainers.RedisContainer;
import com.ticketing.booking.api.BookingRequest;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies SeatAvailabilityCacheService's cache-aside behavior and the
 * AFTER_COMMIT invalidation wiring end to end: real Postgres, real Redis,
 * no mocks - same philosophy as BookingConcurrencyIT.
 *
 * IMPORTANT: not @Transactional, for the same reason as BookingConcurrencyIT -
 * bookingService.create() needs to run and commit its own real transaction
 * for AFTER_COMMIT semantics to actually be exercised. Wrapping the whole
 * test in a transaction would mean nothing ever really commits, and the
 * eviction listener would never fire.
 */
@Testcontainers
@SpringBootTest
class SeatAvailabilityCacheIT {

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
    }

    @Autowired
    private SeatAvailabilityCacheService cacheService;
    @Autowired
    private BookingService bookingService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private SeatRepository seatRepository;

    private UUID eventId;
    private UUID seatId;
    private UUID userId;
    private String cacheKey;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(new User("Alice", "alice-" + UUID.randomUUID() + "@test.com"));
        userId = user.getId();

        Venue venue = venueRepository.save(new Venue("Test Arena", "Pune", 500));
        Event event = eventRepository.save(new Event("Test Concert", venue, Instant.now().plusSeconds(86_400)));
        eventId = event.getId();

        Seat seat = seatRepository.save(new Seat(event, "A1", new BigDecimal("99.99")));
        seatId = seat.getId();

        cacheKey = "event:" + eventId + ":seats";
    }

    @Test
    void getOrLoad_onMiss_populatesRedisWithTTL() {
        assertFalse(redisTemplate.hasKey(cacheKey), "precondition: cache should be empty before first read");

        List<SeatAvailabilityView> result = cacheService.getOrLoad(eventId);

        assertEquals(1, result.size());
        assertEquals(SeatAvailability.AVAILABLE, result.getFirst().status());

        assertTrue(redisTemplate.hasKey(cacheKey), "cache entry should exist after getOrLoad");

        Long ttlSeconds = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
        assertNotNull(ttlSeconds);
        assertTrue(ttlSeconds > 0 && ttlSeconds <= 30,
                "TTL should be set and at most 30s, was: " + ttlSeconds);
    }

    @Test
    void getOrLoad_onHit_servesStaleDataRatherThanRequeryingDatabase() {
        // Warm the cache with the current state (one AVAILABLE seat).
        List<SeatAvailabilityView> firstRead = cacheService.getOrLoad(eventId);
        assertEquals(1, firstRead.size());

        // Mutate the underlying data WITHOUT going through BookingService,
        // so no invalidation event fires. If getOrLoad is truly reading from
        // cache, it must not notice this new seat until the entry expires
        // or is explicitly evicted.
        seatRepository.save(new Seat(
                eventRepository.findById(eventId).orElseThrow(), "A2", new BigDecimal("50.00")));

        List<SeatAvailabilityView> secondRead = cacheService.getOrLoad(eventId);

        assertEquals(1, secondRead.size(),
                "second read should still reflect the cached (stale) 1-seat view, "
                        + "not the 2 seats now actually in the database");
    }

    @Test
    void evict_removesCacheEntry() {
        cacheService.getOrLoad(eventId);
        assertTrue(redisTemplate.hasKey(cacheKey));

        cacheService.evict(eventId);

        assertFalse(redisTemplate.hasKey(cacheKey));
    }

    @Test
    void bookingCreate_afterCommit_evictsCache() {
        cacheService.getOrLoad(eventId);
        assertTrue(redisTemplate.hasKey(cacheKey), "precondition: cache warmed before booking");

        bookingService.create(new BookingRequest(userId, List.of(seatId)));

        assertFalse(redisTemplate.hasKey(cacheKey),
                "cache should be evicted immediately after the booking transaction commits");
    }

    @Test
    void bookingCreate_thatRollsBackOnConflict_doesNotEvictCache() {
        // First booking succeeds and holds the seat.
        bookingService.create(new BookingRequest(userId, List.of(seatId)));

        // Cache is gone from the successful create() above - warm it again
        // to isolate what we're actually testing: does a ROLLED-BACK
        // transaction evict.
        cacheService.getOrLoad(eventId);
        assertTrue(redisTemplate.hasKey(cacheKey), "precondition: cache warmed before conflicting attempt");

        User secondUser = userRepository.save(
                new User("Bob", "bob-" + UUID.randomUUID() + "@test.com"));

        assertThrows(ConflictException.class, () ->
                bookingService.create(new BookingRequest(secondUser.getId(), List.of(seatId))));

        assertTrue(redisTemplate.hasKey(cacheKey),
                "cache must NOT be evicted when the booking transaction rolled back - "
                        + "this is the core AFTER_COMMIT guarantee being verified");
    }
}