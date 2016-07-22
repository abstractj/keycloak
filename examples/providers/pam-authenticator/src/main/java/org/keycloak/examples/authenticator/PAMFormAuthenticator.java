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

package org.keycloak.examples.authenticator;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.managers.AuthenticationManager;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * @@author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 * @version $Revision: 1 $
 */
public class PAMFormAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(PAMFormAuthenticator.class.getSimpleName());

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey("cancel")) {
            context.cancelLogin();
            return;
        }
        if (!validateCredential(context, formData)) {
            return;
        }
        context.success();
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.info("=====================================================");
        logger.info("authenticate()");
        logger.info("=====================================================");

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl<>();
        String loginHint = context.getClientSession().getNote(OIDCLoginProtocol.LOGIN_HINT_PARAM);

        String rememberMeUsername = AuthenticationManager.getRememberMeUsername(context.getRealm(), context.getHttpRequest().getHttpHeaders());

        if (loginHint != null || rememberMeUsername != null) {
            if (loginHint != null) {
                formData.add(AuthenticationManager.FORM_USERNAME, loginHint);
            } else {
                formData.add(AuthenticationManager.FORM_USERNAME, rememberMeUsername);
                formData.add("rememberMe", "on");
            }
        }
        Response challengeResponse = challenge(context, formData);
        context.getClientSession().setNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION, context.getExecution().getId());
        context.challenge(challengeResponse);

    }

    public boolean validateCredential(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {

        logger.info("=====================================================");
        logger.info("validateCredential()");
        logger.info("=====================================================");

        /* set to false for now, otherwise username/password will be validated */
        return false;

//        context.success();
    }

    //Intentionally changed to false
    @Override
    public boolean requiresUser() {
        return false;
    }

    protected Response challenge(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {

        logger.info("=====================================================");
        logger.info("challenge()");
        logger.info("=====================================================");

        LoginFormsProvider forms = context.form();

        if (formData.size() > 0) forms.setFormData(formData);

        return context.form().createForm("pam-login.ftl");
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        logger.info("=====================================================");
        logger.info("configuredFor()");
        logger.info("=====================================================");

        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        //No required actions for PAM
    }



    @Override
    public void close() {

    }
}
