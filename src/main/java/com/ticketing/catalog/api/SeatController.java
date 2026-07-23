package com.ticketing.catalog.api;

import com.ticketing.catalog.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/events/{eventId}/seats")
public class SeatController {
    private final SeatService seatService;

    public SeatController(SeatService seatService){
        this.seatService =seatService;
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<SeatResponse> createBatch(
            @PathVariable UUID eventId,
            @Valid @RequestBody SeatBatchRequest request){

        return seatService.createBatch(eventId,request);
    }
    @GetMapping
    public List<SeatResponse> list(@PathVariable UUID eventId){
        return seatService.listForEvent(eventId);
    }
}
