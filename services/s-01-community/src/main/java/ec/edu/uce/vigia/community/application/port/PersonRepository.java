package ec.edu.uce.vigia.community.application.port;

import ec.edu.uce.vigia.community.domain.Person;
import java.util.List;
import java.util.UUID;

public interface PersonRepository {

    Person save(Person person);

    boolean existsByDocumentNumber(String documentNumber);

    boolean existsById(UUID id);

    List<Person> findAll();
}
