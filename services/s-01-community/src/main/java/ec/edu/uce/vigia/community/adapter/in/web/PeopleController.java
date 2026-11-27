package ec.edu.uce.vigia.community.adapter.in.web;

import ec.edu.uce.vigia.community.application.CommunityService;
import ec.edu.uce.vigia.community.domain.MemberType;
import ec.edu.uce.vigia.community.domain.Person;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** {@code /v1/people}: register and list community members (US-031). */
@RestController
@RequestMapping("/v1/people")
public class PeopleController {

    private final CommunityService service;

    public PeopleController(CommunityService service) {
        this.service = service;
    }

    public record RegisterPersonRequest(
            @NotBlank String documentNumber,
            @NotBlank String firstName,
            @NotBlank String lastName,
            String email,
            @NotNull MemberType memberType) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Person register(@Valid @RequestBody RegisterPersonRequest request) {
        return service.registerPerson(
                request.documentNumber(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.memberType());
    }

    @GetMapping
    public List<Person> list() {
        return service.people();
    }
}
