package com.ticketing.common.outbox;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Polls the outbox table and publishes eligible rows to Kafka.
 *
 * The entire poll-send-mark cycle runs inside a single transaction. This is
 * required, not incidental: the row locks acquired by pollBatch's
 * SELECT ... FOR UPDATE SKIP LOCKED only protect against a concurrent relay
 * instance picking up the same rows for as long as this transaction stays
 * open. If the transaction were split (e.g. poll in one transaction, send
 * and mark in another), the locks would release before the Kafka sends
 * happen, and two relay instances could publish the same event.
 *
 * Delivery is at-least-once by design: a crash after a successful Kafka send
 * but before this transaction commits will cause that event to be retried
 * and published again on the next poll. Consumers of domain-events must be
 * idempotent.
 */
@Service
public class OutboxRelayService {

    private static final String TOPIC = "domain-events";
    private static final int BATCH_SIZE = 20;
    private static final int MAX_RETRIES = 3;
    private static final Duration BASE_BACKOFF = Duration.ofSeconds(2);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelayService(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> batch = outboxEventRepository.pollBatch(Instant.now(), BATCH_SIZE);

        if (batch.isEmpty()) {
            return;
        }

        for (OutboxEvent outboxEvent : batch) {
            publishOne(outboxEvent);
        }

        outboxEventRepository.saveAll(batch);
    }

    private void publishOne(OutboxEvent outboxEvent) {
        try {
            kafkaTemplate.send(TOPIC, outboxEvent.getEventType(), outboxEvent.getPayload()).get();
            outboxEvent.markPublished();
        } catch (Exception ex) {
            outboxEvent.recordFailure(ex.getMessage(), MAX_RETRIES, BASE_BACKOFF);
        }
    }
}