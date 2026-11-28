package ec.edu.uce.vigia.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VehicleTest {

    private static final Instant NOW = Instant.parse("2026-11-27T12:00:00Z");

    @Test
    void registersWithANormalizedPlateAndRaisesItsEvent() {
        UUID owner = UUID.randomUUID();
        Vehicle vehicle = Vehicle.register(UUID.randomUUID(), "pba1234", owner, " Kia ", "Rio", "", NOW);

        assertThat(vehicle.plate().value()).isEqualTo("PBA-1234");
        assertThat(vehicle.brand()).isEqualTo("Kia");
        assertThat(vehicle.color()).isNull();
        assertThat(vehicle.registeredEvent().plate()).isEqualTo("PBA-1234");
        assertThat(vehicle.registeredEvent().ownerId()).isEqualTo(owner);
        assertThat(vehicle.registeredEvent().aggregateId()).isEqualTo(vehicle.id());
    }

    @Test
    void needsAnOwner() {
        assertThatThrownBy(() -> Vehicle.register(UUID.randomUUID(), "PBA-1234", null, null, null, null, NOW))
                .isInstanceOf(DomainException.class)
                .extracting(error -> ((DomainException) error).code())
                .isEqualTo(ErrorCode.OWNER_NOT_FOUND);
    }
}
