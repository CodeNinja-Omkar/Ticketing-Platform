// catalog/api/EventRequest.java
package com.ticketing.catalog.api;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record EventRequest(
        @NotBlank(message = "name is required")
        String name,

        @NotNull(message = "venueId is required")
        UUID venueId,

        @NotNull(message = "startsAt is required")
        @Future(message = "startsAt must be in the future")
        Instant startsAt
) {
}