package org.keycloak.org.keycloak.examples.federation.sssd;

import org.freedesktop.dbus.Variant;
import org.freedesktop.sssd.infopipe.InfoPipe;
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

    private final KeycloakSession session;
    private final UserFederationProviderModel model;
    protected static final Set<String> supportedCredentialTypes = new HashSet<String>();

    private static final Logger LOGGER = Logger.getLogger(SSSDFederationProvider.class.getSimpleName());

    public SSSDFederationProvider(KeycloakSession session, UserFederationProviderModel model) {
        this.session = session;
        this.model = model;
    }

    static
    {
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

    //TODO Bad implemented for testing
    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        if (userExists(username)) {
            UserModel userModel = session.userStorage().addUser(realm, username);
            userModel.setEnabled(true);
            userModel.setFederationLink(model.getId());
            return userModel;
        }

        return null;
    }

    private boolean userExists(String username) {

        String[] attr = {"username", "mail", "givenname", "sn", "telephoneNumber","mail"};
        InfoPipe infoPipe = Sssd.infopipe();
        Map<String, Variant> attributes = infoPipe.getUserAttributes(username, Arrays.asList(attr));

        LOGGER.info("" + attributes);

        List<String> groups = infoPipe.getUserGroups("john");
        LOGGER.info("" + groups);

        if(attributes != null) {
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

    //TODO working on it
    @Override
    public boolean isValid(RealmModel realm, UserModel local) {

        String[] attr = {"username", "mail", "givenname", "sn", "telephoneNumber","mail"};
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

    //TODO implement
    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, List<UserCredentialModel> input) {
        return false;
    }

    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, UserCredentialModel... input) {
        return false;
    }

    @Override
    public CredentialValidationOutput validCredentials(RealmModel realm, UserCredentialModel credential) {
        return null;
    }

    @Override
    public void close() {

    }
}
