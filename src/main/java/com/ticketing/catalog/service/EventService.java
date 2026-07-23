// catalog/service/EventService.java
package com.ticketing.catalog.service;

import com.ticketing.catalog.api.EventRequest;
import com.ticketing.catalog.api.EventResponse;
import com.ticketing.catalog.domain.Event;
import com.ticketing.catalog.domain.Venue;
import com.ticketing.catalog.repository.EventRepository;
import com.ticketing.catalog.repository.VenueRepository;
import com.ticketing.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;

    public EventService(EventRepository eventRepository, VenueRepository venueRepository) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
    }

    @Transactional
    public EventResponse create(EventRequest request) {
        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + request.venueId()));

        Event event = new Event(request.name(), venue, request.startsAt());
        Event saved = eventRepository.save(event);
        return EventResponse.from(saved);
    }

    public EventResponse getById(UUID id) {
        Event event = findEventOrThrow(id);
        return EventResponse.from(event);
    }

    public Page<EventResponse> list(Pageable pageable) {
        return eventRepository.findAll(pageable).map(EventResponse::from);
    }

    @Transactional
    public void cancel(UUID id) {
        Event event = findEventOrThrow(id);
        event.cancel();
    }

    private Event findEventOrThrow(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
    }
}