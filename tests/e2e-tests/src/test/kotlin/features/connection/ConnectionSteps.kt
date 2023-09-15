package features.connection

import api_models.*
import common.ListenToEvents
import common.Utils.lastResponseObject
import common.Utils.wait
import interactions.Get
import interactions.Post
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import org.apache.http.HttpStatus.SC_CREATED
import org.apache.http.HttpStatus.SC_OK
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.*

class ConnectionSteps {

    @When("{actor} generates a connection invitation to {actor}")
    fun inviterGeneratesAConnectionInvitation(inviter: Actor, invitee: Actor) {
        // Acme(Issuer) initiates a connection
        // and sends it to Bob(Holder) out-of-band, e.g. using QR-code
        val connectionLabel = "Connection with ${invitee.name}"
        inviter.attemptsTo(
            Post.to("/connections")
                .with {
                    it.body("""{"label": "$connectionLabel"}""")
                },
        )
        inviter.should(
            ResponseConsequence.seeThatResponse { response ->
                response.statusCode(SC_CREATED)
                response.body("connectionId", notNullValue())
                response.body("createdAt", notNullValue())
                response.body("invitation", notNullValue())
                response.body("label", containsString(connectionLabel))
                response.body("state", containsString(ConnectionState.INVITATION_GENERATED))
                response.body("role", containsString("Inviter"))
            },
        )
        // Acme remembers invitation URL to send it out of band to Bob
        inviter.remember(
            "invitationUrl",
            lastResponseObject("", Connection::class)
                .invitation.invitationUrl.split("=")[1],
        )
        inviter.remember(
            "invitation",
            lastResponseObject("invitation", Invitation::class),
        )

        // Acme remembers its connection ID for further use
        inviter.remember(
            "connectionId",
            lastResponseObject("", Connection::class)
                .connectionId,
        )
        inviter.remember("thid", lastResponseObject("", Connection::class).thid)
    }

    @When("{actor} receives the connection invitation from {actor}")
    fun inviteeReceivesTheConnectionInvitation(invitee: Actor, inviter: Actor) {
        // Here out of band transfer of connection QR code is happening
        // and Bob (Holder) gets an invitation URL
        // they're accepting connection invitation by POST request specifying achieved invitation
        // we demonstrate it by Bob remembering invitationUrl that Acme recalls
        invitee.remember("invitationUrl", inviter.recall<String>("invitationUrl"))
    }

    @When("{actor} sends a connection request to {actor}")
    fun inviteeSendsAConnectionRequestToInviter(invitee: Actor, inviter: Actor) {
        // Bob accepts connection using achieved out-of-band invitation
        invitee.attemptsTo(
            Post.to("/connection-invitations")
                .with {
                    it.body("""{"invitation": "${invitee.recall<String>("invitationUrl")}"}""")
                },
        )
        val acmeInvitation = inviter.recall<Invitation>("invitation")
        invitee.should(
            ResponseConsequence.seeThatResponse { response ->
                response.statusCode(SC_OK)
                response.body("connectionId", notNullValue())
                response.body("createdAt", notNullValue())
                response.body("myDid", notNullValue())
                response.body("theirDid", notNullValue())
                response.body("invitation.from", containsString(acmeInvitation.from))
                response.body("invitation.id", containsString(acmeInvitation.id))
                response.body("invitation.invitationUrl", containsString(acmeInvitation.invitationUrl))
                response.body("invitation.type", containsString(acmeInvitation.type))
                response.body("state", containsString(ConnectionState.CONNECTION_REQUEST_PENDING))
                response.body("role", containsString("Invitee"))
            },
        )
        invitee.remember("connectionId", lastResponseObject("", Connection::class).connectionId)
        invitee.remember("thid", lastResponseObject("", Connection::class).thid)
    }

    @When("{actor} receives the connection request and sends back the response")
    fun inviterReceivesTheConnectionRequest(inviter: Actor) {
        wait(
            {
                val lastEvent = ListenToEvents.`as`(inviter).connectionEvents.lastOrNull {
                    it.data.thid == inviter.recall<String>("thid")
                }
                lastEvent != null &&
                        lastEvent.data.state == ConnectionState.CONNECTION_RESPONSE_SENT
            },
            "Inviter connection didn't reach ${ConnectionState.CONNECTION_RESPONSE_SENT} state",
        )
    }

    @When("{actor} receives the connection response")
    fun inviteeReceivesTheConnectionResponse(invitee: Actor) {
        // Bob (Holder) receives final connection response
        wait(
            {
                val lastEvent = ListenToEvents.`as`(invitee).connectionEvents.lastOrNull {
                    it.data.thid == invitee.recall<String>("thid")
                }
                lastEvent != null &&
                        lastEvent.data.state == ConnectionState.CONNECTION_RESPONSE_RECEIVED
            },
            "Invitee connection didn't reach ${ConnectionState.CONNECTION_RESPONSE_RECEIVED} state.",
        )
    }

    @Then("{actor} and {actor} have a connection")
    fun inviterAndInviteeHaveAConnection(inviter: Actor, invitee: Actor) {
        // Connection established. Both parties exchanged their DIDs with each other
        inviter.attemptsTo(
            Get.resource("/connections/${inviter.recall<String>("connectionId")}"),
        )
        inviter.should(
            ResponseConsequence.seeThatResponse {
                it.statusCode(SC_OK)
            },
        )
        inviter.remember("connection-with-${invitee.name}", lastResponseObject("", Connection::class))

        invitee.attemptsTo(
            Get.resource("/connections/${invitee.recall<String>("connectionId")}"),
        )
        invitee.should(
            ResponseConsequence.seeThatResponse {
                it.statusCode(SC_OK)
            },
        )
        invitee.remember("connection-with-${inviter.name}", lastResponseObject("", Connection::class))

        assertThat(inviter.recall<Connection>("connection-with-${invitee.name}").myDid)
            .isEqualTo(invitee.recall<Connection>("connection-with-${inviter.name}").theirDid)
        assertThat(inviter.recall<Connection>("connection-with-${invitee.name}").theirDid)
            .isEqualTo(invitee.recall<Connection>("connection-with-${inviter.name}").myDid)
        assertThat(inviter.recall<Connection>("connection-with-${invitee.name}").state)
            .isEqualTo(ConnectionState.CONNECTION_RESPONSE_SENT)
        assertThat(invitee.recall<Connection>("connection-with-${inviter.name}").state)
            .isEqualTo(ConnectionState.CONNECTION_RESPONSE_RECEIVED)
    }
}
