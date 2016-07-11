package org.keycloak.examples.federation.sssd;

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
import org.keycloak.sssd.Sssd;

import java.util.*;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
public class SSSDFederationProvider implements UserFederationProvider {

    private static final String PAM_SERVICE = "keycloak";
    private static final Logger LOGGER = Logger.getLogger(SSSDFederationProvider.class.getSimpleName());
    protected static final Set<String> supportedCredentialTypes = new HashSet<String>();

    private final KeycloakSession session;
    private final UserFederationProviderModel model;

    public SSSDFederationProvider(KeycloakSession session, UserFederationProviderModel model, SSSDFederationProviderFactory sssdFederationProviderFactory) {
        this.session = session;
        this.model = model;
    }

    static {
        supportedCredentialTypes.add(UserCredentialModel.PASSWORD);
    }

    public KeycloakSession getSession() {
        return session;
    }

    public UserFederationProviderModel getModel() {
        return model;
    }

    @Override
    public UserModel validateAndProxy(RealmModel realm, UserModel local) {
        return null;
    }

    //Done
    @Override
    public boolean synchronizeRegistrations() {
        return false;
    }

    @Override
    public UserModel register(RealmModel realm, UserModel user) {
        return null;
    }

    //Done
    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        return false;
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {

        LOGGER.info("================================================================");
        LOGGER.info("" + SSSDFederationProvider.class.getEnclosingMethod().getName());
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
        LOGGER.info("" + SSSDFederationProvider.class.getEnclosingMethod().getName());
        LOGGER.info("================================================================");

        String[] attr = {"username", "mail", "givenname", "sn", "telephoneNumber", "mail"};
        InfoPipe infoPipe = Sssd.infopipe();
        Map<String, Variant> attributes = infoPipe.getUserAttributes(username, Arrays.asList(attr));

        LOGGER.info("" + attributes);

        List<String> groups = infoPipe.getUserGroups("john");
        LOGGER.info("" + groups);

        if (attributes != null) {
            return true;
        }

        Sssd.disconnect();

        return false;
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return null;
    }

    //Done
    @Override
    public List<UserModel> searchByAttributes(Map<String, String> attributes, RealmModel realm, int maxResults) {
        return Collections.emptyList();
    }

    //Done
    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return Collections.emptyList();
    }

    //Done
    @Override
    public void preRemove(RealmModel realm) {

    }

    //Done
    @Override
    public void preRemove(RealmModel realm, RoleModel role) {

    }

    //Done
    @Override
    public void preRemove(RealmModel realm, GroupModel group) {

    }

    @Override
    public boolean isValid(RealmModel realm, UserModel local) {

        LOGGER.info("================================================================");
        LOGGER.info("" + SSSDFederationProvider.class.getEnclosingMethod().getName());
        LOGGER.info("================================================================");

        String[] attr = {"username", "mail", "givenname", "sn", "telephoneNumber", "mail"};
        InfoPipe infoPipe = Sssd.infopipe();
        Map<String, Variant> attributes = infoPipe.getUserAttributes(local.getUsername(), Arrays.asList(attr));

        return attributes.containsKey(local.getEmail());
    }

    @Override
    public Set<String> getSupportedCredentialTypes(UserModel user) {
        return null;
    }

    @Override
    public Set<String> getSupportedCredentialTypes() {
        return null;
    }

    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, List<UserCredentialModel> input) {

        LOGGER.info("================================================================");
        LOGGER.info("" + SSSDFederationProvider.class.getEnclosingMethod().getName());
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
        LOGGER.info("" + SSSDFederationProvider.class.getEnclosingMethod().getName());
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
    public CredentialValidationOutput validCredentials(RealmModel realm, UserCredentialModel cred) {
        return CredentialValidationOutput.failed();
    }

    @Override
    public void close() {

    }
}
