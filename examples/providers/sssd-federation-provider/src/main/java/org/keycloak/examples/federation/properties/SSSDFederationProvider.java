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

package org.keycloak.examples.federation.properties;

import org.freedesktop.dbus.Variant;
import org.freedesktop.sssd.infopipe.InfoPipe;
import org.jvnet.libpam.PAM;
import org.jvnet.libpam.PAMException;
import org.jvnet.libpam.UnixUser;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.UserManager;
import org.keycloak.sssd.Sssd;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SSSDFederationProvider implements UserFederationProvider {

    private static final String PAM_SERVICE = "keycloak";
    private static final Logger logger = Logger.getLogger(SSSDFederationProvider.class.getSimpleName());

    protected static final Set<String> supportedCredentialTypes = new HashSet<>();
    protected KeycloakSession session;
    protected UserFederationProviderModel model;

    public SSSDFederationProvider(KeycloakSession session, UserFederationProviderModel model, SSSDFederationProviderFactory sssdFederationProviderFactory) {
        this.session = session;
        this.model = model;
    }

    static {
        supportedCredentialTypes.add(UserCredentialModel.PASSWORD);
    }


    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        return findOrCreateAuthenticatedUser(realm, username);
    }

    /**
     * Called after successful authentication
     *
     * @param realm    realm
     * @param username username without realm prefix
     * @return user if found or successfully created. Null if user with same username already exists, but is not linked to this provider
     */
    protected UserModel findOrCreateAuthenticatedUser(RealmModel realm, String username) {
        UserModel user = session.userStorage().getUserByUsername(username, realm);
        if (user != null) {
//            logger.debug("SSSD authenticated user " + username + " found in Keycloak storage");

            if (!model.getId().equals(user.getFederationLink())) {
//                logger.warn("User with username " + username + " already exists, but is not linked to provider [" + model.getDisplayName() + "]");
                return null;
            } else {
                UserModel proxied = validateAndProxy(realm, user);
                if (proxied != null) {
                    return proxied;
                } else {
//                    logger.warn("User with username " + username + " already exists and is linked to provider [" + model.getDisplayName() +
//                            "] but principal is not correct.");
//                    logger.warn("Will re-create user");
                    new UserManager(session).removeUser(realm, user, session.userStorage());
                }
            }
        }

//        logger.debug("SSSD authenticated user " + username + " not in Keycloak storage. Creating...");
        return importUserToKeycloak(realm, username);
    }

    protected UserModel importUserToKeycloak(RealmModel realm, String username) {
        Map<String, Variant> sssdUser = loadSSSDUserByUsername(username);
//        logger.debugf("Creating SSSD user: %s to local Keycloak storage", username);
        UserModel user = session.userStorage().addUser(realm, username);
        user.setEnabled(true);
        user.setEmail(getRawAttribute(sssdUser.get("mail")));
        user.setFirstName(getRawAttribute(sssdUser.get("givenname")));
        user.setLastName(getRawAttribute(sssdUser.get("sn")));
        user.setFederationLink(model.getId());

        return validateAndProxy(realm, user);
    }

    public String getRawAttribute(Variant variant) {
        return ((Vector) variant.getValue()).get(0).toString();
    }

    private Map<String, Variant> loadSSSDUserByUsername(String username) {
        String[] attr = {"mail", "givenname", "sn", "telephoneNumber"};
        Map<String, Variant> attributes = null;
        try {
            InfoPipe infoPipe = Sssd.infopipe();
            attributes = infoPipe.getUserAttributes(username, Arrays.asList(attr));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return attributes;
    }


    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return null;
    }

    @Override
    public List<UserModel> searchByAttributes(Map<String, String> attributes, RealmModel realm, int maxResults) {
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return Collections.emptyList();
    }

    @Override
    public void preRemove(RealmModel realm) {
        // complete  We don't care about the realm being removed
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        // complete we dont'care if a role is removed

    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        // complete we dont'care if a role is removed

    }

    @Override
    public boolean isValid(RealmModel realm, UserModel local) {
        Map<String, Variant> attributes = loadSSSDUserByUsername(local.getUsername());
        return getRawAttribute(attributes.get("mail")).equalsIgnoreCase(local.getEmail());
    }

    @Override
    public Set<String> getSupportedCredentialTypes(UserModel user) {
        return supportedCredentialTypes;
    }

    @Override
    public Set<String> getSupportedCredentialTypes() {
        return supportedCredentialTypes;
    }

    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, List<UserCredentialModel> input) {
        for (UserCredentialModel cred : input) {
            if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                return (authenticate(user.getUsername(), cred.getValue()) != null);
            }
        }
        return false;
    }

    private UnixUser authenticate(String username, String password) {
        PAM pam = null;
        UnixUser user = null;
        try {
            pam = new PAM(PAM_SERVICE);
            user = pam.authenticate(username, password);
        } catch (PAMException e) {
//            logger.error("Authentication failed", e);
            e.printStackTrace();
        } finally {
            pam.dispose();
        }
        return user;
    }

    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, UserCredentialModel... input) {
        return validCredentials(realm, user, Arrays.asList(input));
    }

    @Override
    public CredentialValidationOutput validCredentials(RealmModel realm, UserCredentialModel credential) {
        return CredentialValidationOutput.failed();
    }

    /**
     * Keycloak will call this method if it finds an imported UserModel.  Here we proxy the UserModel with
     * a Readonly proxy which will barf if password is updated.
     *
     * @param local
     * @return
     */
    @Override
    public UserModel validateAndProxy(RealmModel realm, UserModel local) {
        if (isValid(realm, local)) {
            return new ReadonlyUserModelProxy(local);
        } else {
            return null;
        }
    }

    /**
     * The properties file is readonly so don't suppport registration.
     *
     * @return
     */
    @Override
    public boolean synchronizeRegistrations() {
        return false;
    }

    /**
     * The properties file is readonly so don't suppport registration.
     *
     * @return
     */
    @Override
    public UserModel register(RealmModel realm, UserModel user) {
        throw new IllegalStateException("Registration not supported");
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void close() {
        Sssd.disconnect();
    }
}
