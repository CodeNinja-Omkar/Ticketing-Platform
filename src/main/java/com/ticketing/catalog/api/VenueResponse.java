package com.ticketing.catalog.api;

import com.ticketing.catalog.domain.Venue;

import java.util.UUID;

public record VenueResponse(
        UUID id,
        String name,
        String city,
        int capacity
) {
    public static VenueResponse from(Venue venue){
        return new VenueResponse(
                venue.getId(),
                venue.getName(),
                venue.getCity(),
                venue.getCapacity()
        );
    }
}
