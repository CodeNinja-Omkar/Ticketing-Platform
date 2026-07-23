// catalog/repository/EventRepository.java
package com.ticketing.catalog.repository;

import com.ticketing.catalog.domain.Event;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    @EntityGraph(attributePaths = "venue")
    Page<Event> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "venue")
    java.util.Optional<Event> findById(UUID id);
}