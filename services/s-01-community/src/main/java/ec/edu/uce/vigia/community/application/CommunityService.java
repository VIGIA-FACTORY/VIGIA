package ec.edu.uce.vigia.community.application;

import ec.edu.uce.vigia.community.application.port.EventOutbox;
import ec.edu.uce.vigia.community.application.port.PersonRepository;
import ec.edu.uce.vigia.community.application.port.VehicleRepository;
import ec.edu.uce.vigia.community.domain.DomainException;
import ec.edu.uce.vigia.community.domain.ErrorCode;
import ec.edu.uce.vigia.community.domain.MemberType;
import ec.edu.uce.vigia.community.domain.Person;
import ec.edu.uce.vigia.community.domain.Vehicle;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Use cases of the MVP: register and list people (US-031) and vehicles (US-041). */
public class CommunityService {

    private final PersonRepository people;
    private final VehicleRepository vehicles;
    private final EventOutbox outbox;
    private final Clock clock;

    public CommunityService(PersonRepository people, VehicleRepository vehicles, EventOutbox outbox, Clock clock) {
        this.people = people;
        this.vehicles = vehicles;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    public Person registerPerson(
            String documentNumber, String firstName, String lastName, String email, MemberType memberType) {
        Person person = Person.register(
                UUID.randomUUID(), documentNumber, firstName, lastName, email, memberType, clock.instant());
        if (people.existsByDocumentNumber(person.documentNumber())) {
            throw new DomainException(ErrorCode.DUPLICATE_PERSON, "A person with this document is already registered");
        }
        Person saved = people.save(person);
        outbox.append(saved.registeredEvent());
        return saved;
    }

    @Transactional
    public Vehicle registerVehicle(String plate, UUID ownerId, String brand, String model, String color) {
        Vehicle vehicle = Vehicle.register(UUID.randomUUID(), plate, ownerId, brand, model, color, clock.instant());
        if (!people.existsById(ownerId)) {
            throw new DomainException(ErrorCode.OWNER_NOT_FOUND, "The owner is not a registered person");
        }
        if (vehicles.existsByPlate(vehicle.plate().value())) {
            throw new DomainException(ErrorCode.DUPLICATE_PLATE, "A vehicle with this plate is already registered");
        }
        Vehicle saved = vehicles.save(vehicle);
        outbox.append(saved.registeredEvent());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Person> people() {
        return people.findAll();
    }

    @Transactional(readOnly = true)
    public List<Vehicle> vehicles() {
        return vehicles.findAll();
    }
}
