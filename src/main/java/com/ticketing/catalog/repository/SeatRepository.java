// catalog/repository/SeatRepository.java
package com.ticketing.catalog.repository;

import com.ticketing.catalog.domain.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {
    List<Seat> findByEventId(UUID eventId);
}