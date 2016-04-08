package org.keycloak.testsuite.util;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
public class ClientUtil {

    private final Keycloak adminClient;

    public ClientUtil(Keycloak adminClient) {
        this.adminClient = adminClient;
    }

    //@TODO move to an utility class ClientUtils
    public ClientResource getClientById(String clientId) {
        String appId = adminClient.realm("test").clients().findByClientId(clientId).get(0).getId();
        return adminClient.realm("test").clients().get(appId);
    }


}
