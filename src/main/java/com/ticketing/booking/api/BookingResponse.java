package com.ticketing.booking.api;

import com.ticketing.booking.domain.Booking;
import com.ticketing.booking.domain.BookingStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID userId,
        BookingStatus status,
        Instant createdAt,
        List<BookingSeatResponse> seats
) {
    public static BookingResponse from(Booking booking){
        return new BookingResponse(
                booking.getId(),
                booking.getUser().getId(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getSeats().stream().map(BookingSeatResponse::from).toList()
        );
    }
}
