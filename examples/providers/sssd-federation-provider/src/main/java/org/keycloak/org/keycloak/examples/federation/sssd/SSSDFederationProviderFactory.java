package org.keycloak.org.keycloak.examples.federation.sssd;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserFederationSyncResult;

import java.util.Date;
import java.util.Set;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
public class SSSDFederationProviderFactory implements UserFederationProviderFactory {
    @Override
    public UserFederationProvider getInstance(KeycloakSession session, UserFederationProviderModel model) {
        return null;
    }

    @Override
    public Set<String> getConfigurationOptions() {
        return null;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public UserFederationSyncResult syncAllUsers(KeycloakSessionFactory sessionFactory, String realmId, UserFederationProviderModel model) {
        return null;
    }

    @Override
    public UserFederationSyncResult syncChangedUsers(KeycloakSessionFactory sessionFactory, String realmId, UserFederationProviderModel model, Date lastSync) {
        return null;
    }

    @Override
    public UserFederationProvider create(KeycloakSession session) {
        return null;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }
}
