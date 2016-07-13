package org.keycloak.examples.federation.sssd.impl;

import org.jboss.logging.Logger;
import org.jvnet.libpam.PAM;
import org.jvnet.libpam.PAMException;
import org.jvnet.libpam.UnixUser;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 * @version $Revision: 1 $
 */
public class PAMAuthenticator {

    private static final String PAM_SERVICE = "keycloak";
    private static final Logger logger = Logger.getLogger(PAMAuthenticator.class);
    private final String username;
    private final String password;

    public PAMAuthenticator(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public UnixUser authenticate() {
        PAM pam = null;
        UnixUser user = null;
        try {
            pam = new PAM(PAM_SERVICE);
            user = pam.authenticate(username, password);
        } catch (PAMException e) {
            logger.error("Authentication failed", e);
            e.printStackTrace();
        } finally {
            pam.dispose();
        }
        return user;
    }
}
