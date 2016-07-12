package org.keycloak.examples.federation.properties.impl;

import org.jvnet.libpam.PAM;
import org.jvnet.libpam.PAMException;
import org.jvnet.libpam.UnixUser;

import java.util.logging.Logger;

/**
 * Created by abstractj on 7/12/16.
 */
public class PAMAuthenticator {

    private static final String PAM_SERVICE = "keycloak";
    private static final Logger logger = Logger.getLogger(PAMAuthenticator.class.getSimpleName());

    public UnixUser authenticate(String username, String password) {
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
}
