package org.keycloak.sssd;

import org.freedesktop.dbus.Variant;
import org.freedesktop.sssd.infopipe.InfoPipe;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
public class SssdTest {

    public static void main(String[] args) {

        String[] attr = {"mail", "givenname", "sn", "telephoneNumber"};
        InfoPipe infoPipe = Sssd.infopipe();
        Map<String, Variant> attributes = infoPipe.getUserAttributes("john", Arrays.asList(attr));

        System.out.println(attributes);

        List<String> groups = infoPipe.getUserGroups("alice");

        System.out.println(groups);

        Sssd.disconnect();
    }
}
