package com.ticketing.common.outbox;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Declares the domain-events topic explicitly. Without this, Kafka would
 * auto-create the topic on first publish using its default partition count
 * (typically 1), silently discarding the 3-partition design decided for
 * this project. Spring Kafka's admin client reconciles this declaration
 * against the broker on startup, creating the topic if it does not exist.
 *
 * Also declares an explicitly-typed KafkaTemplate<String, String> bean.
 * Spring Boot's autoconfigured KafkaTemplate is typed <Object, Object>, and
 * generic type parameters are part of what Spring's dependency injection
 * matches against — without this bean, injecting KafkaTemplate<String,
 * String> anywhere would fail at startup with no matching bean found, even
 * though the configured StringSerializers would work correctly at runtime.
 */
@Configuration
public class KafkaTopicConfig {

    public static final String DOMAIN_EVENTS_TOPIC = "domain-events";

    @Bean
    public NewTopic domainEventsTopic() {
        return TopicBuilder.name(DOMAIN_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public ProducerFactory<String, String> stringProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // The producer client has its own internal timeout/retry layer,
        // independent of the outbox's retry_count/backoff design. Left at
        // Kafka's defaults (max.block.ms=60s, delivery.timeout.ms=120s), a
        // single kafkaTemplate.send().get() call can block for minutes
        // against an unreachable broker before OutboxRelayService.publishOne
        // ever gets a chance to catch the failure and apply its own backoff.
        // These are tuned so one outbox "attempt" fails fast, and the
        // outbox's own 2s/4s/8s backoff schedule is what actually governs
        // retry timing, rather than being dwarfed by the producer's defaults.
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 5000);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> stringKafkaTemplate(
            ProducerFactory<String, String> stringProducerFactory) {
        return new KafkaTemplate<>(stringProducerFactory);
    }
}