package steps.common

import common.CredentialSchema
import common.DidPurpose
import interactions.Get
import io.cucumber.java.en.Given
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus
import org.hyperledger.identus.client.models.Connection
import org.hyperledger.identus.client.models.ConnectionsPage
import steps.connection.ConnectionSteps
import steps.credentials.IssueCredentialsSteps
import steps.did.PublishDidSteps
import steps.schemas.CredentialSchemasSteps

class CommonSteps {
    @Given("{actor} has a jwt issued credential from {actor}")
    fun holderHasIssuedCredentialFromIssuer(holder: Actor, issuer: Actor) {
        actorsHaveExistingConnection(issuer, holder)

        val publishDidSteps = PublishDidSteps()
        publishDidSteps.agentHasAnUnpublishedDID(holder, DidPurpose.JWT)
        publishDidSteps.agentHasAPublishedDID(issuer, DidPurpose.JWT)

        val issueSteps = IssueCredentialsSteps()
        issueSteps.issuerOffersACredential(issuer, holder, "short")
        issueSteps.holderReceivesCredentialOffer(holder)
        issueSteps.holderAcceptsCredentialOfferForJwt(holder)
        issueSteps.acmeIssuesTheCredential(issuer)
        issueSteps.bobHasTheCredentialIssued(holder)
    }

    @Given("{actor} has a jwt issued credential with {} schema from {actor}")
    fun holderHasIssuedCredentialFromIssuerWithSchema(
        holder: Actor,
        schema: CredentialSchema,
        issuer: Actor
    ) {
        actorsHaveExistingConnection(issuer, holder)

        val publishDidSteps = PublishDidSteps()
        publishDidSteps.agentHasAnUnpublishedDID(holder, DidPurpose.JWT)
        publishDidSteps.agentHasAPublishedDID(issuer, DidPurpose.JWT)

        val schemaSteps = CredentialSchemasSteps()
        schemaSteps.agentHasAPublishedSchema(issuer, schema)

        val issueSteps = IssueCredentialsSteps()
        issueSteps.issuerOffersCredentialToHolderUsingSchema(issuer, holder, "short", schema)
        issueSteps.holderReceivesCredentialOffer(holder)
        issueSteps.holderAcceptsCredentialOfferForJwt(holder)
        issueSteps.acmeIssuesTheCredential(issuer)
        issueSteps.bobHasTheCredentialIssued(holder)
    }

    @Given("{actor} and {actor} have an existing connection")
    fun actorsHaveExistingConnection(inviter: Actor, invitee: Actor) {
        inviter.attemptsTo(
            Get.resource("/connections"),
        )
        inviter.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_OK),
        )
        val inviterConnection = SerenityRest.lastResponse().get<ConnectionsPage>().contents!!.firstOrNull {
            it.label == "Connection with ${invitee.name}" && it.state == Connection.State.CONNECTION_RESPONSE_SENT
        }

        var inviteeConnection: Connection? = null
        if (inviterConnection != null) {
            invitee.attemptsTo(
                Get.resource("/connections"),
            )
            invitee.attemptsTo(
                Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_OK),
            )
            inviteeConnection = SerenityRest.lastResponse().get<ConnectionsPage>().contents!!.firstOrNull {
                it.theirDid == inviterConnection.myDid && it.state == Connection.State.CONNECTION_RESPONSE_RECEIVED
            }
        }

        if (inviterConnection != null && inviteeConnection != null) {
            inviter.remember("connection-with-${invitee.name}", inviterConnection)
            invitee.remember("connection-with-${inviter.name}", inviteeConnection)
        } else {
            val connectionSteps = ConnectionSteps()
            connectionSteps.inviterGeneratesAConnectionInvitation(inviter, invitee)
            connectionSteps.inviteeSendsAConnectionRequestToInviter(invitee, inviter)
            connectionSteps.inviterReceivesTheConnectionRequest(inviter)
            connectionSteps.inviteeReceivesTheConnectionResponse(invitee)
            connectionSteps.inviterAndInviteeHaveAConnection(inviter, invitee)
        }
    }
}
