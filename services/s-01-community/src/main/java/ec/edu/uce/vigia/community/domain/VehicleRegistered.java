package ec.edu.uce.vigia.community.domain;

import java.time.Instant;
import java.util.UUID;

public record VehicleRegistered(UUID vehicleId, String plate, UUID ownerId, Instant occurredAt) implements DomainEvent {

    @Override
    public UUID aggregateId() {
        return vehicleId;
    }
}
