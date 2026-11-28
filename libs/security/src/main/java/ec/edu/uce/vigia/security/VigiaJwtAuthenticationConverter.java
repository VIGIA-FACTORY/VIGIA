package ec.edu.uce.vigia.security;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Maps the claim {@code vigia:role} of a Keycloak access token to Spring authorities {@code ROLE_<ROLE>}
 * (ADR-050, US-021). The claim is a list because a person can hold several roles, for example OWNER and
 * DRIVER (03.08.04.01 §2); a single string is accepted as well. The principal name is the
 * {@code preferred_username} claim, or the subject when it is missing.
 */
public final class VigiaJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String name =
                jwt.hasClaim("preferred_username") ? jwt.getClaimAsString("preferred_username") : jwt.getSubject();
        return new JwtAuthenticationToken(jwt, authorities(jwt), name);
    }

    /** VIGIA roles of the token, upper case, without duplicates and in the order of the claim. */
    public static List<String> roles(Jwt jwt) {
        Object claim = jwt.getClaims().get(VigiaClaims.ROLE);
        List<?> values;
        if (claim instanceof Collection<?> collection) {
            values = List.copyOf(collection);
        } else if (claim instanceof String text) {
            values = List.of(text);
        } else {
            values = List.of();
        }
        return values.stream()
                .map(String::valueOf)
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> role.toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private static Collection<GrantedAuthority> authorities(Jwt jwt) {
        return roles(jwt).stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(VigiaClaims.AUTHORITY_PREFIX + role))
                .toList();
    }
}
