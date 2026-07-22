package com.ticketing.catalog.domain;

import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;

@Entity
@Table(name="seats",uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_id","seat_label"})
})
public class Seat {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name="event_id",nullable = false)
    private Event event;

    @Column(name="seat_label", nullable = false)
    private String seatLabel;

    @Column(nullable = false)
    private BigDecimal price;

    protected Seat(){

    }

    public Seat(Event event, String seatLabel, BigDecimal price){
        this.event = event;
        this.seatLabel = seatLabel;
        this.price = price;
    }
    public UUID getId(){
        return id;
    }
    public Event getEvent(){
        return event;
    }

    public String getSeatLabel(){
        return seatLabel;
    }
    public BigDecimal getPrice(){
        return price;
    }

}