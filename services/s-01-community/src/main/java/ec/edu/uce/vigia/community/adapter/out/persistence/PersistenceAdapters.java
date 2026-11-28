package ec.edu.uce.vigia.community.adapter.out.persistence;

import ec.edu.uce.vigia.community.application.port.PersonRepository;
import ec.edu.uce.vigia.community.application.port.VehicleRepository;
import ec.edu.uce.vigia.community.domain.Person;
import ec.edu.uce.vigia.community.domain.Vehicle;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** PostgreSQL implementation of the repository ports (database {@code community}, ADR-003). */
@Component
class PersonPersistenceAdapter implements PersonRepository {

    private final PersonJpaRepository jpa;

    PersonPersistenceAdapter(PersonJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Person save(Person person) {
        return jpa.save(PersonEntity.from(person)).toDomain();
    }

    @Override
    public boolean existsByDocumentNumber(String documentNumber) {
        return jpa.existsByDocumentNumber(documentNumber);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpa.existsById(id);
    }

    @Override
    public List<Person> findAll() {
        return jpa.findAllByOrderByLastNameAscFirstNameAsc().stream()
                .map(PersonEntity::toDomain)
                .toList();
    }
}

@Component
class VehiclePersistenceAdapter implements VehicleRepository {

    private final VehicleJpaRepository jpa;

    VehiclePersistenceAdapter(VehicleJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Vehicle save(Vehicle vehicle) {
        return jpa.save(VehicleEntity.from(vehicle)).toDomain();
    }

    @Override
    public boolean existsByPlate(String plate) {
        return jpa.existsByPlate(plate);
    }

    @Override
    public List<Vehicle> findAll() {
        return jpa.findAllByOrderByPlateAsc().stream()
                .map(VehicleEntity::toDomain)
                .toList();
    }
}
