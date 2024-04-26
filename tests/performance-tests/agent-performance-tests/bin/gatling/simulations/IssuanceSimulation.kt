package simulations

import io.gatling.javaapi.core.*
import io.gatling.javaapi.core.CoreDsl.*
import steps.ConnectionSteps
import steps.IssuanceSteps

/**
 * Simulation for Atala PRISM V2 Credential Issuance protocol
 */
class IssuanceSimulation : Simulation() {

    private val issuingCredential = scenario("Issuing a credential").exec(
        ConnectionSteps.generateInvitation(),
        ConnectionSteps.inviteeSendsConnectionRequest(),
        ConnectionSteps.inviterReceivesTheConnectionRequestAndSendsTheConnectionResponseToInvitee(),
        ConnectionSteps.inviteeAchievesConnectionResponse(),
        IssuanceSteps.issuerOffersACredential(),
        IssuanceSteps.holderAwaitsCredentialOffer(),
        IssuanceSteps.holderRequestsCredential(),
        IssuanceSteps.issuerReceivesRequest(),
        IssuanceSteps.issuerIssuesCredential(),
        IssuanceSteps.issuerWaitsCredentialIssued(),
        IssuanceSteps.holderAwaitsCredentialReceived()
    )

    init {
        setUp(
            issuingCredential
                .injectOpen(rampUsers(1)
                    .during(1))
        )
    }
}
