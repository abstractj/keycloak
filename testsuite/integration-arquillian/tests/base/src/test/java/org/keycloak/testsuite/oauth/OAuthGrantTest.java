/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.oauth;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AccountApplicationsPage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.ProtocolMapperUtil;
import org.keycloak.testsuite.util.RoleBuilder;
import org.openqa.selenium.By;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

/**
 * @author <a href="mailto:vrockai@redhat.com">Viliam Rockai</a>
 */
public class OAuthGrantTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected OAuthGrantPage grantPage;
    @Page
    protected AccountApplicationsPage accountAppsPage;
    @Page
    protected AppPage appPage;

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realmRepresentation);
    }

    private static String ROLE_USER = "Have User privileges";
    private static String ROLE_CUSTOMER = "Have Customer User privileges";

    @Test
    public void oauthGrantAcceptTest() {
        oauth.clientId("third-party");
        oauth.doLoginGrant("test-user@localhost", "password");

        grantPage.assertCurrent();
        Assert.assertTrue(driver.getPageSource().contains(ROLE_USER));
        Assert.assertTrue(driver.getPageSource().contains(ROLE_CUSTOMER));

        grantPage.accept();

        Assert.assertTrue(oauth.getCurrentQuery().containsKey(OAuth2Constants.CODE));

        EventRepresentation loginEvent = events.expectLogin()
                .client("third-party")
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String sessionId = loginEvent.getSessionId();

        OAuthClient.AccessTokenResponse accessToken = oauth.doAccessTokenRequest(oauth.getCurrentQuery().get(OAuth2Constants.CODE), "password");

        String tokenString = accessToken.getAccessToken();
        Assert.assertNotNull(tokenString);
        AccessToken token = oauth.verifyToken(tokenString);
        assertEquals(sessionId, token.getSessionState());

        AccessToken.Access realmAccess = token.getRealmAccess();
        assertEquals(1, realmAccess.getRoles().size());
        Assert.assertTrue(realmAccess.isUserInRole("user"));

        Map<String, AccessToken.Access> resourceAccess = token.getResourceAccess();
        assertEquals(1, resourceAccess.size());
        assertEquals(1, resourceAccess.get("test-app").getRoles().size());
        Assert.assertTrue(resourceAccess.get("test-app").isUserInRole("customer-user"));

        events.expectCodeToToken(codeId, loginEvent.getSessionId()).client("third-party").assertEvent();

        accountAppsPage.open();

        assertEquals(1, driver.findElements(By.id("revoke-third-party")).size());

        accountAppsPage.revokeGrant("third-party");

        events.expect(EventType.REVOKE_GRANT)
                .client("account").detail(Details.REVOKED_CLIENT, "third-party").assertEvent();

        assertEquals(0, driver.findElements(By.id("revoke-third-party")).size());
    }

    @Test
    public void oauthGrantCancelTest() {
        oauth.clientId("third-party");
        oauth.doLoginGrant("test-user@localhost", "password");

        grantPage.assertCurrent();
        Assert.assertTrue(driver.getPageSource().contains(ROLE_USER));
        Assert.assertTrue(driver.getPageSource().contains(ROLE_CUSTOMER));

        grantPage.cancel();

        Assert.assertTrue(oauth.getCurrentQuery().containsKey(OAuth2Constants.ERROR));
        assertEquals("access_denied", oauth.getCurrentQuery().get(OAuth2Constants.ERROR));

        events.expectLogin()
                .client("third-party")
                .error("rejected_by_user")
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }

    @Test
    public void oauthGrantNotShownWhenAlreadyGranted() {
        // Grant permissions on grant screen
        oauth.clientId("third-party");
        oauth.doLoginGrant("test-user@localhost", "password");

        grantPage.assertCurrent();
        grantPage.accept();

        events.expectLogin()
                .client("third-party")
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        // Assert permissions granted on Account mgmt. applications page
        accountAppsPage.open();
        AccountApplicationsPage.AppEntry thirdPartyEntry = accountAppsPage.getApplications().get("third-party");
        Assert.assertTrue(thirdPartyEntry.getRolesGranted().contains(ROLE_USER));
        Assert.assertTrue(thirdPartyEntry.getRolesGranted().contains("Have Customer User privileges in test-app"));
        Assert.assertTrue(thirdPartyEntry.getProtocolMappersGranted().contains("Full name"));
        Assert.assertTrue(thirdPartyEntry.getProtocolMappersGranted().contains("Email"));

        // Open login form and assert grantPage not shown
        oauth.openLoginForm();
        appPage.assertCurrent();
        events.expectLogin()
                .detail(Details.AUTH_METHOD, OIDCLoginProtocol.LOGIN_PROTOCOL)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_PERSISTED_CONSENT)
                .removeDetail(Details.USERNAME)
                .client("third-party").assertEvent();

        // Revoke grant in account mgmt.
        accountAppsPage.open();
        accountAppsPage.revokeGrant("third-party");

        events.expect(EventType.REVOKE_GRANT)
                .client("account").detail(Details.REVOKED_CLIENT, "third-party").assertEvent();

        // Open login form again and assert grant Page is shown
        oauth.openLoginForm();
        grantPage.assertCurrent();
        Assert.assertTrue(driver.getPageSource().contains(ROLE_USER));
        Assert.assertTrue(driver.getPageSource().contains(ROLE_CUSTOMER));
    }

    @Test
    public void oauthGrantAddAnotherRoleAndMapper() {
        // Grant permissions on grant screen
        oauth.clientId("third-party");
        oauth.doLoginGrant("test-user@localhost", "password");
        oauth.scope(OAuth2Constants.GRANT_TYPE);

        // Add new protocolMapper and role before showing grant page
        ProtocolMapperRepresentation protocolMapper = ProtocolMapperUtil.createClaimMapper(
                KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME,
                KerberosConstants.GSS_DELEGATION_CREDENTIAL,
                KerberosConstants.GSS_DELEGATION_CREDENTIAL, "String",
                true, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME,
                true, false);


        RealmResource appRealm = adminClient.realm("test");
        appRealm.roles().create(RoleBuilder.create().name("new-role").build());
        RoleRepresentation newRole = appRealm.roles().get("new-role").toRepresentation();

        ClientManager.realm(adminClient.realm("test")).clientId("third-party")
                .addProtocolMapper(protocolMapper)
                .addScopeMapping(newRole);

        UserRepresentation userResource = ApiUtil.findUserByUsername(appRealm, "test-user@localhost");
        appRealm.users().get(userResource.getId()).roles().realmLevel().add(Collections.singletonList(newRole));

        // Confirm grant page
        grantPage.assertCurrent();
        grantPage.accept();
        events.expectLogin()
                .client("third-party")
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        // Assert new role and protocol mapper not in account mgmt.
        accountAppsPage.open();
        AccountApplicationsPage.AppEntry appEntry = accountAppsPage.getApplications().get("third-party");
        Assert.assertFalse(appEntry.getRolesGranted().contains("new-role"));
        Assert.assertFalse(appEntry.getProtocolMappersGranted().contains(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME));

        // Show grant page another time. Just new role and protocol mapper are on the page
        oauth.openLoginForm();
        grantPage.assertCurrent();

        Assert.assertFalse(driver.getPageSource().contains(ROLE_USER));
        Assert.assertFalse(driver.getPageSource().contains("Full name"));
        Assert.assertTrue(driver.getPageSource().contains("new-role"));
        Assert.assertTrue(driver.getPageSource().contains(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME));
        grantPage.accept();
        events.expectLogin()
                .client("third-party")
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        // Go to account mgmt. Everything is granted now
        accountAppsPage.open();
        appEntry = accountAppsPage.getApplications().get("third-party");
        Assert.assertTrue(appEntry.getRolesGranted().contains("new-role"));
        Assert.assertTrue(appEntry.getProtocolMappersGranted().contains(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME));

        // Revoke
        accountAppsPage.revokeGrant("third-party");
        events.expect(EventType.REVOKE_GRANT)
                .client("account").detail(Details.REVOKED_CLIENT, "third-party").assertEvent();

        // Cleanup
        ClientManager.realm(adminClient.realm("test")).clientId("third-party")
                .removeProtocolMapper(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME)
                .removeScopeMapping(newRole);

        appRealm.roles().deleteRole("new-role");

    }

    /*@Test
    public void oauthGrantScopeParamRequired() throws Exception {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                ClientModel thirdParty = appRealm.getClientByClientId("third-party");
                RoleModel barAppRole = thirdParty.addRole("bar-role");
                barAppRole.setScopeParamRequired(true);

                RoleModel fooRole = appRealm.addRole("foo-role");
                fooRole.setScopeParamRequired(true);
                thirdParty.addScopeMapping(fooRole);

                UserModel testUser = manager.getSession().users().getUserByUsername("test-user@localhost", appRealm);
                testUser.grantRole(fooRole);
                testUser.grantRole(barAppRole);
            }

        });

        // Assert roles not on grant screen when not requested
        oauth.clientId("third-party");
        oauth.doLoginGrant("test-user@localhost", "password");
        grantPage.assertCurrent();
        Assert.assertFalse(driver.getPageSource().contains("foo-role"));
        Assert.assertFalse(driver.getPageSource().contains("bar-role"));
        grantPage.cancel();

        events.expectLogin()
                .client("third-party")
                .error("rejected_by_user")
                .removeDetail(Details.CONSENT)
                .assertEvent();

        oauth.scope("foo-role third-party/bar-role");
        oauth.doLoginGrant("test-user@localhost", "password");
        grantPage.assertCurrent();
        Assert.assertTrue(driver.getPageSource().contains("foo-role"));
        Assert.assertTrue(driver.getPageSource().contains("bar-role"));
        grantPage.accept();

        events.expectLogin()
                .client("third-party")
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        // Revoke
        accountAppsPage.open();
        accountAppsPage.revokeGrant("third-party");
        events.expect(EventType.REVOKE_GRANT)
                .client("account").detail(Details.REVOKED_CLIENT, "third-party").assertEvent();

        // cleanup
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.removeRole(appRealm.getRole("foo-role"));
                ClientModel thirdparty = appRealm.getClientByClientId("third-party");
                thirdparty.removeRole(thirdparty.getRole("bar-role"));
            }

        });

    }*/

}
