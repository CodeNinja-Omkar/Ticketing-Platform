package com.ticketing.catalog.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="events")
public class Event {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="venue_id", nullable = false)
    private Venue venue;

    @Column(nullable = false)
    private Instant startsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    protected Event(){

    }

    public Event(String name, Venue venue, Instant startsAt) {
        this.name = name;
        this.venue = venue;
        this.startsAt = startsAt;
        this.status = EventStatus.SCHEDULED;

    }

    public UUID getId(){
        return id;
    }
    public String getName(){
        return name;
    }
    public Venue getVenue(){
        return venue;
    }
    public Instant getStartsAt(){
        return startsAt;
    }
    public EventStatus getStatus(){
        return status;
    }

    public void cancel(){
        if(status==EventStatus.COMPLETED){
            throw new IllegalStateException("Cannot cancel a completed event");
        }
    this.status = EventStatus.CANCELLED;
    }
}