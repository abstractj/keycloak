package org.keycloak.examples.federation.sssd;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserFederationSyncResult;

import java.util.Date;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
public class SSSDFederationProviderFactory implements UserFederationProviderFactory {

    private static final Logger LOGGER = Logger.getLogger(SSSDFederationProvider.class.getSimpleName());
    public static final String PROVIDER_NAME = "SSSD";

    @Override
    public UserFederationProvider getInstance(KeycloakSession session, UserFederationProviderModel model) {
        return new SSSDFederationProvider(session, model, this);
    }

    @Override
    public Set<String> getConfigurationOptions() {
        return null;
    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public UserFederationSyncResult syncAllUsers(KeycloakSessionFactory sessionFactory, String realmId, UserFederationProviderModel model) {
        LOGGER.info("Sync users not supported for this provider");
        return UserFederationSyncResult.empty();
    }

    @Override
    public UserFederationSyncResult syncChangedUsers(KeycloakSessionFactory sessionFactory, String realmId, UserFederationProviderModel model, Date lastSync) {
        LOGGER.info("Sync users not supported for this provider");
        return UserFederationSyncResult.empty();
    }

    @Override
    public UserFederationProvider create(KeycloakSession session) {
        return null;
    }

    @Override
    public void init(Config.Scope config) {
        throw new IllegalAccessError("Illegal to call this method");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }
}
