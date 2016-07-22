<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=social.displayInfo; section>
    <#if section = "title">
    ${msg("loginTitle",(realm.displayName!''))}
    <#elseif section = "header">
    ${msg("loginTitleHtml",(realm.displayNameHtml!''))}
    <#elseif section = "form">
        <#if realm.password>
        <form id="kc-form-login" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="first-factor" class="${properties.kcLabelClass!}">First factor or Password</label>
                </div>

                <div class="${properties.kcInputWrapperClass!}">
                    <input id="first-factor" class="${properties.kcInputClass!}" name="first-factor" type="password" autocomplete="off" />
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="second-factor" class="${properties.kcLabelClass!}">Second factor</label>
                </div>

                <div class="${properties.kcInputWrapperClass!}">
                    <input id="second-factor" class="${properties.kcInputClass!}" name="second-factor" type="password" autocomplete="off" />
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <#if realm.rememberMe && !usernameEditDisabled??>
                        <div class="checkbox">
                            <label>
                                <#if login.rememberMe??>
                                    <input id="rememberMe" name="rememberMe" type="checkbox" tabindex="3" checked> ${msg("rememberMe")}
                                <#else>
                                    <input id="rememberMe" name="rememberMe" type="checkbox" tabindex="3"> ${msg("rememberMe")}
                                </#if>
                            </label>
                        </div>
                    </#if>
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                        <#if realm.resetPasswordAllowed>
                            <span><a href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a></span>
                        </#if>
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <div class="${properties.kcFormButtonsWrapperClass!}">
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>
                    </div>
                </div>
            </div>
        </form>
        </#if>
    </#if>
</@layout.registrationLayout>
