// booking/cache/BookingSeatsChangedListener.java
package com.ticketing.booking.cache;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BookingSeatsChangedListener {

    private final SeatAvailabilityCacheService cacheService;

    public BookingSeatsChangedListener(SeatAvailabilityCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingSeatsChanged(BookingSeatsChangedEvent event) {
        cacheService.evict(event.eventId());
    }
}