package features

import api_models.Connection
import api_models.Credential
import common.Agents.Acme
import common.Agents.Bob
import common.Agents.Faber
import common.Agents.Mallory
import common.Agents.createAgents
import common.Utils.lastResponseList
import features.connection.ConnectionSteps
import features.issue_credentials.IssueCredentialsSteps
import io.cucumber.java.Before
import io.cucumber.java.ParameterType
import io.cucumber.java.en.Given
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.actors.Cast
import net.serenitybdd.screenplay.actors.OnStage
import net.serenitybdd.screenplay.rest.interactions.Get
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import org.apache.http.HttpStatus.SC_OK

class CommonSteps {
    @Before
    fun setStage() {
        createAgents()
        val cast = object: Cast() {
            override fun getActors(): MutableList<Actor> {
                return mutableListOf(Acme, Bob, Mallory, Faber)
            }
        }
        OnStage.setTheStage(cast)
    }

    @ParameterType(".*")
    fun actor(actorName: String): Actor {
        return OnStage.theActorCalled(actorName);
    }

    @Given("{actor} has an issued credential from {actor}")
    fun holderHasIssuedCredentialFromIssuer(holder: Actor, issuer: Actor) {
        holder.attemptsTo(
            Get.resource("/issue-credentials/records")
        )
        holder.should(
            ResponseConsequence.seeThatResponse("Credential records") {
                it.statusCode(SC_OK)
            }
        )
        val receivedCredential = lastResponseList("items", Credential::class).findLast { credential ->
            credential.protocolState == "CredentialReceived"
        }

        if (receivedCredential != null) {
            holder.remember("issuedCredential", receivedCredential)
        } else {
            val issueSteps = IssueCredentialsSteps()
            actorsHaveExistingConnection(issuer, holder)
            issueSteps.acmeOffersACredential(issuer, holder)
            issueSteps.bobRequestsTheCredential(holder)
            issueSteps.acmeIssuesTheCredential(issuer)
            issueSteps.bobHasTheCredentialIssued(holder)
        }
    }

    @Given("{actor} and {actor} have an existing connection")
    fun actorsHaveExistingConnection(inviter: Actor, invitee: Actor) {
        inviter.attemptsTo(
            Get.resource("/connections")
        )
        inviter.should(
            ResponseConsequence.seeThatResponse("Get connections") {
                it.statusCode(200)
            }
        )
        val inviterConnection = lastResponseList("contents", Connection::class).firstOrNull {
            it.label == "Connection with ${invitee.name}" &&
                    (it.state == "ConnectionResponseSent" || it.state == "ConnectionResponseReceived")
        }

        var inviteeConnection: Connection? = null
        if (inviterConnection != null) {
            invitee.attemptsTo(
                Get.resource("/connections")
            )
            invitee.should(
                ResponseConsequence.seeThatResponse("Get connections") {
                    it.statusCode(SC_OK)
                }
            )
            inviteeConnection = lastResponseList("contents", Connection::class).firstOrNull {
                it.theirDid == inviterConnection.myDid
            }
        }

        if (inviterConnection != null && inviteeConnection != null) {
            inviter.remember("connection-with-${invitee.name}", inviterConnection)
            invitee.remember("connection-with-${inviter.name}", inviteeConnection)
        } else {
            val connectionSteps = ConnectionSteps()
            connectionSteps.inviterGeneratesAConnectionInvitation(inviter, invitee)
            connectionSteps.inviteeReceivesTheConnectionInvitation(invitee, inviter)
            connectionSteps.inviteeSendsAConnectionRequestToInviter(invitee, inviter)
            connectionSteps.inviterReceivesTheConnectionRequest(inviter)
            connectionSteps.inviterSendsAConnectionResponseToInvitee(inviter, invitee)
            connectionSteps.inviteeReceivesTheConnectionResponse(invitee)
            connectionSteps.inviterAndInviteeHaveAConnection(inviter, invitee)
        }
    }
}
