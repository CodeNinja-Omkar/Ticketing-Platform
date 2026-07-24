package com.ticketing.booking.api;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record BookingRequest(
        @NotNull(message = "userId is required")
        UUID userId,

        @NotNull(message = "seatIds must not be empty")
        List<UUID> seatIds
) {

}
