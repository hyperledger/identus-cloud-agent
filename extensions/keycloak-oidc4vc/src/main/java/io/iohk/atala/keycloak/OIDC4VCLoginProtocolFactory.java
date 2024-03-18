package io.iohk.atala.keycloak;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.OIDCProviderConfig;

public class OIDC4VCLoginProtocolFactory extends OIDCLoginProtocolFactory {
    private static final Logger logger = Logger.getLogger(OIDC4VCLoginProtocolFactory.class);
    private OIDCProviderConfig providerConfig;

    @Override
    public int order() {
        return 1;
    }

    @Override
    public void init(Config.Scope config) {
        this.providerConfig = new OIDCProviderConfig(config);
        super.init(config);
    }

    @Override
    public Object createProtocolEndpoint(KeycloakSession session, EventBuilder event) {
        return new OIDC4VCLoginProtocolService(session, event, providerConfig);
    }
}
