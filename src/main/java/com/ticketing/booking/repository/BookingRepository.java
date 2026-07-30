package com.ticketing.booking.repository;

import com.ticketing.booking.domain.Booking;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Override
    @EntityGraph(attributePaths = {"seats", "seats.seat", "seats.seat.event"})
    @NonNull Optional<Booking> findById(@NonNull  UUID id);
}