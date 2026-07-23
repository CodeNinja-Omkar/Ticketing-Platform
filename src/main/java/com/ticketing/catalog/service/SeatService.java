// catalog/service/SeatService.java
package com.ticketing.catalog.service;

import com.ticketing.catalog.api.SeatBatchRequest;
import com.ticketing.catalog.api.SeatRequest;
import com.ticketing.catalog.api.SeatResponse;
import com.ticketing.catalog.domain.Event;
import com.ticketing.catalog.domain.Seat;
import com.ticketing.catalog.repository.EventRepository;
import com.ticketing.catalog.repository.SeatRepository;
import com.ticketing.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SeatService {

    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;

    public SeatService(SeatRepository seatRepository, EventRepository eventRepository) {
        this.seatRepository = seatRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public List<SeatResponse> createBatch(UUID eventId, SeatBatchRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        List<Seat> seats = request.seats().stream()
                .map(seatRequest -> new Seat(event, seatRequest.seatLabel(), seatRequest.price()))
                .toList();

        return seatRepository.saveAll(seats).stream()
                .map(SeatResponse::from)
                .toList();
    }

    public List<SeatResponse> listForEvent(UUID eventId) {
        return seatRepository.findByEventId(eventId).stream()
                .map(SeatResponse::from)
                .toList();
    }
}