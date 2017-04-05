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

package org.keycloak.testsuite.admin;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.UserBuilder;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 */
public class SMTPConnectionTest extends AbstractKeycloakTest {

    @Rule
    public GreenMailRule greenMailRule = new GreenMailRule();
    private RealmResource realm;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    @Before
    public void before() {
        realm = adminClient.realm("master");
        List<UserRepresentation> admin = realm.users().search("admin", 0, 1);
        UserRepresentation user = UserBuilder.edit(admin.get(0)).email("admin@localhost").build();
        realm.users().get(user.getId()).update(user);
    }

    @Test
    public void testWithEmptySettings() {
        Response response = realm.testSMTPConnection(null, null, null, null, null, null,
                null, null);
        assertStatus(response, 400);
    }

    @Test
    public void testWithProperSettings() throws IOException, MessagingException {
        Response response = realm.testSMTPConnection("127.0.0.1", "3025", "auto@keycloak.org", null, null, null,
                null, null);
        assertStatus(response, 204);
        assertMailReceived();
    }

    @Test
    public void testWithAuthEnabledCredentialsEmpty() throws IOException, MessagingException {
        Response response = realm.testSMTPConnection("127.0.0.1", "3025", "auto@keycloak.org", "true", null, null,
                null, null);
        assertStatus(response, 400);
    }

    @Test
    public void testWithAuthEnabledValidCredentials() throws IOException, MessagingException {
        greenMailRule.credentials("admin@localhost", "admin");
        Response response = realm.testSMTPConnection("127.0.0.1", "3025", "auto@keycloak.org", "true", null, null,
                "admin@localhost", "admin");
        assertStatus(response, 204);
    }

    private void assertStatus(Response response, int status) {
        assertEquals(status, response.getStatus());
        response.close();
    }

    private void assertMailReceived() {
        if (greenMailRule.getReceivedMessages().length == 1) {
            try {
                MimeMessage message = greenMailRule.getReceivedMessages()[0];
                assertEquals("[KEYCLOAK] - SMTP test message", message.getSubject());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            fail("E-mail was not received");
        }
    }
}
