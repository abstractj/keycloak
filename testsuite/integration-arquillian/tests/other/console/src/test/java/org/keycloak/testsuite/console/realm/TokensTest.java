/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.console.realm;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.authentication.actiontoken.verifyemail.VerifyEmailActionToken;
import org.keycloak.models.UserModel;
import org.keycloak.testsuite.console.page.realm.TokenSettings;
import org.keycloak.testsuite.console.page.users.UserAttributes;
import org.keycloak.testsuite.model.RequiredUserAction;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.keycloak.testsuite.model.RequiredUserAction.UPDATE_PASSWORD;
import static org.keycloak.testsuite.model.RequiredUserAction.VERIFY_EMAIL;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;

/**
 *
 * @author Petr Mensik
 */
public class TokensTest extends AbstractRealmTest {

    @Page
    private TokenSettings tokenSettingsPage;

    @Page
    private UserAttributes userAttributesPage;

    private static final int TIMEOUT = 1;
    private static final TimeUnit TIME_UNIT = TimeUnit.MINUTES;

    @Before
    public void beforeTokensTest() {
//        configure().realmSettings();
//        tabs().tokens();
        tokenSettingsPage.navigateTo();
    }

    @Ignore
    @Test
    public void testTimeoutForRealmSession() throws InterruptedException {
        tokenSettingsPage.form().setSessionTimeout(TIMEOUT, TIME_UNIT);
        tokenSettingsPage.form().save();

        loginToTestRealmConsoleAs(testUser);
        waitForTimeout(TIMEOUT + 2);

        driver.navigate().refresh();

        log.debug(driver.getCurrentUrl());
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }

    @Ignore
    @Test
    public void testLifespanOfRealmSession() throws InterruptedException {
        tokenSettingsPage.form().setSessionTimeoutLifespan(TIMEOUT, TIME_UNIT);
        tokenSettingsPage.form().save();

        loginToTestRealmConsoleAs(testUser);
        waitForTimeout(TIMEOUT / 2);

        driver.navigate().refresh();
        assertCurrentUrlStartsWith(testRealmAdminConsolePage); // assert still logged in (within lifespan)

        waitForTimeout(TIMEOUT / 2 + 2);
        driver.navigate().refresh();

        log.debug(driver.getCurrentUrl());
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage); // assert logged out (lifespan exceeded)
    }

    @Test
    public void testLifespanOfVerifyEmailActionToken() throws InterruptedException {
        tokenSettingsPage.navigateTo();

        tokenSettingsPage.form().setOperation(VerifyEmailActionToken.TOKEN_TYPE, TIMEOUT, TIME_UNIT);
        tokenSettingsPage.form().save();

        addRequiredAction(VERIFY_EMAIL);

        loginToTestRealmConsoleAs(testUser);
        /*waitForTimeout(TIMEOUT / 2);

        driver.navigate().refresh();
        assertCurrentUrlStartsWith(testRealmAdminConsolePage); // assert still logged in (within lifespan)

        waitForTimeout(TIMEOUT / 2 + 2);
        driver.navigate().refresh();

        log.debug(driver.getCurrentUrl());
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage); // assert logged out (lifespan exceeded)*/
    }

    private void addRequiredAction(RequiredUserAction userAction) {
        userAttributesPage.setId(testUser.getId());
        userAttributesPage.navigateTo();

        userAttributesPage.form().addRequiredAction(userAction.getActionName());
        userAttributesPage.form().save();
        assertAlertSuccess();
    }

    private void waitForTimeout (int timeout) throws InterruptedException {
        log.info("Wait for timeout: " + timeout + " " + TIME_UNIT);
        TIME_UNIT.sleep(timeout);
        log.info("Timeout reached");
    }

}
