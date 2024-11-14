package steps.connection

import abilities.ListenToEvents
import interactions.Get
import interactions.Post
import interactions.body
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import io.iohk.atala.automation.serenity.interactions.PollingWait
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus.SC_CREATED
import org.apache.http.HttpStatus.SC_OK
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers
import org.hyperledger.identus.client.models.AcceptConnectionInvitationRequest
import org.hyperledger.identus.client.models.Connection
import org.hyperledger.identus.client.models.Connection.State.CONNECTION_RESPONSE_RECEIVED
import org.hyperledger.identus.client.models.Connection.State.CONNECTION_RESPONSE_SENT
import org.hyperledger.identus.client.models.Connection.State.INVITATION_GENERATED
import org.hyperledger.identus.client.models.CreateConnectionRequest

class ConnectionSteps {

    @When("{actor} generates a connection invitation to {actor}")
    fun inviterGeneratesAConnectionInvitation(inviter: Actor, invitee: Actor) {
        // Acme(Issuer) initiates a connection
        // and sends it to Bob(Holder) out-of-band, e.g. using QR-code
        val connectionLabel = "Connection with ${invitee.name}"

        inviter.attemptsTo(
            Post.to("/connections").body(CreateConnectionRequest(label = connectionLabel)),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
        )

        val connection = SerenityRest.lastResponse().get<Connection>()
        inviter.attemptsTo(
            Ensure.that(connection.label!!).isEqualTo(connectionLabel),
            Ensure.that(connection.state).isEqualTo(Connection.State.INVITATION_GENERATED),
            Ensure.that(connection.role).isEqualTo(Connection.Role.INVITER),
        )

        // Acme remembers connection to send it out of band to Bob
        inviter.remember("connection", connection)
    }

    @When("{actor} sends a connection request to {actor}")
    fun inviteeSendsAConnectionRequestToInviter(invitee: Actor, inviter: Actor) {
        // Bob accepts connection using achieved out-of-band invitation
        val inviterConnection = inviter.recall<Connection>("connection")
        val body = AcceptConnectionInvitationRequest(inviterConnection.invitation.invitationUrl.split("=")[1])
        invitee.attemptsTo(
            Post.to("/connection-invitations").body(body),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
        )

        val inviteeConnection = SerenityRest.lastResponse().get<Connection>()
        invitee.attemptsTo(
            Ensure.that(inviteeConnection.invitation.from).isEqualTo(inviterConnection.invitation.from),
            Ensure.that(inviteeConnection.invitation.id).isEqualTo(inviterConnection.invitation.id),
            Ensure.that(inviteeConnection.invitation.invitationUrl)
                .isEqualTo(inviterConnection.invitation.invitationUrl),
            Ensure.that(inviteeConnection.invitation.type).isEqualTo(inviterConnection.invitation.type),
            Ensure.that(inviteeConnection.state).isEqualTo(Connection.State.CONNECTION_REQUEST_PENDING),
            Ensure.that(inviteeConnection.role).isEqualTo(Connection.Role.INVITEE),
        )

        invitee.remember("connection", inviteeConnection)
    }

    @When("{actor} receives the connection request and sends back the response")
    fun inviterReceivesTheConnectionRequest(inviter: Actor) {
        inviter.attemptsTo(
            PollingWait.until(
                ListenToEvents.connectionState(inviter),
                CoreMatchers.equalTo(CONNECTION_RESPONSE_SENT),
            ),
        )
    }

    @When("{actor} receives the connection response")
    fun inviteeReceivesTheConnectionResponse(invitee: Actor) {
        invitee.attemptsTo(
            PollingWait.until(
                ListenToEvents.connectionState(invitee),
                CoreMatchers.equalTo(CONNECTION_RESPONSE_RECEIVED),
            ),
        )
    }

    @Then("{actor} and {actor} have a connection")
    fun inviterAndInviteeHaveAConnection(inviter: Actor, invitee: Actor) {
        // Connection established. Both parties exchanged their DIDs with each other
        inviter.attemptsTo(
            Get.resource("/connections/${inviter.recall<Connection>("connection").connectionId}"),
        )
        inviter.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
        )
        inviter.remember("connection-with-${invitee.name}", SerenityRest.lastResponse().get<Connection>())

        invitee.attemptsTo(
            Get.resource("/connections/${invitee.recall<Connection>("connection").connectionId}"),
        )
        invitee.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
        )
        invitee.remember("connection-with-${inviter.name}", SerenityRest.lastResponse().get<Connection>())

        assertThat(inviter.recall<Connection>("connection-with-${invitee.name}").myDid)
            .isEqualTo(invitee.recall<Connection>("connection-with-${inviter.name}").theirDid)
        assertThat(inviter.recall<Connection>("connection-with-${invitee.name}").theirDid)
            .isEqualTo(invitee.recall<Connection>("connection-with-${inviter.name}").myDid)
        assertThat(inviter.recall<Connection>("connection-with-${invitee.name}").state)
            .isEqualTo(CONNECTION_RESPONSE_SENT)
        assertThat(invitee.recall<Connection>("connection-with-${inviter.name}").state)
            .isEqualTo(CONNECTION_RESPONSE_RECEIVED)
    }
}
