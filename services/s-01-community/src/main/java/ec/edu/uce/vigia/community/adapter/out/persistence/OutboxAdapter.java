package ec.edu.uce.vigia.community.adapter.out.persistence;

import ec.edu.uce.vigia.community.application.port.EventOutbox;
import ec.edu.uce.vigia.community.domain.DomainEvent;
import ec.edu.uce.vigia.community.domain.PersonRegistered;
import ec.edu.uce.vigia.community.domain.VehicleRegistered;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes each event as a CloudEvents JSON envelope in {@code outbox_event}, in the same transaction as the
 * change (ADR-006). Topics follow {@code vigia.<context>.<stream>.v<n>} (01.02 §4.2).
 */
@Component
class OutboxAdapter implements EventOutbox {

    static final String PEOPLE_TOPIC = "vigia.community.people.v1";
    static final String VEHICLES_TOPIC = "vigia.community.vehicles.v1";

    private final OutboxJpaRepository jpa;
    private final JsonMapper json;

    OutboxAdapter(OutboxJpaRepository jpa, JsonMapper json) {
        this.jpa = jpa;
        this.json = json;
    }

    @Override
    public void append(DomainEvent event) {
        UUID id = UUID.randomUUID();
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("specversion", "1.0");
        envelope.put("id", id.toString());
        envelope.put("source", "/s-01-community");
        envelope.put("type", "ec.edu.uce.vigia.community." + event.type());
        envelope.put("time", event.occurredAt().toString());
        envelope.put("subject", event.aggregateId().toString());
        envelope.put("data", event);
        jpa.save(new OutboxEntity(
                id,
                event.aggregateId(),
                event.type(),
                topicOf(event),
                json.writeValueAsString(envelope),
                event.occurredAt()));
    }

    static String topicOf(DomainEvent event) {
        return switch (event) {
            case PersonRegistered ignored -> PEOPLE_TOPIC;
            case VehicleRegistered ignored -> VEHICLES_TOPIC;
        };
    }
}
