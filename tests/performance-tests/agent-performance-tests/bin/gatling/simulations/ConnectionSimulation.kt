package simulations

import io.gatling.javaapi.core.*
import io.gatling.javaapi.core.CoreDsl.*
import steps.ConnectionSteps

/**
 * Simulation for Atala PRISM V2 Connection protocol
 */
class ConnectionSimulation : Simulation() {

    private val settingUpConnection = scenario("Setting up connection").exec(
        ConnectionSteps.generateInvitation(),
        ConnectionSteps.inviteeSendsConnectionRequest(),
        ConnectionSteps.inviterReceivesTheConnectionRequestAndSendsTheConnectionResponseToInvitee(),
        ConnectionSteps.inviteeAchievesConnectionResponse()
    )

    init {
        setUp(
            settingUpConnection
                .injectOpen(rampUsers(1)
                    .during(1))
        )
    }
}
