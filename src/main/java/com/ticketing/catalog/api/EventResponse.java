package com.ticketing.catalog.api;

import com.ticketing.catalog.domain.Event;
import com.ticketing.catalog.domain.EventStatus;

import java.time.Instant;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String name,
        UUID venueId,
        String venueName,
        Instant startsAt,
        EventStatus status
) {
    public static EventResponse from(Event event){
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getVenue().getId(),
                event.getVenue().getName(),
                event.getStartsAt(),
                event.getStatus()
        );
    }
}
