package com.ticketing.catalog.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SeatBatchRequest(
        @NotEmpty(message = "seats must not be empty")
        @Valid
        List<SeatRequest> seats
) {
}
