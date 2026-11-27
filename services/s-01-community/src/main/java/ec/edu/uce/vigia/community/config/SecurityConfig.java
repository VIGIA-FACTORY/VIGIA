package ec.edu.uce.vigia.community.config;

import ec.edu.uce.vigia.security.VigiaJwtAuthenticationConverter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * S-01 is an OAuth2 resource server: it validates the Keycloak access token (issuer and JWKS, ADR-050) and
 * reads the roles with the converter of libs/security. Health probes, metrics and the API documentation are
 * public. Registering people and vehicles needs ADMIN; reading them needs ADMIN, SUPERVISOR or GUARD
 * (03.08.04.01, MVP subset).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests.requestMatchers(
                                "/actuator/health/**", "/actuator/prometheus", "/v3/api-docs/**", "/swagger-ui/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/v1/**")
                        .hasAnyRole("ADMIN", "SUPERVISOR", "GUARD")
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(server ->
                        server.jwt(jwt -> jwt.jwtAuthenticationConverter(new VigiaJwtAuthenticationConverter())));
        return http.build();
    }

    /** Origins of the web clients (CORS allow-list, PR-05), from VIGIA_CORS_ORIGINS. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${vigia.cors.origins}") List<String> origins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Correlation-Id"));
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
