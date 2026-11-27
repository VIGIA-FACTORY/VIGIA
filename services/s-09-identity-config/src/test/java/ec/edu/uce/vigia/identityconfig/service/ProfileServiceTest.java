package ec.edu.uce.vigia.identityconfig.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.edu.uce.vigia.identityconfig.model.UserProfile;
import ec.edu.uce.vigia.identityconfig.repository.UserProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class ProfileServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-27T12:00:00Z");

    private final UserProfileRepository repository = mock(UserProfileRepository.class);
    private final ProfileService service = new ProfileService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    private static Jwt token() {
        return Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .subject("sub-1")
                .claim("preferred_username", "guard1")
                .claim("email", "guard1@vigia.test")
                .claim("name", "Guard One")
                .claim("vigia:role", List.of("GUARD"))
                .claim("vigia:gatehouse", "G-01")
                .build();
    }

    @Test
    void createsTheProfileOnTheFirstSignIn() {
        when(repository.findById("sub-1")).thenReturn(Optional.empty());
        when(repository.save(any(UserProfile.class))).thenAnswer(call -> call.getArgument(0));

        SignedInUser user = service.currentUser(token());

        assertThat(user.subject()).isEqualTo("sub-1");
        assertThat(user.username()).isEqualTo("guard1");
        assertThat(user.roles()).containsExactly("GUARD");
        assertThat(user.gatehouse()).isEqualTo("G-01");
        assertThat(user.firstSeenAt()).isEqualTo(NOW);
    }

    @Test
    void keepsTheFirstSignInDateOfAKnownUser() {
        Instant earlier = Instant.parse("2026-11-01T08:00:00Z");
        when(repository.findById("sub-1")).thenReturn(Optional.of(new UserProfile("sub-1", earlier)));
        when(repository.save(any(UserProfile.class))).thenAnswer(call -> call.getArgument(0));

        SignedInUser user = service.currentUser(token());

        assertThat(user.firstSeenAt()).isEqualTo(earlier);
        assertThat(user.email()).isEqualTo("guard1@vigia.test");
    }
}
