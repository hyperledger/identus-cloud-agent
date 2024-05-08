package io.iohk.atala.keycloak;

import org.jboss.logging.Logger;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint;
import org.keycloak.sessions.AuthenticationSessionModel;

public class OID4VCIAuthorizationEndpoint extends AuthorizationEndpoint {
    private static final Logger logger = Logger.getLogger(OID4VCITokenEndpoint.class);

    private final String issuerState;

    public OID4VCIAuthorizationEndpoint(KeycloakSession session, EventBuilder event) {
        super(session, event);
        this.issuerState = session.getContext().getUri().getQueryParameters().getFirst(OID4VCIConstants.ISSUER_STATE);
    }

    @Override
    protected AuthenticationSessionModel createAuthenticationSession(ClientModel client, String requestState) {
        AuthenticationSessionModel authSession = super.createAuthenticationSession(client, requestState);
        authSession.setClientNote(OID4VCIConstants.ISSUER_STATE, issuerState);
        return super.createAuthenticationSession(client, requestState);
    }
}
