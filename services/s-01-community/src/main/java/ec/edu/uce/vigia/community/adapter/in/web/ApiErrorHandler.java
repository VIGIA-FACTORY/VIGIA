package ec.edu.uce.vigia.community.adapter.in.web;

import ec.edu.uce.vigia.community.domain.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Business errors as RFC 9457 problem details with the error code in {@code code} (ADR-033). */
@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler(DomainException.class)
    public ProblemDetail domainError(DomainException error) {
        HttpStatus status = switch (error.code()) {
            case DUPLICATE_PERSON, DUPLICATE_PLATE -> HttpStatus.CONFLICT;
            case OWNER_NOT_FOUND -> HttpStatus.UNPROCESSABLE_CONTENT;
            case INVALID_PERSON, INVALID_PLATE -> HttpStatus.BAD_REQUEST;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, error.getMessage());
        problem.setTitle(error.code().name());
        problem.setProperty("code", error.code().name());
        return problem;
    }
}
