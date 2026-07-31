// booking/controller/BookingControllerIT.java
package com.ticketing.booking.controller;

import com.redis.testcontainers.RedisContainer;
import com.ticketing.catalog.domain.Event;
import com.ticketing.catalog.domain.Seat;
import com.ticketing.catalog.domain.Venue;
import com.ticketing.catalog.repository.EventRepository;
import com.ticketing.catalog.repository.SeatRepository;
import com.ticketing.catalog.repository.VenueRepository;
import com.ticketing.user.domain.User;
import com.ticketing.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end HTTP test for BookingController.create(), specifically the
 * multi-event validation -> 400 mapping. Needs both Postgres and Redis
 * containers: create() publishes BookingSeatsChangedEvent on success, and
 * its AFTER_COMMIT listener evicts the Redis cache as a real side effect -
 * without Redis running, a successful booking would fail with a connection
 * error at the listener, not a clean 201.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerIT {

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
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private SeatRepository seatRepository;

    private UUID userId;
    private UUID seatFromEventAId;
    private UUID seatFromEventBId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(new User("Alice", "alice-" + UUID.randomUUID() + "@test.com"));
        userId = user.getId();

        Venue venue = venueRepository.save(new Venue("Test Arena", "Pune", 500));

        Event eventA = eventRepository.save(new Event("Concert A", venue, Instant.now().plusSeconds(86_400)));
        Event eventB = eventRepository.save(new Event("Concert B", venue, Instant.now().plusSeconds(86_400)));

        seatFromEventAId = seatRepository.save(new Seat(eventA, "A1", new BigDecimal("50.00"))).getId();
        seatFromEventBId = seatRepository.save(new Seat(eventB, "B1", new BigDecimal("50.00"))).getId();
    }

    @Test
    void create_withSeatsSpanningMultipleEvents_returns400() throws Exception {
        String requestBody = """
                {
                    "userId": "%s",
                    "seatIds": ["%s", "%s"]
                }
                """.formatted(userId, seatFromEventAId, seatFromEventBId);

        mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("All seats in a booking must belong to the same event"));
    }

    @Test
    void create_withSeatsFromSingleEvent_returns201() throws Exception {
        String requestBody = """
                {
                    "userId": "%s",
                    "seatIds": ["%s"]
                }
                """.formatted(userId, seatFromEventAId);

        mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}