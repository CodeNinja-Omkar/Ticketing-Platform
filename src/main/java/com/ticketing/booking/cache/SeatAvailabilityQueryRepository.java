
package com.ticketing.booking.cache;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only, cache-support query. Deliberately not extending JpaRepository —
 * this repository exists to serve exactly one query for exactly one
 * consumer (SeatAvailabilityCacheService), not general CRUD on Seat.
 */
public interface SeatAvailabilityQueryRepository extends Repository<com.ticketing.catalog.domain.Seat, UUID> {

    @Query("""
        SELECT new com.ticketing.booking.cache.SeatAvailabilityRow(
            s.id, s.seatLabel, s.price, bs.status
        )
        FROM Seat s
        LEFT JOIN BookingSeat bs
            ON bs.seat = s
            AND (bs.status = com.ticketing.booking.domain.SeatHoldStatus.CONFIRMED
                 OR (bs.status = com.ticketing.booking.domain.SeatHoldStatus.HELD
                     AND bs.holdExpiresAt > :now))
        WHERE s.event.id = :eventId
        ORDER BY s.seatLabel
        """)
    List<SeatAvailabilityRow> findAvailabilityForEvent(
            @Param("eventId") UUID eventId,
            @Param("now") Instant now
    );
}