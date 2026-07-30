
package com.ticketing.booking.cache;

import java.math.BigDecimal;
import java.util.UUID;

public record SeatAvailabilityView(
        UUID seatId,
        String seatLabel,
        BigDecimal price,
        SeatAvailability status
) {
    static SeatAvailabilityView from(SeatAvailabilityRow row) {
        SeatAvailability availability = switch (row.status()) {
            case null -> SeatAvailability.AVAILABLE;
            case CONFIRMED -> SeatAvailability.CONFIRMED;
            case HELD -> SeatAvailability.HELD;
            case RELEASED, EXPIRED ->
                    throw new IllegalStateException(
                            "Query should only return active holds, got: " + row.status());
        };
        return new SeatAvailabilityView(row.seatId(), row.seatLabel(), row.price(), availability);
    }
}