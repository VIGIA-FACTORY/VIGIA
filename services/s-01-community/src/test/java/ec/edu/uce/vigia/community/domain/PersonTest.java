package ec.edu.uce.vigia.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PersonTest {

    private static final UUID ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-27T12:00:00Z");

    @Test
    void normalizesTheData() {
        Person person =
                Person.register(ID, " 1712345678 ", " Ana ", " Pérez ", " Ana@UCE.edu.ec ", MemberType.STUDENT, NOW);

        assertThat(person.documentNumber()).isEqualTo("1712345678");
        assertThat(person.firstName()).isEqualTo("Ana");
        assertThat(person.email()).isEqualTo("ana@uce.edu.ec");
        assertThat(person.registeredEvent().fullName()).isEqualTo("Ana Pérez");
        assertThat(person.registeredEvent().aggregateId()).isEqualTo(ID);
        assertThat(person.registeredEvent().type()).isEqualTo("PersonRegistered");
    }

    @Test
    void emailIsOptional() {
        assertThat(Person.register(ID, "AB123456", "Luis", "Mora", "  ", MemberType.STAFF, NOW)
                        .email())
                .isNull();
    }

    @Test
    void rejectsAShortDocument() {
        assertInvalid(() -> Person.register(ID, "123", "Ana", "Pérez", null, MemberType.STUDENT, NOW));
    }

    @Test
    void rejectsMissingNames() {
        assertInvalid(() -> Person.register(ID, "1712345678", " ", "Pérez", null, MemberType.STUDENT, NOW));
        assertInvalid(() -> Person.register(ID, "1712345678", "Ana", null, null, MemberType.STUDENT, NOW));
    }

    @Test
    void rejectsABadEmailAndAMissingType() {
        assertInvalid(() -> Person.register(ID, "1712345678", "Ana", "Pérez", "ana@", MemberType.STUDENT, NOW));
        assertInvalid(() -> Person.register(ID, "1712345678", "Ana", "Pérez", null, null, NOW));
    }

    private static void assertInvalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(DomainException.class)
                .extracting(error -> ((DomainException) error).code())
                .isEqualTo(ErrorCode.INVALID_PERSON);
    }
}
