package io.iohk.atala.keycloak.model;

// TODO: support credential display option
// https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-11.2.3-2.11.2.6.1
public class CredentialConfiguration {
    private String name;
    private String scope;
    private String description;
    private String displayName;

    public CredentialConfiguration() {
        // for reflection
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
