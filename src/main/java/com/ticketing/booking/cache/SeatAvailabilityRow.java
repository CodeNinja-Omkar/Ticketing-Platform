package com.ticketing.booking.cache;

import com.ticketing.booking.domain.SeatHoldStatus;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Raw projection row — status is null when the seat has no currently
 * active hold. Not exposed outside the cache package; SeatAvailabilityView
 * is the public shape.
 */
public record SeatAvailabilityRow(
        UUID seatId,
        String seatLabel,
        BigDecimal price,
        SeatHoldStatus status
) {}