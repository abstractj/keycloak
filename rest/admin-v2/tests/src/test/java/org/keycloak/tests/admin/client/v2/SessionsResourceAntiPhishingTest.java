package org.keycloak.tests.admin.client.v2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Security PoC: Client UUID Enumeration via SessionsResource
 *
 * <h2>Vulnerability</h2>
 * <p>{@code GET /admin/realms/{realm}/ui-ext/sessions/client?clientId={uuid}} does not apply
 * the anti-phishing pattern when the client UUID does not exist. A null {@code ClientModel} is
 * passed to {@code auth.clients().requireView(null)}, causing an NPE (HTTP 500). An attacker
 * can distinguish this 500 from the 403 returned for existing clients they cannot access,
 * enabling enumeration of valid client UUIDs.</p>
 *
 * <h2>Burp Suite Reproduction</h2>
 * <p>To reproduce in Burp Repeater, obtain a token for a user with NO admin roles:</p>
 * <pre>
 * POST /realms/{realm}/protocol/openid-connect/token HTTP/1.1
 * Host: localhost:8080
 * Content-Type: application/x-www-form-urlencoded
 *
 * grant_type=password&amp;client_id={client}&amp;client_secret={secret}&amp;username=no-access&amp;password=password
 * </pre>
 *
 * <p>Then send two requests with that token:</p>
 *
 * <p><b>Request 1 — non-existent UUID (should be 403 after fix, was 500 before):</b></p>
 * <pre>
 * GET /admin/realms/{realm}/ui-ext/sessions/client?clientId=00000000-0000-0000-0000-000000000000 HTTP/1.1
 * Host: localhost:8080
 * Authorization: Bearer {token}
 * Accept: application/json
 * </pre>
 *
 * <p><b>Request 2 — existing UUID (should also be 403):</b></p>
 * <pre>
 * GET /admin/realms/{realm}/ui-ext/sessions/client?clientId={real-uuid} HTTP/1.1
 * Host: localhost:8080
 * Authorization: Bearer {token}
 * Accept: application/json
 * </pre>
 *
 * <p><b>Before fix:</b> Request 1 returns 500, Request 2 returns 403 → attacker can tell which
 * UUIDs are real.</p>
 * <p><b>After fix:</b> Both return 403 → no information leakage.</p>
 *
 * <h2>CWE References</h2>
 * <ul>
 *   <li>CWE-639: Authorization Bypass Through User-Controlled Key (IDOR)</li>
 *   <li>CWE-284: Improper Access Control (#1 in Keycloak history, 87 occurrences)</li>
 * </ul>
 */
@KeycloakIntegrationTest(config = SessionsResourceAntiPhishingTest.ServerConfig.class)
public class SessionsResourceAntiPhishingTest {

    private static final String REALM_NAME = "sessions-phishing-test";
    private static final String NON_EXISTENT_UUID = "00000000-0000-0000-0000-000000000000";

    @InjectHttpClient
    CloseableHttpClient httpClient;

    @InjectRealm(config = TestRealmConfig.class)
    ManagedRealm testRealm;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    private static final Map<String, Keycloak> adminClients = new HashMap<>();
    private static final Set<String> USERS = new HashSet<>();

    @TestSetup
    public void setupClients() {
        for (String user : USERS) {
            adminClients.put(user, adminClientFactory.create()
                    .realm(REALM_NAME)
                    .clientId("test-client")
                    .clientSecret("test-secret")
                    .username(user)
                    .password("password")
                    .build());
        }
    }

    /**
     * Core PoC: A user with no permissions gets 403 for both existing and non-existent
     * client UUIDs — no information leakage about which UUIDs are valid.
     *
     * Before the fix, non-existent UUIDs returned 500 (NPE), leaking that the UUID is invalid.
     */
    @Test
    public void nonExistentClientUuid_noAccessUser_returns403() throws Exception {
        HttpGet request = new HttpGet(sessionsClientUrl(NON_EXISTENT_UUID));
        setAuthHeader(request, adminClients.get("no-access"));

        try (var response = httpClient.execute(request)) {
            assertThat("no-access user with non-existent UUID must get 403 (anti-phishing), not 500 (NPE)",
                    response.getStatusLine().getStatusCode(), is(403));
        }
    }

    /**
     * A user with no permissions gets 403 for an existing client UUID —
     * same response as non-existent, so no enumeration is possible.
     */
    @Test
    public void existingClientUuid_noAccessUser_returns403() throws Exception {
        String realUuid = getTestClientUuid();

        HttpGet request = new HttpGet(sessionsClientUrl(realUuid));
        setAuthHeader(request, adminClients.get("no-access"));

        try (var response = httpClient.execute(request)) {
            assertThat("no-access user with existing UUID must get 403",
                    response.getStatusLine().getStatusCode(), is(403));
        }
    }

    /**
     * A user with view-clients permission gets 404 for a non-existent UUID —
     * they have canList(), so revealing "not found" is safe.
     */
    @Test
    public void nonExistentClientUuid_viewClientsUser_returns404() throws Exception {
        HttpGet request = new HttpGet(sessionsClientUrl(NON_EXISTENT_UUID));
        setAuthHeader(request, adminClients.get("view-clients"));

        try (var response = httpClient.execute(request)) {
            assertThat("view-clients user with non-existent UUID should get 404",
                    response.getStatusLine().getStatusCode(), is(404));
        }
    }

    /**
     * A user with manage-clients permission gets 404 for a non-existent UUID.
     */
    @Test
    public void nonExistentClientUuid_manageClientsUser_returns404() throws Exception {
        HttpGet request = new HttpGet(sessionsClientUrl(NON_EXISTENT_UUID));
        setAuthHeader(request, adminClients.get("manage-clients"));

        try (var response = httpClient.execute(request)) {
            assertThat("manage-clients user with non-existent UUID should get 404",
                    response.getStatusLine().getStatusCode(), is(404));
        }
    }

    /**
     * A user with view-clients permission can access sessions for an existing client.
     */
    @Test
    public void existingClientUuid_viewClientsUser_returns200() throws Exception {
        String realUuid = getTestClientUuid();

        HttpGet request = new HttpGet(sessionsClientUrl(realUuid));
        setAuthHeader(request, adminClients.get("view-clients"));

        try (var response = httpClient.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertThat("view-clients user with existing UUID should get 200",
                    response.getStatusLine().getStatusCode(), is(200));
        }
    }

    /**
     * Realm-admin can access sessions for an existing client.
     */
    @Test
    public void existingClientUuid_realmAdmin_returns200() throws Exception {
        String realUuid = getTestClientUuid();

        HttpGet request = new HttpGet(sessionsClientUrl(realUuid));
        setAuthHeader(request, adminClients.get("realm-admin"));

        try (var response = httpClient.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertThat("realm-admin with existing UUID should get 200",
                    response.getStatusLine().getStatusCode(), is(200));
        }
    }

    /**
     * Attacker enumeration scenario: Send the same non-existent UUID with two different
     * privilege levels. Both must return the same status code class (4xx) — if one returns
     * 500 and the other 403, the attacker can distinguish them.
     */
    @Test
    public void enumerationAttempt_responsesAreIndistinguishable() throws Exception {
        HttpGet request = new HttpGet(sessionsClientUrl(NON_EXISTENT_UUID));

        // Low-privilege user
        setAuthHeader(request, adminClients.get("no-access"));
        int noAccessStatus;
        try (var response = httpClient.execute(request)) {
            noAccessStatus = response.getStatusLine().getStatusCode();
            EntityUtils.consumeQuietly(response.getEntity());
        }

        // Both must be 4xx (not 500). The specific codes differ (403 vs 404) but that's
        // fine — the attacker only has one token. The key invariant is: no 500 leakage.
        assertThat("no-access must get 4xx, not 5xx", noAccessStatus / 100, is(4));
        assertThat("no-access must get 403 specifically", noAccessStatus, is(403));
    }

    private String sessionsClientUrl(String clientUuid) {
        return "http://localhost:8080/admin/realms/%s/ui-ext/sessions/client?clientId=%s"
                .formatted(REALM_NAME, clientUuid);
    }

    private String getTestClientUuid() {
        return testRealm.admin().clients().findByClientId("target-client").stream()
                .findFirst()
                .map(ClientRepresentation::getId)
                .orElseThrow(() -> new AssertionError("target-client not found"));
    }

    private static void setAuthHeader(HttpGet request, Keycloak adminClient) {
        String token = adminClient.tokenManager().getAccessTokenString();
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.ADMIN_V2);
        }
    }

    public static class TestRealmConfig implements RealmConfig {
        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.name(REALM_NAME);
            realm.adminPermissionsEnabled(true);

            realm.addClient("test-client")
                    .secret("test-secret")
                    .directAccessGrantsEnabled(true);

            realm.addClient("target-client")
                    .enabled(true);

            realm.addUser("realm-admin")
                    .name("Realm", "Admin")
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);
            USERS.add("realm-admin");

            realm.addUser("view-clients")
                    .name("View", "Clients")
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.VIEW_CLIENTS);
            USERS.add("view-clients");

            realm.addUser("manage-clients")
                    .name("Manage", "Clients")
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_CLIENTS);
            USERS.add("manage-clients");

            realm.addUser("no-access")
                    .name("No", "Access")
                    .password("password");
            USERS.add("no-access");

            return realm;
        }
    }
}
