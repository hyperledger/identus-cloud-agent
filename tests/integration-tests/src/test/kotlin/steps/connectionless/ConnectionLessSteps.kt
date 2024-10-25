package steps.connectionless

import interactions.Post
import interactions.body
import io.cucumber.java.en.*
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus.SC_CREATED
import org.apache.http.HttpStatus.SC_OK
import org.hyperledger.identus.client.models.*

class ConnectionLessSteps {

    @When("{actor} creates a {string} credential offer invitation with {string} form DID")
    fun inviterGeneratesACredentialOfferInvitation(issuer: Actor, credentialFormat: String, didForm: String) {
        val claims = linkedMapOf(
            "firstName" to "Automation",
            "lastName" to "Execution",
            "email" to "email@example.com",
        )
        val did: String = if (didForm == "short") {
            issuer.recall("shortFormDid")
        } else {
            issuer.recall("longFormDid")
        }
        val credentialOfferRequest = CreateIssueCredentialRecordRequest(
            claims = claims,
            issuingDID = did,
            validityPeriod = 3600.0,
            credentialFormat = credentialFormat,
            automaticIssuance = false,
            goalCode = "issue-vc",
            goal = "To issue a Faber College Graduate credential",
        )

        issuer.attemptsTo(
            Post.to("/issue-credentials/credential-offers/invitation").body(credentialOfferRequest),
        )

        val credentialRecord = SerenityRest.lastResponse().get<IssueCredentialRecord>()

        issuer.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
            Ensure.that(credentialRecord.goalCode!!).isEqualTo("issue-vc"),
            Ensure.that(credentialRecord.protocolState).isEqualTo(IssueCredentialRecord.ProtocolState.INVITATION_GENERATED),
            Ensure.that(credentialRecord.role).isEqualTo(IssueCredentialRecord.Role.ISSUER),
        )

        // Acme remembers connection to send it out of band to Bob
        issuer.remember("credentialRecord", credentialRecord)
        issuer.remember("thid", credentialRecord.thid)
    }

    @And("{actor} accepts the credential offer invitation from {actor}")
    fun holderAcceptsCredentialOfferInvitation(holder: Actor, issuer: Actor) {
        // Bob accepts connection using achieved out-of-band invitation
        val credentialOfferInvitationRecord = issuer.recall<IssueCredentialRecord>("credentialRecord")
        holder.attemptsTo(
            Post.to("/issue-credentials/credential-offers/accept-invitation")
                .with {
                    it.body(
                        AcceptCredentialOfferInvitation(
                            credentialOfferInvitationRecord.invitation?.invitationUrl?.split("=")?.getOrNull(1)
                                ?: throw IllegalStateException("Invalid invitation URL format"),
                        ),
                    )
                },
        )
        val holderIssueCredentialRecord = SerenityRest.lastResponse().get<IssueCredentialRecord>()

        holder.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
            Ensure.that(holderIssueCredentialRecord.protocolState).isEqualTo(IssueCredentialRecord.ProtocolState.OFFER_RECEIVED),
            Ensure.that(holderIssueCredentialRecord.role).isEqualTo(IssueCredentialRecord.Role.HOLDER),
        )
        holder.remember("recordId", holderIssueCredentialRecord.recordId)
        holder.remember("thid", holderIssueCredentialRecord.thid)
    }
}
