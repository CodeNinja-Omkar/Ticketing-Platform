package com.ticketing.booking.api;

import com.ticketing.booking.domain.BookingSeat;
import com.ticketing.booking.domain.SeatHoldStatus;

import java.time.Instant;
import java.util.UUID;

public record BookingSeatResponse(
        UUID seatId,
        String seatLabel,
        SeatHoldStatus status,
        Instant holdExpiresAt
) {
    public static BookingSeatResponse from(BookingSeat bookingSeat){
        return new BookingSeatResponse(
                bookingSeat.getSeat().getId(),
                bookingSeat.getSeat().getSeatLabel(),
                bookingSeat.getStatus(),
                bookingSeat.getHoldExpiresAt()
        );
    }
}
