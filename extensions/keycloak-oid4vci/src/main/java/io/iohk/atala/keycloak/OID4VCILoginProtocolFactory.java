package io.iohk.atala.keycloak;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.OIDCProviderConfig;

public class OID4VCILoginProtocolFactory extends OIDCLoginProtocolFactory {
    private static final Logger logger = Logger.getLogger(OID4VCILoginProtocolFactory.class);
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
        return new OID4VCILoginProtocolService(session, event, providerConfig);
    }
}
