package ec.edu.uce.vigia.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Checks the realm as code of US-219 (infra/identity/realm/vigia-realm.json, ADR-050) in a real Keycloak: it is
 * imported with no manual step (K4, local part), publishes its discovery document and RS256 keys, and issues
 * tokens with the claims the services read: {@code vigia:role}, {@code vigia:gatehouse} and the audience
 * {@code vigia-api}, valid for 15 minutes (03.08.04.01).
 */
@Tag("integration")
@Testcontainers
class VigiaRealmIntegrationTest {

    private static final String N8N_SECRET = "test-only-n8n";

    @Container
    static final KeycloakContainer KEYCLOAK = new KeycloakContainer("quay.io/keycloak/keycloak:26.8")
            .withRealmImportFile("/vigia-realm.json")
            .withEnv("VIGIA_ADMIN_URL", "http://localhost:5173")
            .withEnv("VIGIA_GATE_URL", "http://localhost:5174")
            .withEnv("VIGIA_PORTAL_URL", "http://localhost:5175")
            .withEnv("VIGIA_S09_CLIENT_SECRET", "test-only-s09")
            .withEnv("VIGIA_S01_CLIENT_SECRET", "test-only-s01")
            .withEnv("VIGIA_N8N_CLIENT_SECRET", N8N_SECRET)
            .withEnv("VIGIA_ENTRA_TENANT_ID", "00000000-0000-0000-0000-000000000000")
            .withEnv("VIGIA_ENTRA_CLIENT_ID", "not-configured")
            .withEnv("VIGIA_ENTRA_CLIENT_SECRET", "not-configured");

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static String realmUrl;

    @BeforeAll
    static void createGuard() {
        realmUrl = KEYCLOAK.getAuthServerUrl() + "/realms/vigia";
        RealmResource realm = KEYCLOAK.getKeycloakAdminClient().realm("vigia");

        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue("Guard-test-only-2026");
        password.setTemporary(false);
        UserRepresentation guard = new UserRepresentation();
        guard.setUsername("guard1");
        guard.setEmail("guard1@vigia.test");
        guard.setFirstName("Guard");
        guard.setLastName("One");
        guard.setEnabled(true);
        guard.setEmailVerified(true);
        guard.setAttributes(Map.of("gatehouse", List.of("north")));
        guard.setCredentials(List.of(password));
        realm.users().create(guard).close();

        String guardId =
                realm.users().searchByUsername("guard1", true).getFirst().getId();
        String apiId = realm.clients().findByClientId("vigia-api").getFirst().getId();
        var role = realm.clients().get(apiId).roles().get("GUARD").toRepresentation();
        realm.users().get(guardId).roles().clientLevel(apiId).add(List.of(role));

        // Only for this test: the gate client uses the authorization code flow with PKCE in real use.
        ClientRepresentation gate = realm.clients().findByClientId("vigia-gate").getFirst();
        gate.setDirectAccessGrantsEnabled(true);
        realm.clients().get(gate.getId()).update(gate);
    }

    @Test
    void publishesDiscoveryAndRs256SigningKey() throws Exception {
        String discovery = get(realmUrl + "/.well-known/openid-configuration");
        assertThat(JsonPath.<String>read(discovery, "$.issuer")).isEqualTo(realmUrl);

        String jwks = get(JsonPath.read(discovery, "$.jwks_uri"));
        List<String> signingAlgorithms = JsonPath.read(jwks, "$.keys[?(@.use == 'sig')].alg");
        assertThat(signingAlgorithms).contains("RS256");
    }

    @Test
    void guardTokenCarriesRoleGatehouseAudienceAndShortLifetime() throws Exception {
        String response = token(Map.of(
                "grant_type", "password",
                "client_id", "vigia-gate",
                "username", "guard1",
                "password", "Guard-test-only-2026"));
        String accessToken = JsonPath.read(response, "$.access_token");

        String[] parts = accessToken.split("\\.");
        String header = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        String claims = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

        assertThat(JsonPath.<String>read(header, "$.alg")).isEqualTo("RS256");
        assertThat(JsonPath.<List<String>>read(claims, "$['vigia:role']")).containsExactly("GUARD");
        assertThat(JsonPath.<String>read(claims, "$['vigia:gatehouse']")).isEqualTo("north");
        assertThat(JsonPath.<List<String>>read(claims, "$.aud")).contains("vigia-api");
        long lifetime = JsonPath.<Number>read(claims, "$.exp").longValue()
                - JsonPath.<Number>read(claims, "$.iat").longValue();
        assertThat(lifetime).isEqualTo(900);
    }

    @Test
    void machineClientGetsTokenOnlyWithItsSecret() throws Exception {
        String ok = token(
                Map.of("grant_type", "client_credentials", "client_id", "vigia-n8n", "client_secret", N8N_SECRET));
        assertThat(JsonPath.<String>read(ok, "$.access_token")).isNotBlank();

        HttpResponse<String> wrong =
                post(Map.of("grant_type", "client_credentials", "client_id", "vigia-n8n", "client_secret", "wrong"));
        assertThat(wrong.statusCode()).isEqualTo(401);
    }

    @Test
    void publicClientsRequirePkceAndEntraIdIsDeclared() {
        RealmResource realm = KEYCLOAK.getKeycloakAdminClient().realm("vigia");
        for (String clientId : List.of("vigia-admin", "vigia-portal", "vigia-mobile", "vigia-desktop")) {
            ClientRepresentation client =
                    realm.clients().findByClientId(clientId).getFirst();
            assertThat(client.isPublicClient()).as(clientId).isTrue();
            assertThat(client.getAttributes()).as(clientId).containsEntry("pkce.code.challenge.method", "S256");
            assertThat(client.isDirectAccessGrantsEnabled()).as(clientId).isFalse();
        }
        assertThat(realm.identityProviders().get("entra-id").toRepresentation().getProviderId())
                .isEqualTo("oidc");
    }

    private static String token(Map<String, String> form) throws Exception {
        HttpResponse<String> response = post(form);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return response.body();
    }

    private static HttpResponse<String> post(Map<String, String> form) throws Exception {
        String body = form.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder(URI.create(realmUrl + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }
}
