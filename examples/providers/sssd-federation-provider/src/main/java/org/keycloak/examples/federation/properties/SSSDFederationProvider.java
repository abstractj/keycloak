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
import org.jboss.logging.Logger;
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
import org.keycloak.sssd.Sssd;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SSSDFederationProvider implements UserFederationProvider {

    private static final String PAM_SERVICE = "keycloak";
    private static final Logger LOGGER = Logger.getLogger(SSSDFederationProvider.class.getSimpleName());

    protected static final Set<String> supportedCredentialTypes = new HashSet<String>();
    protected KeycloakSession session;
    protected UserFederationProviderModel model;

    public SSSDFederationProvider(KeycloakSession session, UserFederationProviderModel model, SSSDFederationProviderFactory sssdFederationProviderFactory) {
        this.session = session;
        this.model = model;
    }

    static
    {
        supportedCredentialTypes.add(UserCredentialModel.PASSWORD);
    }


    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        LOGGER.info("================================================================");
        LOGGER.info("getUserByUsername");
        LOGGER.info("================================================================");

        if (userExists(username)) {
            UserModel userModel = session.userStorage().addUser(realm, username);
            userModel.setEnabled(true);
            userModel.setFederationLink(model.getId());
            return userModel;
        }

        return null;
    }

    private boolean userExists(String username) {

        LOGGER.info("================================================================");
        LOGGER.info("userExists");
        LOGGER.info("================================================================");

        String[] attr = {"mail", "givenname", "sn", "telephoneNumber"};
        try {
            InfoPipe infoPipe = Sssd.infopipe();
            Map<String, Variant> attributes = infoPipe.getUserAttributes(username, Arrays.asList(attr));

            LOGGER.info("" + attributes);

            List<String> groups = infoPipe.getUserGroups(username);
            LOGGER.info("" + groups);

            if (attributes != null) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Sssd.disconnect();
        }

        return false;
    }


    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return null;
    }

    /**
     * We only search for Usernames as that is all that is stored in the properties file.  Not that if the user
     * does exist in the properties file, we only import it if the user hasn't been imported already.
     *
     * @param attributes
     * @param realm
     * @param maxResults
     * @return
     */
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

    /**
     * See if the user is still in the properties file
     *
     * @param local
     * @return
     */
    @Override
    public boolean isValid(RealmModel realm, UserModel local) {
        LOGGER.info("================================================================");
        LOGGER.info("isValid");
        LOGGER.info("================================================================");

        String[] attr = {"mail", "givenname", "sn", "telephoneNumber"};
        InfoPipe infoPipe = Sssd.infopipe();
        Map<String, Variant> attributes = infoPipe.getUserAttributes(local.getUsername(), Arrays.asList(attr));

        LOGGER.info("" + attributes);

        LOGGER.info("" + attributes.keySet());

        //TODO remove this dirty thing
        if(attributes != null)
            return true;

        return attributes.containsKey(local.getEmail());
    }

    /**
     * hardcoded to only return PASSWORD
     *
     * @param user
     * @return
     */
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
        LOGGER.info("================================================================");
        LOGGER.info("validCredentials1");
        LOGGER.info("================================================================");

        for (UserCredentialModel cred : input) {
            if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                UnixUser u = null;
                try {
                    u = new PAM(PAM_SERVICE).authenticate(user.getUsername(), cred.getValue());
                } catch (PAMException e) {
                    e.printStackTrace();
                }

                if (u == null) return false;
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, UserCredentialModel... input) {
        LOGGER.info("================================================================");
        LOGGER.info("validCredentials2");
        LOGGER.info("================================================================");

        for (UserCredentialModel cred : input) {
            if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                UnixUser u = null;
                try {
                    u = new PAM(PAM_SERVICE).authenticate(user.getUsername(), cred.getValue());
                } catch (PAMException e) {
                    e.printStackTrace();
                }

                if (u == null) return false;
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    @Override
    public CredentialValidationOutput validCredentials(RealmModel realm, UserCredentialModel credential) {
        LOGGER.info("================================================================");
        LOGGER.info("validCredentials3");
        LOGGER.info("================================================================");

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

    /**
     * The properties file is readonly so don't removing a user
     *
     * @return
     */
    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        throw new IllegalStateException("Remove not supported");
    }

    @Override
    public void close() {

    }
}
