package ec.edu.uce.vigia.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class VigiaJwtAuthenticationConverterTest {

    private final VigiaJwtAuthenticationConverter converter = new VigiaJwtAuthenticationConverter();

    private static Jwt token(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("3f6c0d1e")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900));
        claims.forEach(builder::claim);
        return builder.build();
    }

    private static List<String> authorities(org.springframework.security.core.Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    @Test
    void mapsEveryRoleOfTheListClaim() {
        var authentication = converter.convert(
                token(Map.of(VigiaClaims.ROLE, List.of("OWNER", "DRIVER"), "preferred_username", "owner1")));

        assertThat(authorities(authentication)).containsExactly("ROLE_OWNER", "ROLE_DRIVER");
        assertThat(authentication.getName()).isEqualTo("owner1");
    }

    @Test
    void acceptsASingleStringAndNormalizesIt() {
        var authentication = converter.convert(token(Map.of(VigiaClaims.ROLE, " guard ")));

        assertThat(authorities(authentication)).containsExactly("ROLE_GUARD");
        assertThat(authentication.getName()).isEqualTo("3f6c0d1e");
    }

    @Test
    void givesNoAuthorityWithoutTheClaim() {
        var authentication = converter.convert(token(Map.of("scope", "openid")));

        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    void dropsDuplicatesAndBlankValues() {
        assertThat(VigiaJwtAuthenticationConverter.roles(
                        token(Map.of(VigiaClaims.ROLE, List.of("ADMIN", "admin", "", "SUPERVISOR")))))
                .containsExactly("ADMIN", "SUPERVISOR");
    }
}
