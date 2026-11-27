package ec.edu.uce.vigia.community.domain;

/** A business rule of the Community context was broken; the code is the API error code (ADR-033). */
public class DomainException extends RuntimeException {

    private final ErrorCode code;

    public DomainException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
