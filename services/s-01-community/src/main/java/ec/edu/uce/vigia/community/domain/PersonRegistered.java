package ec.edu.uce.vigia.community.domain;

import java.time.Instant;
import java.util.UUID;

public record PersonRegistered(
        UUID personId, String documentNumber, String fullName, MemberType memberType, Instant occurredAt)
        implements DomainEvent {

    @Override
    public UUID aggregateId() {
        return personId;
    }
}
