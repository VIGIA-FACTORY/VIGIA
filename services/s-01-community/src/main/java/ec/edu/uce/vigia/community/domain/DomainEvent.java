package ec.edu.uce.vigia.community.domain;

import java.time.Instant;
import java.util.UUID;

/** An event of the Community context, published through the outbox (ADR-006). */
public sealed interface DomainEvent permits PersonRegistered, VehicleRegistered {

    /** Identifier of the aggregate, used as the Kafka key. */
    UUID aggregateId();

    Instant occurredAt();

    /** Event type, PascalCase in past tense (01.02 §4.2). */
    default String type() {
        return getClass().getSimpleName();
    }
}
