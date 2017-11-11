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
package org.keycloak.testsuite.console.page.realm;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.authentication.actiontoken.verifyemail.VerifyEmailActionToken;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import java.util.concurrent.TimeUnit;

import static java.lang.String.valueOf;
import static org.apache.commons.lang3.text.WordUtils.capitalize;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 *
 * @author Petr Mensik
 */
public class TokenSettings extends RealmSettings {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/token-settings";
    }

    @Page
    private TokenSettingsForm form;

    public TokenSettingsForm form() {
        return form;
    }

    public class TokenSettingsForm extends Form {

        @FindBy(id = "ssoSessionIdleTimeout")
        private WebElement sessionTimeout;

        @FindBy(name = "ssoSessionIdleTimeoutUnit")
        private Select sessionTimeoutUnit;

        @FindBy(id = "ssoSessionMaxLifespan")
        private WebElement sessionLifespanTimeout;

        @FindBy(name = "ssoSessionMaxLifespanUnit")
        private Select sessionLifespanTimeoutUnit;

        @FindBy(name = "actionTokenAttributeSelect")
        private Select actionTokenAttributeSelect;

        @FindBy(name = "actionTokenAttributeUnit")
        private Select actionTokenAttributeUnit;

        @FindBy(id = "actionTokenAttributeTime")
        private WebElement actionTokenAttributeTime;

        public void setSessionTimeout(int timeout, TimeUnit unit) {
            setTimeout(sessionTimeoutUnit, sessionTimeout, timeout, unit);
        }

        public void setSessionTimeoutLifespan(int time, TimeUnit unit) {
            setTimeout(sessionLifespanTimeoutUnit, sessionLifespanTimeout, time, unit);
        }

        public void setOperation(String tokenType, int time, TimeUnit unit) {
            //FIXME duplicate line with seTimeout. Maybe refactor
            waitUntilElement(sessionTimeout).is().present();
            actionTokenAttributeSelect.selectByValue(tokenType.toLowerCase());
            setTimeout(actionTokenAttributeUnit, actionTokenAttributeTime, time, unit);
        }

        public void selectOperation(String tokenType) {

        }

        private void setTimeout(Select timeoutElement, WebElement unitElement,
                int timeout, TimeUnit unit) {
            waitUntilElement(sessionTimeout).is().present();
            timeoutElement.selectByValue(capitalize(unit.name().toLowerCase()));
            unitElement.clear();
            unitElement.sendKeys(valueOf(timeout));
        }

        public String getActionTokenAttributeUnit() {
            return actionTokenAttributeUnit.getFirstSelectedOption().getText();
        }

        public String getActionTokenAttributeTime() {
            return actionTokenAttributeTime.getAttribute("value");
        }

        public boolean isOperationEquals(String tokenType, int timeout, TimeUnit unit) {
            selectOperation(tokenType);

            waitUntilElement(sessionTimeout).is().present();
            actionTokenAttributeSelect.selectByValue(tokenType.toLowerCase());

            return actionTokenAttributeTime.getAttribute("value").equals(Integer.toString(timeout)) &&
                    actionTokenAttributeUnit.getFirstSelectedOption().getText().equals(capitalize(unit.name().toLowerCase()));
        }
    }
}
