package ec.edu.uce.vigia.community.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** A member of the university community (US-031). The document number is unique among active members. */
public record Person(
        UUID id,
        String documentNumber,
        String firstName,
        String lastName,
        String email,
        MemberType memberType,
        Instant registeredAt) {

    private static final Pattern DOCUMENT = Pattern.compile("[A-Z0-9]{6,20}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    /** Validates and normalizes the data of a new member. */
    public static Person register(
            UUID id,
            String documentNumber,
            String firstName,
            String lastName,
            String email,
            MemberType memberType,
            Instant now) {
        String document = normalize(documentNumber).toUpperCase(Locale.ROOT);
        if (!DOCUMENT.matcher(document).matches()) {
            throw new DomainException(
                    ErrorCode.INVALID_PERSON, "The document number must have 6 to 20 letters or digits");
        }
        String first = required(firstName, "first name");
        String last = required(lastName, "last name");
        String mail = normalize(email).toLowerCase(Locale.ROOT);
        if (!mail.isEmpty() && !EMAIL.matcher(mail).matches()) {
            throw new DomainException(ErrorCode.INVALID_PERSON, "The email is not valid");
        }
        if (memberType == null) {
            throw new DomainException(ErrorCode.INVALID_PERSON, "The member type is required");
        }
        return new Person(id, document, first, last, mail.isEmpty() ? null : mail, memberType, now);
    }

    public PersonRegistered registeredEvent() {
        return new PersonRegistered(id, documentNumber, firstName + " " + lastName, memberType, registeredAt);
    }

    private static String required(String value, String field) {
        String text = normalize(value);
        if (text.isEmpty()) {
            throw new DomainException(ErrorCode.INVALID_PERSON, "The " + field + " is required");
        }
        return text;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
