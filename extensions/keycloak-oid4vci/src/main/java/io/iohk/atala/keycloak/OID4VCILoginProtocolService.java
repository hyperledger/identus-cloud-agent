package io.iohk.atala.keycloak;

import jakarta.ws.rs.Path;
import org.jboss.logging.Logger;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.OIDCProviderConfig;
import org.keycloak.protocol.oidc.TokenManager;

public class OID4VCILoginProtocolService extends OIDCLoginProtocolService {
    private static final Logger logger = Logger.getLogger(OID4VCILoginProtocolService.class);
    private final TokenManager tokenManager;
    private final KeycloakSession session;
    private final EventBuilder event;

    public OID4VCILoginProtocolService(KeycloakSession session, EventBuilder event, OIDCProviderConfig providerConfig) {
        super(session, event, providerConfig);
        this.tokenManager = new TokenManager();
        this.session = session;
        this.event = event;
    }

    @Path("auth")
    public Object auth() {
        return new OID4VCIAuthorizationEndpoint(session, event);
    }

    @Path("token")
    public Object token() {
        return new OID4VCITokenEndpoint(session, tokenManager, event);
    }
}
