// catalog/repository/SeatRepository.java
package com.ticketing.catalog.repository;

import com.ticketing.catalog.domain.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    List<Seat> findByEventId(UUID eventId);

    /**
     * Locks the given seats for the duration of the caller's transaction
     * (Postgres SELECT ... FOR UPDATE), blocking any other transaction that
     * tries to acquire a conflicting lock on the same rows until this one
     * commits or rolls back.

     * ORDER BY s.id is deliberate: it guarantees every caller acquires locks
     * on a multi-seat booking in the same global order, regardless of the
     * order seat IDs appear in the request. Without this, two concurrent
     * multi-seat bookings that share seats could lock them in opposite
     * orders and deadlock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id IN :seatIds ORDER BY s.id")
    List<Seat> findAllForUpdate(@Param("seatIds") List<UUID> seatIds);
}