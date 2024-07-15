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
import steps.credentials.*
import steps.did.PublishDidSteps
import steps.schemas.CredentialSchemasSteps

class CommonSteps {
    @Given("{actor} has a jwt issued credential from {actor}")
    fun holderHasIssuedJwtCredentialFromIssuer(holder: Actor, issuer: Actor) {
        actorsHaveExistingConnection(issuer, holder)

        val publishDidSteps = PublishDidSteps()
        publishDidSteps.agentHasAnUnpublishedDID(holder, DidPurpose.JWT)
        publishDidSteps.agentHasAPublishedDID(issuer, DidPurpose.JWT)

        val jwtCredentialSteps = JwtCredentialSteps()
        val credentialSteps = CredentialSteps()

        jwtCredentialSteps.issuerOffersAJwtCredential(issuer, holder, "short")
        credentialSteps.holderReceivesCredentialOffer(holder)
        jwtCredentialSteps.holderAcceptsJwtCredentialOfferForJwt(holder)
        credentialSteps.issuerIssuesTheCredential(issuer)
        credentialSteps.holderReceivesTheIssuedCredential(holder)
    }

    @Given("{actor} has a jwt issued credential with {} schema from {actor}")
    fun holderHasIssuedJwtCredentialFromIssuerWithSchema(
        holder: Actor,
        schema: CredentialSchema,
        issuer: Actor,
    ) {
        actorsHaveExistingConnection(issuer, holder)

        val publishDidSteps = PublishDidSteps()
        publishDidSteps.agentHasAnUnpublishedDID(holder, DidPurpose.JWT)
        publishDidSteps.agentHasAPublishedDID(issuer, DidPurpose.JWT)

        val schemaSteps = CredentialSchemasSteps()
        schemaSteps.agentHasAPublishedSchema(issuer, schema)

        val jwtCredentialSteps = JwtCredentialSteps()
        val credentialSteps = CredentialSteps()
        jwtCredentialSteps.issuerOffersJwtCredentialToHolderUsingSchema(issuer, holder, "short", schema)
        credentialSteps.holderReceivesCredentialOffer(holder)
        jwtCredentialSteps.holderAcceptsJwtCredentialOfferForJwt(holder)
        credentialSteps.issuerIssuesTheCredential(issuer)
        credentialSteps.holderReceivesTheIssuedCredential(holder)
    }

    @Given("{actor} has a sd-jwt issued credential from {actor}")
    fun holderHasIssuedSdJwtCredentialFromIssuer(holder: Actor, issuer: Actor) {
        actorsHaveExistingConnection(issuer, holder)

        val publishDidSteps = PublishDidSteps()
        publishDidSteps.agentHasAnUnpublishedDID(holder, DidPurpose.SD_JWT)
        publishDidSteps.agentHasAPublishedDID(issuer, DidPurpose.SD_JWT)

        val sdJwtCredentialSteps = SdJwtCredentialSteps()
        val credentialSteps = CredentialSteps()
        sdJwtCredentialSteps.issuerOffersSdJwtCredentialToHolder(issuer, holder)
        credentialSteps.holderReceivesCredentialOffer(holder)
        sdJwtCredentialSteps.holderAcceptsSdJwtCredentialOffer(holder)
        credentialSteps.issuerIssuesTheCredential(issuer)
        credentialSteps.holderReceivesTheIssuedCredential(holder)
    }

    @Given("{actor} has a bound sd-jwt issued credential from {actor}")
    fun holderHasIssuedSdJwtCredentialFromIssuerWithKeyBind(holder: Actor, issuer: Actor) {
        actorsHaveExistingConnection(issuer, holder)

        val publishDidSteps = PublishDidSteps()
        publishDidSteps.agentHasAnUnpublishedDID(holder, DidPurpose.SD_JWT)
        publishDidSteps.agentHasAPublishedDID(issuer, DidPurpose.SD_JWT)

        val sdJwtCredentialSteps = SdJwtCredentialSteps()
        val credentialSteps = CredentialSteps()
        sdJwtCredentialSteps.issuerOffersSdJwtCredentialToHolder(issuer, holder)
        credentialSteps.holderReceivesCredentialOffer(holder)
        sdJwtCredentialSteps.holderAcceptsSdJwtCredentialOfferWithKeyBinding(holder, "auth-1")
        credentialSteps.issuerIssuesTheCredential(issuer)
        credentialSteps.holderReceivesTheIssuedCredential(holder)
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
