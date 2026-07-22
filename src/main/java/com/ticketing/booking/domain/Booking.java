// booking/domain/Booking.java
package com.ticketing.booking.domain;

import com.ticketing.user.domain.User;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@SQLRestriction("deleted_at IS NULL")
public class Booking {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant deletedAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingSeat> seats = new ArrayList<>();

    protected Booking() {
    }

    public Booking(User user) {
        this.user = user;
        this.status = BookingStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public BookingStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public boolean isDeleted() { return deletedAt != null; }
    public List<BookingSeat> getSeats() { return seats; }

    public void addSeat(BookingSeat bookingSeat) {
        seats.add(bookingSeat);
        bookingSeat.assignTo(this);
    }

    public void confirm() {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException("Only a PENDING booking can be confirmed");
        }
        this.status = BookingStatus.CONFIRMED;
    }

    public void softDelete() {
        if (this.deletedAt != null) {
            throw new IllegalStateException("Booking is already deleted");
        }
        this.deletedAt = Instant.now();
    }
}