// payment/domain/Payment.java
package com.ticketing.payment.domain;

import com.ticketing.booking.domain.Booking;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Payment() {
    }

    public Payment(Booking booking, BigDecimal amount) {
        this.booking = booking;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public Booking getBooking() { return booking; }
    public BigDecimal getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void authorize() {
        transitionTo(PaymentStatus.AUTHORIZED, PaymentStatus.PENDING);
    }

    public void capture() {
        transitionTo(PaymentStatus.CAPTURED, PaymentStatus.AUTHORIZED);
    }

    public void fail() {
        transitionTo(PaymentStatus.FAILED, PaymentStatus.PENDING, PaymentStatus.AUTHORIZED);
    }

    public void refund() {
        transitionTo(PaymentStatus.REFUNDED, PaymentStatus.CAPTURED);
    }

    private void transitionTo(PaymentStatus target, PaymentStatus... allowedFrom) {
        boolean allowed = false;
        for (PaymentStatus from : allowedFrom) {
            if (this.status == from) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new IllegalStateException(
                    "Cannot transition payment from " + this.status + " to " + target);
        }
        this.status = target;
        this.updatedAt = Instant.now();
    }
}