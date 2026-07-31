// booking/controller/SeatAvailabilityControllerIT.java
package com.ticketing.booking.controller;

import com.ticketing.catalog.domain.Event;
import com.ticketing.catalog.domain.Seat;
import com.ticketing.catalog.domain.Venue;
import com.ticketing.catalog.repository.EventRepository;
import com.ticketing.catalog.repository.SeatRepository;
import com.ticketing.catalog.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.redis.testcontainers.RedisContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

/**
 * End-to-end HTTP test for the seat availability read path: real Postgres,
 * real Redis, real MVC dispatch and JSON serialization - deliberately full
 * @SpringBootTest rather than @WebMvcTest, since this phase has already
 * surfaced multiple Jackson-specific surprises and the HTTP-layer
 * ObjectMapper is a distinct pipeline from the Redis one.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class SeatAvailabilityControllerIT {

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
    private VenueRepository venueRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private SeatRepository seatRepository;

    private UUID eventId;

    @BeforeEach
    void setUp() {
        Venue venue = venueRepository.save(new Venue("Test Arena", "Pune", 500));
        Event event = eventRepository.save(new Event("Test Concert", venue, Instant.now().plusSeconds(86_400)));
        eventId = event.getId();
        seatRepository.save(new Seat(event, "A1", new BigDecimal("99.99")));
    }

    @Test
    void getAvailability_returnsSeatWithAvailableStatus() throws Exception {
        mockMvc.perform(get("/events/{eventId}/seats/availability", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].seatLabel").value("A1"))
                .andExpect(jsonPath("$[0].price").value(99.99))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void getAvailability_forNonexistentEvent_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/events/{eventId}/seats/availability", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}