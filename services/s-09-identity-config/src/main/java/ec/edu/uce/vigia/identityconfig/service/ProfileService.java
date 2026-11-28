package ec.edu.uce.vigia.identityconfig.service;

import ec.edu.uce.vigia.identityconfig.model.UserProfile;
import ec.edu.uce.vigia.identityconfig.repository.UserProfileRepository;
import ec.edu.uce.vigia.security.VigiaClaims;
import ec.edu.uce.vigia.security.VigiaJwtAuthenticationConverter;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Records each signed-in user and answers who they are and which roles they hold (US-021). */
@Service
public class ProfileService {

    private final UserProfileRepository repository;
    private final Clock clock;

    public ProfileService(UserProfileRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public SignedInUser currentUser(Jwt jwt) {
        Instant now = clock.instant();
        List<String> roles = VigiaJwtAuthenticationConverter.roles(jwt);
        String username =
                jwt.hasClaim("preferred_username") ? jwt.getClaimAsString("preferred_username") : jwt.getSubject();
        UserProfile profile =
                repository.findById(jwt.getSubject()).orElseGet(() -> new UserProfile(jwt.getSubject(), now));
        profile.refresh(
                username, jwt.getClaimAsString("email"), jwt.getClaimAsString("name"), String.join(",", roles), now);
        repository.save(profile);
        return new SignedInUser(
                profile.getSubject(),
                profile.getUsername(),
                profile.getEmail(),
                profile.getDisplayName(),
                roles,
                jwt.getClaimAsString(VigiaClaims.GATEHOUSE),
                profile.getFirstSeenAt());
    }
}
