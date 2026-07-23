// catalog/repository/VenueRepository.java
package com.ticketing.catalog.repository;

import com.ticketing.catalog.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VenueRepository extends JpaRepository<Venue, UUID> {
}