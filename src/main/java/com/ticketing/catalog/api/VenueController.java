package com.ticketing.catalog.api;

import com.ticketing.catalog.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/v1/venues")
public class VenueController {
    private final VenueService venueService;

    public VenueController(VenueService venueService){
        this.venueService = venueService;
    }
    @PostMapping
    public ResponseEntity<VenueResponse> create(@Valid @RequestBody VenueRequest request){
        VenueResponse created = venueService.create(request);
        return ResponseEntity.created(URI.create("/v1/venues/" + created.id())).body(created);
    }
    @GetMapping("/{id}")
    public VenueResponse getById(@PathVariable UUID id){
        return venueService.getById(id);
    }
    @GetMapping
    public Page<VenueResponse> list(Pageable pageable) {
        return venueService.list(pageable);
    }
}
