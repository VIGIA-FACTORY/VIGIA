package ec.edu.uce.vigia.community.domain;

import java.time.Instant;
import java.util.UUID;

/** A vehicle of a community member (US-041). The plate is unique. */
public record Vehicle(
        UUID id, Plate plate, UUID ownerId, String brand, String model, String color, Instant registeredAt) {

    public static Vehicle register(
            UUID id, String plate, UUID ownerId, String brand, String model, String color, Instant now) {
        if (ownerId == null) {
            throw new DomainException(ErrorCode.OWNER_NOT_FOUND, "The owner is required");
        }
        return new Vehicle(id, Plate.of(plate), ownerId, trim(brand), trim(model), trim(color), now);
    }

    public VehicleRegistered registeredEvent() {
        return new VehicleRegistered(id, plate.value(), ownerId, registeredAt);
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
