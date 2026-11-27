package ec.edu.uce.vigia.community.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.edu.uce.vigia.community.application.CommunityService;
import ec.edu.uce.vigia.community.config.SecurityConfig;
import ec.edu.uce.vigia.community.domain.DomainException;
import ec.edu.uce.vigia.community.domain.ErrorCode;
import ec.edu.uce.vigia.community.domain.MemberType;
import ec.edu.uce.vigia.community.domain.Person;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({PeopleController.class, ApiErrorHandler.class})
@Import(SecurityConfig.class)
class PeopleControllerTest {

    private static final String BODY = "{\"documentNumber\":\"1712345678\",\"firstName\":\"Ana\","
            + "\"lastName\":\"Pérez\",\"memberType\":\"STUDENT\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CommunityService service;

    @Test
    void anAdministratorRegistersAPerson() throws Exception {
        when(service.registerPerson(any(), any(), any(), any(), any()))
                .thenReturn(new Person(
                        UUID.randomUUID(), "1712345678", "Ana", "Pérez", null, MemberType.STUDENT, Instant.EPOCH));

        mvc.perform(post("/v1/people")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentNumber").value("1712345678"));
    }

    @Test
    void aDuplicateDocumentIsAConflictWithItsCode() throws Exception {
        when(service.registerPerson(any(), any(), any(), any(), any()))
                .thenThrow(new DomainException(ErrorCode.DUPLICATE_PERSON, "duplicate"));

        mvc.perform(post("/v1/people")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_PERSON"));
    }

    @Test
    void aGuardCannotRegisterButCanList() throws Exception {
        when(service.people()).thenReturn(List.of());

        mvc.perform(post("/v1/people")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_GUARD")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(get("/v1/people").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_GUARD"))))
                .andExpect(status().isOk());
    }

    @Test
    void withoutTokenTheRequestIsRejected() throws Exception {
        mvc.perform(get("/v1/people")).andExpect(status().isUnauthorized());
    }
}
