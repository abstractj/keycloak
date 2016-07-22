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
                    <label for="username" class="${properties.kcLabelClass!}"><#if !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>
                </div>

                <div class="${properties.kcInputWrapperClass!}">
                    <input id="username" class="${properties.kcInputClass!}" name="username" type="text" autofocus />
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="password" class="${properties.kcLabelClass!}">${msg("password")}</label>
                </div>

                <div class="${properties.kcInputWrapperClass!}">
                    <input id="password" class="${properties.kcInputClass!}" name="password" type="password" autocomplete="off" />
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
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="login-next" id="kc-login-next" type="submit" value="Next"/>
                    </div>
                </div>
            </div>
        </form>
        </#if>
    </#if>
</@layout.registrationLayout>
