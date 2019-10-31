package org.keycloak.representations.account;

/**
 * Created by st on 29/03/17.
 */
public class ClientRepresentation {
    private String clientId;
    private String clientName;
    private String description;
    private boolean internal;
    private boolean inUse;
    private String url;
    private ConsentRepresentation consent;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public boolean isInUse() {
        return inUse;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public ConsentRepresentation getConsent() {
        return consent;
    }

    public void setConsent(ConsentRepresentation consent) {
        this.consent = consent;
    }
}
