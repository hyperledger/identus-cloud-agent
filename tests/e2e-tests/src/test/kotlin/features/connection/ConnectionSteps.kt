package features.connection

import api_models.Connection
import api_models.Invitation
import common.Agents.Acme
import common.Agents.Bob
import common.Utils.lastResponseObject
import common.Utils.wait
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.interactions.Get
import net.serenitybdd.screenplay.rest.interactions.Post
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
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
                }
        )
        inviter.should(
            ResponseConsequence.seeThatResponse("Generates connection request") { response ->
                response.statusCode(201)
                response.body("connectionId", notNullValue())
                response.body("createdAt", notNullValue())
                response.body("invitation", notNullValue())
                response.body("label", containsString(connectionLabel))
                response.body("state", containsString("InvitationGenerated"))
            }
        )
        // Acme remembers invitation URL to send it out of band to Bob
        inviter.remember(
            "invitationUrl",
            lastResponseObject("", Connection::class)
                .invitation.invitationUrl.split("=")[1]
        )
        inviter.remember(
            "invitation",
            lastResponseObject("invitation", Invitation::class)
        )

        // Acme remembers its connection ID for further use
        inviter.remember(
            "connectionId",
            lastResponseObject("", Connection::class)
                .connectionId
        )
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
                }
        )
        val acmeInvitation = inviter.recall<Invitation>("invitation")
        invitee.should(
            ResponseConsequence.seeThatResponse("Accepts connection request") { response ->
                response.statusCode(200)
                response.body("connectionId", notNullValue())
                response.body("createdAt", notNullValue())
                response.body("myDid", notNullValue())
                response.body("theirDid", notNullValue())
                response.body("invitation.from", containsString(acmeInvitation.from))
                response.body("invitation.id", containsString(acmeInvitation.id))
                response.body("invitation.invitationUrl", containsString(acmeInvitation.invitationUrl))
                response.body("invitation.type", containsString(acmeInvitation.type))
                response.body("state", containsString("ConnectionRequestPending"))
            }
        )
        invitee.remember("connectionId", lastResponseObject("", Connection::class).connectionId)
    }

    @When("{actor} receives the connection request")
    fun inviterReceivesTheConnectionRequest(inviter: Actor) {
        wait(
            {
                inviter.attemptsTo(
                    Get.resource("/connections/${inviter.recall<String>("connectionId")}"),
                )
                inviter.should(
                    ResponseConsequence.seeThatResponse("Get connection ${inviter.recall<String>("connectionId")}") {
                        it.statusCode(200)
                    }
                )
                lastResponseObject("", Connection::class).state == "ConnectionResponsePending"
            },
            "Issuer connection didn't reach ConnectionResponsePending state."
        )
    }

    @When("{actor} sends a connection response to {actor}")
    fun inviterSendsAConnectionResponseToInvitee(inviter: Actor, invitee: Actor) {
        // Acme(Issuer) checks their connections to check if invitation was accepted by Bob(Holder)
        // and sends final connection response
        wait(
            {
                inviter.attemptsTo(
                    Get.resource("/connections/${inviter.recall<String>("connectionId")}"),
                )
                inviter.should(
                    ResponseConsequence.seeThatResponse("Get connection ${inviter.recall<String>("connectionId")}") {
                        it.statusCode(200)
                    }
                )
                lastResponseObject("", Connection::class).state == "ConnectionResponseSent"
            },
            "Issuer connection didn't reach ConnectionResponseSent state."
        )
    }

    @When("{actor} receives the connection response")
    fun inviteeReceivesTheConnectionResponse(invitee: Actor) {
        // Bob (Holder) receives final connection response
        wait(
            {
                invitee.attemptsTo(
                    Get.resource("/connections/${invitee.recall<String>("connectionId")}")
                )
                invitee.should(
                    ResponseConsequence.seeThatResponse("Get connection ${invitee.recall<String>("connectionId")}") {
                        it.statusCode(200)
                    }
                )
                lastResponseObject("", Connection::class).state == "ConnectionResponseReceived"
            },
            "Holder connection didn't reach ConnectionResponseReceived state."
        )
    }

    @Then("{actor} and {actor} have a connection")
    fun inviterAndInviteeHaveAConnection(inviter: Actor, invitee: Actor) {
        // Connection established. Both parties exchanged their DIDs with each other
        inviter.attemptsTo(
            Get.resource("/connections/${inviter.recall<String>("connectionId")}"),
        )
        inviter.should(
            ResponseConsequence.seeThatResponse("Get connection ${inviter.recall<String>("connectionId")}") {
                it.statusCode(200)
            }
        )
        inviter.remember("connection-with-${invitee.name}", lastResponseObject("", Connection::class))

        invitee.attemptsTo(
            Get.resource("/connections/${invitee.recall<String>("connectionId")}")
        )
        invitee.should(
            ResponseConsequence.seeThatResponse("Get connection ${invitee.recall<String>("connectionId")}") {
                it.statusCode(200)
            }
        )
        invitee.remember("connection-with-${inviter.name}", lastResponseObject("", Connection::class))

        assertThat(inviter.recall<Connection>("connection-with-${invitee.name}").myDid)
            .isEqualTo(invitee.recall<Connection>("connection-with-${inviter.name}").theirDid)
        assertThat(inviter.recall<Connection>("connection-with-${invitee.name}").theirDid)
            .isEqualTo(invitee.recall<Connection>("connection-with-${inviter.name}").myDid)
        assertThat(inviter.recall<Connection>("connection-with-${invitee.name}").state)
            .isEqualTo("ConnectionResponseSent")
        assertThat(invitee.recall<Connection>("connection-with-${inviter.name}").state)
            .isEqualTo("ConnectionResponseReceived")
    }
}
