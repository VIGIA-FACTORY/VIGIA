package ec.edu.uce.vigia.community.adapter.out.persistence;

import ec.edu.uce.vigia.community.domain.MemberType;
import ec.edu.uce.vigia.community.domain.Person;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "person")
class PersonEntity {

    @Id
    private UUID id;

    @Column(name = "document_number", nullable = false, unique = true)
    private String documentNumber;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_type", nullable = false)
    private MemberType memberType;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    protected PersonEntity() {}

    static PersonEntity from(Person person) {
        PersonEntity entity = new PersonEntity();
        entity.id = person.id();
        entity.documentNumber = person.documentNumber();
        entity.firstName = person.firstName();
        entity.lastName = person.lastName();
        entity.email = person.email();
        entity.memberType = person.memberType();
        entity.registeredAt = person.registeredAt();
        return entity;
    }

    Person toDomain() {
        return new Person(id, documentNumber, firstName, lastName, email, memberType, registeredAt);
    }
}
