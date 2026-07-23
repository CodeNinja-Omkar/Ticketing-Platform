package com.ticketing.catalog.api;

import com.ticketing.catalog.domain.Seat;

import java.math.BigDecimal;
import java.util.UUID;

public record SeatResponse(
        UUID id,
        UUID eventId,
        String seatLabel,
        BigDecimal price
) {
    public static SeatResponse from(Seat seat){
        return new SeatResponse(
                seat.getId(),
                seat.getEvent().getId(),
                seat.getSeatLabel(),
                seat.getPrice()
        );
    }
}
