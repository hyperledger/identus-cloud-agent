package io.iohk.atala.keycloak;

import org.jboss.logging.Logger;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint;
import org.keycloak.sessions.AuthenticationSessionModel;

public class OIDC4VCAuthorizationEndpoint extends AuthorizationEndpoint {
    private static final Logger logger = Logger.getLogger(OIDC4VCTokenEndpoint.class);

    private final String issuerState;

    public OIDC4VCAuthorizationEndpoint(KeycloakSession session, EventBuilder event) {
        super(session, event);
        this.issuerState = session.getContext().getUri().getQueryParameters().getFirst(OIDC4VCConstants.ISSUER_STATE);
    }

    @Override
    protected AuthenticationSessionModel createAuthenticationSession(ClientModel client, String requestState) {
        AuthenticationSessionModel authSession = super.createAuthenticationSession(client, requestState);
        authSession.setClientNote(OIDC4VCConstants.ISSUER_STATE, issuerState);
        return super.createAuthenticationSession(client, requestState);
    }
}
