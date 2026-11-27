package ec.edu.uce.vigia.community.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.edu.uce.vigia.community.application.port.EventOutbox;
import ec.edu.uce.vigia.community.application.port.PersonRepository;
import ec.edu.uce.vigia.community.application.port.VehicleRepository;
import ec.edu.uce.vigia.community.domain.DomainEvent;
import ec.edu.uce.vigia.community.domain.DomainException;
import ec.edu.uce.vigia.community.domain.ErrorCode;
import ec.edu.uce.vigia.community.domain.MemberType;
import ec.edu.uce.vigia.community.domain.Person;
import ec.edu.uce.vigia.community.domain.PersonRegistered;
import ec.edu.uce.vigia.community.domain.Vehicle;
import ec.edu.uce.vigia.community.domain.VehicleRegistered;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommunityServiceTest {

    private final List<Person> people = new ArrayList<>();
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final List<DomainEvent> events = new ArrayList<>();

    private final PersonRepository personRepository = new PersonRepository() {
        @Override
        public Person save(Person person) {
            people.add(person);
            return person;
        }

        @Override
        public boolean existsByDocumentNumber(String documentNumber) {
            return people.stream().anyMatch(p -> p.documentNumber().equals(documentNumber));
        }

        @Override
        public boolean existsById(UUID id) {
            return people.stream().anyMatch(p -> p.id().equals(id));
        }

        @Override
        public List<Person> findAll() {
            return List.copyOf(people);
        }
    };

    private final VehicleRepository vehicleRepository = new VehicleRepository() {
        @Override
        public Vehicle save(Vehicle vehicle) {
            vehicles.add(vehicle);
            return vehicle;
        }

        @Override
        public boolean existsByPlate(String plate) {
            return vehicles.stream().anyMatch(v -> v.plate().value().equals(plate));
        }

        @Override
        public List<Vehicle> findAll() {
            return List.copyOf(vehicles);
        }
    };

    private final EventOutbox outbox = events::add;

    private final CommunityService service = new CommunityService(
            personRepository,
            vehicleRepository,
            outbox,
            Clock.fixed(Instant.parse("2026-11-27T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void registersAPersonAndPublishesTheEvent() {
        Person person = service.registerPerson("1712345678", "Ana", "Pérez", null, MemberType.STUDENT);

        assertThat(service.people()).containsExactly(person);
        assertThat(events).singleElement().isInstanceOf(PersonRegistered.class);
    }

    @Test
    void rejectsADuplicateDocument() {
        service.registerPerson("1712345678", "Ana", "Pérez", null, MemberType.STUDENT);

        assertThatThrownBy(() -> service.registerPerson("1712345678", "Otra", "Persona", null, MemberType.STAFF))
                .isInstanceOf(DomainException.class)
                .extracting(error -> ((DomainException) error).code())
                .isEqualTo(ErrorCode.DUPLICATE_PERSON);
        assertThat(events).hasSize(1);
    }

    @Test
    void registersAVehicleOfAKnownOwner() {
        Person owner = service.registerPerson("1712345678", "Ana", "Pérez", null, MemberType.STUDENT);

        Vehicle vehicle = service.registerVehicle("pba-1234", owner.id(), "Kia", "Rio", "Rojo");

        assertThat(service.vehicles()).containsExactly(vehicle);
        assertThat(events).last().isInstanceOf(VehicleRegistered.class);
    }

    @Test
    void rejectsAVehicleOfAnUnknownOwner() {
        assertThatThrownBy(() -> service.registerVehicle("PBA-1234", UUID.randomUUID(), null, null, null))
                .extracting(error -> ((DomainException) error).code())
                .isEqualTo(ErrorCode.OWNER_NOT_FOUND);
    }

    @Test
    void rejectsADuplicatePlate() {
        Person owner = service.registerPerson("1712345678", "Ana", "Pérez", null, MemberType.STUDENT);
        service.registerVehicle("PBA-1234", owner.id(), null, null, null);

        assertThatThrownBy(() -> service.registerVehicle("pba1234", owner.id(), null, null, null))
                .extracting(error -> ((DomainException) error).code())
                .isEqualTo(ErrorCode.DUPLICATE_PLATE);
    }
}
