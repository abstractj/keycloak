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

package org.keycloak.services.managers;

import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.RealmAuth;

import java.util.Map;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 */
public class SMTPConnectionTestManager {

    private final RealmModel realm;
    private final EmailSenderProvider emailProvider;
    private final UserModel user;
    private static final Logger logger = Logger.getLogger(SMTPConnectionTestManager.class);

    public SMTPConnectionTestManager(RealmModel realm, KeycloakSession session, RealmAuth auth) {
        this.realm = realm;
        this.emailProvider = session.getProvider(EmailSenderProvider.class);
        this.user = auth.getAuth().getUser();
    }

    public boolean testSMTP(Map<String, String> config) {

        if (user.getEmail() == null) {
            logger.errorf("%s e-mail is empty. Please provide a valid e-mail.", user.getUsername());
            return false;
        }

        realm.setSmtpConfig(config);

        try {
            emailProvider.send(realm, user, "[KEYCLOAK] - SMTP test message", "Congratulations, you've made it!",
                    "Congratulations, you've made it!");
            return true;
        } catch (EmailException e) {
            logger.errorf("Invalid SMTP configuration", e.getMessage());
            return false;
        }
    }
}
