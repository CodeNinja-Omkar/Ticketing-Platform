package com.ticketing.catalog.service;


import com.ticketing.catalog.api.VenueRequest;
import com.ticketing.catalog.api.VenueResponse;
import com.ticketing.catalog.domain.Venue;
import com.ticketing.catalog.repository.VenueRepository;
import com.ticketing.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class VenueService {
    private final VenueRepository venueRepository;

    public VenueService(VenueRepository venueRepository){
        this.venueRepository = venueRepository;
    }

    @Transactional
    public VenueResponse create(VenueRequest request){
        Venue venue = new Venue(request.name(),request.city(), request.capacity());
        Venue saved = venueRepository.save(venue);
        return VenueResponse.from(saved);
    }
    public VenueResponse getById(UUID id){
        Venue venue = findVenueOrThrow(id);
        return VenueResponse.from(venue);
    }
    public Page<VenueResponse> list(Pageable pageable) {
        return venueRepository.findAll(pageable).map(VenueResponse::from);
    }

    private Venue findVenueOrThrow(UUID id){
        return venueRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Venue not found: "+id));
    }
}
