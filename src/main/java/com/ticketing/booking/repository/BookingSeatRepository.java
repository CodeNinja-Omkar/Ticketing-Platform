// booking/repository/BookingSeatRepository.java
package com.ticketing.booking.repository;

import com.ticketing.booking.domain.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, UUID> {

    @Query("""
        SELECT bs FROM BookingSeat bs
        WHERE bs.seat.id IN :seatIds
        AND (bs.status = com.ticketing.booking.domain.SeatHoldStatus.CONFIRMED
             OR (bs.status = com.ticketing.booking.domain.SeatHoldStatus.HELD
                 AND bs.holdExpiresAt > :now))
        """)
    List<BookingSeat> findActiveHolds(
            @Param("seatIds") List<UUID> seatIds,
            @Param("now") Instant now
    );
}