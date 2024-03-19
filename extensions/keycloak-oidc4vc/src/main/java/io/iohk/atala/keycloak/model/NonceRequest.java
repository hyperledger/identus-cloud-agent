package io.iohk.atala.keycloak.model;

public class NonceRequest {
    private String issuerState;

    public NonceRequest(String issuerState) {
        this.issuerState = issuerState;
    }

    public NonceRequest() {
        // for reflection
    }

    public String getIssuerState() {
        return this.issuerState;
    }

    public void setIssuerState(String issuerState) {
        this.issuerState = issuerState;
    }
}