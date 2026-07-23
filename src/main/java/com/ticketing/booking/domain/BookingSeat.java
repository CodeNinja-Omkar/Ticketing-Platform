package com.ticketing.booking.domain;


import com.ticketing.catalog.domain.Seat;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_seats")
public class BookingSeat {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name="booking_id",nullable = false)
    private Booking booking;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name="seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatHoldStatus  status;

    @Column(nullable = false)
    private Instant holdExpiresAt;

    protected BookingSeat(){

    }

    public BookingSeat(Seat seat, Instant holdExpiresAt){
        this.seat = seat;
        this.status = SeatHoldStatus.HELD;
        this.holdExpiresAt = holdExpiresAt;
    }

    void assignTo(Booking booking) {
        this.booking = booking;
    }

    public UUID getId() { return id; }
    public Booking getBooking() { return booking; }
    public Seat getSeat() { return seat; }
    public SeatHoldStatus getStatus() { return status; }
    public Instant getHoldExpiresAt() { return holdExpiresAt; }

    public void confirm() {
        if (status != SeatHoldStatus.HELD) {
            throw new IllegalStateException("Only a HELD seat can be confirmed");
        }
        this.status = SeatHoldStatus.CONFIRMED;
    }

    public void release() {
        if (status == SeatHoldStatus.RELEASED || status == SeatHoldStatus.EXPIRED) {
            return; // already released, no-op
        }
        this.status = SeatHoldStatus.RELEASED;
    }

}
