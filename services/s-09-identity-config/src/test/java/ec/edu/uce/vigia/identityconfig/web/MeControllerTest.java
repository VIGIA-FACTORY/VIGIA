package ec.edu.uce.vigia.identityconfig.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.edu.uce.vigia.identityconfig.config.SecurityConfig;
import ec.edu.uce.vigia.identityconfig.service.ProfileService;
import ec.edu.uce.vigia.identityconfig.service.SignedInUser;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MeController.class)
@Import(SecurityConfig.class)
class MeControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProfileService profileService;

    @Test
    void answersTheSignedInUserWithTheirRoles() throws Exception {
        when(profileService.currentUser(any()))
                .thenReturn(new SignedInUser(
                        "sub-1", "owner1", null, null, List.of("OWNER", "DRIVER"), null, Instant.EPOCH));

        mvc.perform(get("/v1/me").with(jwt().jwt(token -> token.subject("sub-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("owner1"))
                .andExpect(jsonPath("$.roles[1]").value("DRIVER"));
    }

    @Test
    void rejectsARequestWithoutToken() throws Exception {
        mvc.perform(get("/v1/me")).andExpect(status().isUnauthorized());
    }
}
