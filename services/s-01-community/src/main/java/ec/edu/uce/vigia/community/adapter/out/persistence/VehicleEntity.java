package ec.edu.uce.vigia.community.adapter.out.persistence;

import ec.edu.uce.vigia.community.domain.Plate;
import ec.edu.uce.vigia.community.domain.Vehicle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vehicle")
class VehicleEntity {

    @Id
    private UUID id;

    @Column(name = "plate", nullable = false, unique = true)
    private String plate;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "brand")
    private String brand;

    @Column(name = "model")
    private String model;

    @Column(name = "color")
    private String color;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    protected VehicleEntity() {}

    static VehicleEntity from(Vehicle vehicle) {
        VehicleEntity entity = new VehicleEntity();
        entity.id = vehicle.id();
        entity.plate = vehicle.plate().value();
        entity.ownerId = vehicle.ownerId();
        entity.brand = vehicle.brand();
        entity.model = vehicle.model();
        entity.color = vehicle.color();
        entity.registeredAt = vehicle.registeredAt();
        return entity;
    }

    Vehicle toDomain() {
        return new Vehicle(id, new Plate(plate), ownerId, brand, model, color, registeredAt);
    }
}
