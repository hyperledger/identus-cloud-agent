package io.iohk.atala.keycloak;

import io.iohk.atala.keycloak.model.NonceResponse;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint;
import org.keycloak.protocol.oidc.endpoints.TokenEndpoint;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.clientpolicy.ClientPolicyContext;

import java.util.function.Function;

public class OIDC4VCTokenEndpoint extends TokenEndpoint {
    private static final Logger logger = Logger.getLogger(OIDC4VCTokenEndpoint.class);

    private final IdentusClient identusClient;

    public OIDC4VCTokenEndpoint(KeycloakSession session, TokenManager tokenManager, EventBuilder event) {
        super(session, tokenManager, event);
        this.identusClient = new IdentusClient();
    }

    @Override
    public Response createTokenResponse(UserModel user, UserSessionModel userSession, ClientSessionContext clientSessionCtx,
                                        String scopeParam, boolean code, Function<TokenManager.AccessTokenResponseBuilder, ClientPolicyContext> clientPolicyContextGenerator) {
        String noteKey = AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + OIDC4VCConstants.ISSUER_STATE;
        String issuerState = clientSessionCtx.getClientSession().getNote(noteKey);

        if (code && issuerState != null) {
            Response originalResponse = super.createTokenResponse(user, userSession, clientSessionCtx, scopeParam, true, clientPolicyContextGenerator);

            logger.warn("TokenEndpoint issuer_state: " + issuerState);
            logger.warn("TokenEndpoint userSession ID: " + userSession.getId());

            NonceResponse nonceResponse = identusClient.syncTokenDetails(issuerState);
            AccessTokenResponse responseEntity = (AccessTokenResponse) originalResponse.getEntity();
            responseEntity.setOtherClaims(OIDC4VCConstants.C_NONCE, nonceResponse.getNonce());
            responseEntity.setOtherClaims(OIDC4VCConstants.C_NONCE_EXPIRE, nonceResponse.getNonceExpiresIn());
            return Response.fromResponse(originalResponse)
                    .entity(responseEntity)
                    .build();
        } else {
            return super.createTokenResponse(user, userSession, clientSessionCtx, scopeParam, false, clientPolicyContextGenerator);
        }
    }
}
