package com.ticketing.booking.cache;

import com.ticketing.common.outbox.OutboxEvent;
import com.ticketing.common.outbox.OutboxEventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Writes an outbox row for BookingSeatsChangedEvent so it can later be
 * published to Kafka by the outbox relay.
 *
 * Deliberately BEFORE_COMMIT, not AFTER_COMMIT: this write must be atomic
 * with the booking transaction that triggered the event. If it were
 * AFTER_COMMIT, a crash between the booking commit and this write would
 * leave a persisted booking with no corresponding outbox row, silently
 * losing the event forever. Running it before commit means a failure here
 * rolls back the booking too, so the two either both persist or neither does.
 *
 * This is a separate listener from the AFTER_COMMIT cache-eviction listener
 * on the same event: the two listeners have different atomicity
 * requirements, not just different jobs.
 */
@Component
public class BookingOutboxEventListener {

    private static final String EVENT_TYPE = "BookingSeatsChanged";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public BookingOutboxEventListener(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onBookingSeatsChanged(BookingSeatsChangedEvent event) {
        String payload = objectMapper.writeValueAsString(Map.of("eventId", event.eventId()));
        outboxEventRepository.save(new OutboxEvent(EVENT_TYPE, payload));
    }
}