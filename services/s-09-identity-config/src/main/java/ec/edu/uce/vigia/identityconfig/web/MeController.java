package ec.edu.uce.vigia.identityconfig.web;

import ec.edu.uce.vigia.identityconfig.service.ProfileService;
import ec.edu.uce.vigia.identityconfig.service.SignedInUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /v1/me}: the signed-in user and their VIGIA roles (US-021, 03.08.01). */
@RestController
@RequestMapping("/v1/me")
public class MeController {

    private final ProfileService profileService;

    public MeController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public SignedInUser me(@AuthenticationPrincipal Jwt jwt) {
        return profileService.currentUser(jwt);
    }
}
