package ec.edu.uce.vigia.community.adapter.out.messaging;

import ec.edu.uce.vigia.community.adapter.out.persistence.OutboxEntity;
import ec.edu.uce.vigia.community.adapter.out.persistence.OutboxJpaRepository;
import java.time.Clock;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publishes pending outbox events: the domain event to Kafka (ADR-039) and a copy to the RabbitMQ queue
 * {@code automation.n8n}, where n8n starts its workflows (ADR-019). Delivery is at least once (ADR-006): an
 * event is marked published only after both sends are acknowledged, and consumers discard duplicates by id.
 */
@Component
@ConditionalOnProperty(name = "vigia.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
class OutboxRelay {

    static final String N8N_QUEUE = "automation.n8n";
    private static final Logger LOG = LoggerFactory.getLogger(OutboxRelay.class);
    private static final long SEND_TIMEOUT_SECONDS = 5;

    private final OutboxJpaRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final RabbitTemplate rabbit;
    private final Clock clock;

    OutboxRelay(OutboxJpaRepository outbox, KafkaTemplate<String, String> kafka, RabbitTemplate rabbit, Clock clock) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.rabbit = rabbit;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${vigia.outbox.relay.delay-ms:1000}")
    @Transactional
    public void publishPending() {
        for (OutboxEntity event : outbox.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                kafka.send(event.getTopic(), event.getAggregateId().toString(), event.getPayload())
                        .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                rabbit.convertAndSend("", N8N_QUEUE, event.getPayload(), message -> {
                    message.getMessageProperties().setMessageId(event.getId().toString());
                    message.getMessageProperties().setContentType("application/json");
                    message.getMessageProperties().setType(event.getEventType());
                    return message;
                });
                event.markPublished(clock.instant());
            } catch (Exception failure) {
                LOG.warn("Outbox event {} not published yet: {}", event.getId(), failure.getMessage());
                return; // keep the order; the next run retries from this event
            }
        }
    }
}
