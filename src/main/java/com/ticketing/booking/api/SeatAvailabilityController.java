// booking/controller/SeatAvailabilityController.java
package com.ticketing.booking.api;

import com.ticketing.booking.api.BookingSeatAvailabilityResponse;
import com.ticketing.booking.cache.SeatAvailabilityCacheService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events/{eventId}/seats/availability")
public class SeatAvailabilityController {

    private final SeatAvailabilityCacheService cacheService;

    public SeatAvailabilityController(SeatAvailabilityCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping
    public List<BookingSeatAvailabilityResponse> getAvailability(@PathVariable UUID eventId) {
        return cacheService.getOrLoad(eventId).stream()
                .map(BookingSeatAvailabilityResponse::from)
                .toList();
    }
}