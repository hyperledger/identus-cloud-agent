package features.connection

import api_models.Connection
import api_models.Invitation
import common.Agents.Acme
import common.Agents.Bob
import common.Utils.lastResponseObject
import common.Utils.wait
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.rest.interactions.Get
import net.serenitybdd.screenplay.rest.interactions.Post
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.*

class ConnectionSteps {

    @When("Acme generates a connection invitation")
    fun inviterGeneratesAConnectionInvitation() {
        // Acme(Issuer) initiates a connection
        // and sends it to Bob(Holder) out-of-band, e.g. using QR-code
        val connectionLabel = "Connection with ${Bob.name}"
        Acme.attemptsTo(
            Post.to("/connections")
                .with {
                    it.header("Content-Type", "application/json")
                    it.body("""{"label": "$connectionLabel"}""")
                }
        )
        Acme.should(
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
        Acme.remember(
            "invitationUrl",
            lastResponseObject("", Connection::class)
                .invitation.invitationUrl.split("=")[1]
        )
        Acme.remember(
            "invitation",
            lastResponseObject("invitation", Invitation::class)
        )

        // Acme remembers its connection ID for further use
        Acme.remember(
            "connectionId",
            lastResponseObject("", Connection::class)
                .connectionId
        )
    }

    @When("Bob receives the connection invitation")
    fun inviteeReceivesTheConnectionInvitation() {
        // Here out of band transfer of connection QR code is happening
        // and Bob (Holder) gets an invitation URL
        // they're accepting connection invitation by POST request specifying achieved invitation
        // we demonstrate it by Bob remembering invitationUrl that Acme recalls
        Bob.remember("invitationUrl", Acme.recall<String>("invitationUrl"))
    }

    @When("Bob sends a connection request to Acme")
    fun inviteeSendsAConnectionRequestToInviter() {
        // Bob accepts connection using achieved out-of-band invitation
        Bob.attemptsTo(
            Post.to("/connection-invitations")
                .with {
                    it.header("Content-Type", "application/json")
                    it.body("""{"invitation": "${Bob.recall<String>("invitationUrl")}"}""")
                }
        )
        val acmeInvitation = Acme.recall<Invitation>("invitation")
        Bob.should(
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
        Bob.remember("connectionId", lastResponseObject("", Connection::class).connectionId)
    }

    @When("Acme receives the connection request")
    fun inviterReceivesTheConnectionRequest() {
        wait(
            {
                Acme.attemptsTo(
                    Get.resource("/connections/${Acme.recall<String>("connectionId")}"),
                )
                Acme.should(
                    ResponseConsequence.seeThatResponse("Get connection ${Acme.recall<String>("connectionId")}") {
                        it.statusCode(200)
                    }
                )
                lastResponseObject("", Connection::class).state == "ConnectionResponsePending"
            },
            "Issuer connection didn't reach ConnectionResponsePending state."
        )
    }

    @When("Acme sends a connection response to Bob")
    fun inviterSendsAConnectionResponseToInvitee() {
        // Acme(Issuer) checks their connections to check if invitation was accepted by Bob(Holder)
        // and sends final connection response
        wait(
            {
                Acme.attemptsTo(
                    Get.resource("/connections/${Acme.recall<String>("connectionId")}"),
                )
                Acme.should(
                    ResponseConsequence.seeThatResponse("Get connection ${Acme.recall<String>("connectionId")}") {
                        it.statusCode(200)
                    }
                )
                lastResponseObject("", Connection::class).state == "ConnectionResponseSent"
            },
            "Issuer connection didn't reach ConnectionResponseSent state."
        )
    }

    @When("Bob receives the connection response")
    fun inviteeReceivesTheConnectionResponse() {
        // Bob (Holder) receives final connection response
        wait(
            {
                Bob.attemptsTo(
                    Get.resource("/connections/${Bob.recall<String>("connectionId")}")
                )
                Bob.should(
                    ResponseConsequence.seeThatResponse("Get connection ${Bob.recall<String>("connectionId")}") {
                        it.statusCode(200)
                    }
                )
                lastResponseObject("", Connection::class).state == "ConnectionResponseReceived"
            },
            "Holder connection didn't reach ConnectionResponseReceived state."
        )
    }

    @Then("Acme and Bob have a connection")
    fun inviterAndInviteeHaveAConnection() {
        // Connection established. Both parties exchanged their DIDs with each other
        Acme.attemptsTo(
            Get.resource("/connections/${Acme.recall<String>("connectionId")}"),
        )
        Acme.should(
            ResponseConsequence.seeThatResponse("Get connection ${Acme.recall<String>("connectionId")}") {
                it.statusCode(200)
            }
        )
        Acme.remember("connection", lastResponseObject("", Connection::class))

        Bob.attemptsTo(
            Get.resource("/connections/${Bob.recall<String>("connectionId")}")
        )
        Bob.should(
            ResponseConsequence.seeThatResponse("Get connection ${Bob.recall<String>("connectionId")}") {
                it.statusCode(200)
            }
        )
        Bob.remember("connection", lastResponseObject("", Connection::class))

        assertThat(Acme.recall<Connection>("connection").myDid)
            .isEqualTo(Bob.recall<Connection>("connection").theirDid)
        assertThat(Acme.recall<Connection>("connection").theirDid)
            .isEqualTo(Bob.recall<Connection>("connection").myDid)
        assertThat(Acme.recall<Connection>("connection").state)
            .isEqualTo("ConnectionResponseSent")
        assertThat(Bob.recall<Connection>("connection").state)
            .isEqualTo("ConnectionResponseReceived")
    }
}
