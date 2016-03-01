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

package org.keycloak.sssd;

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.sssd.infopipe.InfoPipe;
import org.freedesktop.sssd.infopipe.User;

import static org.freedesktop.sssd.infopipe.InfoPipe.OBJECTPATH;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
public class Sssd {

    public static final String BUSNAME = "org.freedesktop.sssd.infopipe";

    public static User user() {
        return SingletonHolder.USER_OBJECT;
    }

    public static InfoPipe infopipe() {
        return SingletonHolder.INFOPIPE_OBJECT;
    }

    private Sssd() {
    }

    private static final class SingletonHolder {
        private static InfoPipe INFOPIPE_OBJECT;
        private static User USER_OBJECT;

        static {
            try {
                DBusConnection connection = DBusConnection.getConnection(DBusConnection.SYSTEM);
                INFOPIPE_OBJECT = connection.getRemoteObject(BUSNAME, InfoPipe.OBJECTPATH, InfoPipe.class);
                USER_OBJECT = connection.getRemoteObject(BUSNAME, User.OBJECTPATH, User.class);
            } catch (DBusException e) {
                e.printStackTrace();
            }
        }
    }
}
