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
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;

/**
 * @@author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 * @version $Revision: 1 $
 */
public class PAMFormAuthenticator extends AbstractUsernameFormAuthenticator implements Authenticator {

    protected static ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

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
        String username = formData.getFirst(AuthenticationManager.FORM_USERNAME);
        if (username == null) {
            context.getEvent().error(Errors.USER_NOT_FOUND);
            Response challengeResponse = invalidUser(context);
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challengeResponse);
            return false;
        }
        context.getEvent().detail(Details.USERNAME, username);
        context.getClientSession().setNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, username);

        UserModel user = null;
        try {
            user = KeycloakModelUtils.findUserByNameOrEmail(context.getSession(), context.getRealm(), username);
        } catch (ModelDuplicateException mde) {
            logger.modelDuplicateException(mde);

            // Could happen during federation import
            if (mde.getDuplicateFieldName() != null && mde.getDuplicateFieldName().equals(UserModel.EMAIL)) {
                setDuplicateUserChallenge(context, Errors.EMAIL_IN_USE, Messages.EMAIL_EXISTS, AuthenticationFlowError.INVALID_USER);
            } else {
                setDuplicateUserChallenge(context, Errors.USERNAME_IN_USE, Messages.USERNAME_EXISTS, AuthenticationFlowError.INVALID_USER);
            }

            return false;
        }

        if (invalidUser(context, user)){
            return false;
        }

        if (!validatePasswordAndOTP(context, user, formData)){
            return false;
        }

        if(!enabledUser(context, user)){
            return false;
        }

        String rememberMe = formData.getFirst("rememberMe");
        boolean remember = rememberMe != null && rememberMe.equalsIgnoreCase("on");
        if (remember) {
            context.getClientSession().setNote(Details.REMEMBER_ME, "true");
            context.getEvent().detail(Details.REMEMBER_ME, "true");
        } else {
            context.getClientSession().removeNote(Details.REMEMBER_ME);
        }
        context.setUser(user);
        return true;
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

    private boolean validatePasswordAndOTP(AuthenticationFlowContext context, UserModel user, MultivaluedMap<String, String> inputData) {

        logger.info("=====================================================");
        logger.info("validatePasswordAndOTP()");
        logger.info("=====================================================");


        List<UserCredentialModel> credentials = new LinkedList<>();
        String password = inputData.getFirst(CredentialRepresentation.PASSWORD);
        String totp = inputData.getFirst(CredentialRepresentation.TOTP);
        credentials.add(UserCredentialModel.password(password));
        credentials.add(UserCredentialModel.totp(totp));

        logger.info("=====================================================");
        logger.info("password: " + password);
        logger.info("otp: " + totp);
        logger.info("=====================================================");


        boolean valid = context.getSession().users().validCredentials(context.getSession(), context.getRealm(), user, credentials);
        if (!valid) {
            context.getEvent().user(user);
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            Response challengeResponse = invalidCredentials(context);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
            context.clearUser();
            return false;
        }
        return true;
    }
}
