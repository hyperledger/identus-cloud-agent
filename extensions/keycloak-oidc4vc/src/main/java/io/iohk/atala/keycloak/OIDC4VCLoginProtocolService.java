package io.iohk.atala.keycloak;

import jakarta.ws.rs.Path;
import org.jboss.logging.Logger;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.OIDCProviderConfig;
import org.keycloak.protocol.oidc.TokenManager;

public class OIDC4VCLoginProtocolService extends OIDCLoginProtocolService {
    private static final Logger logger = Logger.getLogger(OIDC4VCLoginProtocolService.class);
    private final TokenManager tokenManager;
    private final KeycloakSession session;
    private final EventBuilder event;

    public OIDC4VCLoginProtocolService(KeycloakSession session, EventBuilder event, OIDCProviderConfig providerConfig) {
        super(session, event, providerConfig);
        this.tokenManager = new TokenManager();
        this.session = session;
        this.event = event;
        logger.warn("OIDC4VCLoginProtocolService is created !!!!");
    }

    @Path("auth")
    public Object auth() {
        return new OIDC4VCAuthorizationEndpoint(session, event);
    }

    @Path("token")
    public Object token() {
        return new OIDC4VCTokenEndpoint(session, tokenManager, event);
    }
}
