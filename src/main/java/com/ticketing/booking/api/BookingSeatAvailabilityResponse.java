// booking/api/BookingSeatAvailabilityResponse.java
package com.ticketing.booking.api;

import com.ticketing.booking.cache.SeatAvailability;
import com.ticketing.booking.cache.SeatAvailabilityView;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingSeatAvailabilityResponse(
        UUID seatId,
        String seatLabel,
        BigDecimal price,
        SeatAvailability status
) {
    static BookingSeatAvailabilityResponse from(SeatAvailabilityView view) {
        return new BookingSeatAvailabilityResponse(
                view.seatId(), view.seatLabel(), view.price(), view.status());
    }
}