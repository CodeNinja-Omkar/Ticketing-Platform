package com.ticketing.catalog.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SeatRequest(
        @NotBlank(message = "seatLabel is required")
        String seatLabel,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "price must be postive")
        BigDecimal price
) {

}
