package org.keycloak.testsuite.util;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
public class UserUtil {

    private static Keycloak adminClient;

    public UserUtil(Keycloak adminClient) {
        this.adminClient = adminClient;
    }

    //@TODO move to an utility class UserUtils
    public static UserResource getUserByUsername(String username) {
        String userId = adminClient.realm("test").users().search(username, null, null, null, 0, 1).get(0).getId();
        return adminClient.realm("test").users().get(userId);
    }

    public static UserResource getUserByUsername(RealmResource realm, String username) {
        String userId = realm.users().search(username, null, null, null, 0, 1).get(0).getId();
        return realm.users().get(userId);
    }

}
