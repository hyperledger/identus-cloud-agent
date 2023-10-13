package features.credentials

import common.ListenToEvents
import common.Utils.wait
import interactions.Post
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import io.iohk.atala.prism.models.AcceptCredentialOfferRequest
import io.iohk.atala.prism.models.Connection
import io.iohk.atala.prism.models.CreateIssueCredentialRecordRequest
import io.iohk.atala.prism.models.IssueCredentialRecord
import models.CredentialEvent
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus.SC_CREATED
import org.apache.http.HttpStatus.SC_OK

class IssueCredentialsSteps {

    var credentialEvent: CredentialEvent? = null

    @When("{actor} offers a credential to {actor} with {string} form DID")
    fun acmeOffersACredential(issuer: Actor, holder: Actor, didForm: String) {
        val did: String = if (didForm == "short") {
            issuer.recall("shortFormDid")
        } else {
            issuer.recall("longFormDid")
        }

        val credentialOfferRequest = CreateIssueCredentialRecordRequest(
            schemaId = null,
            claims = linkedMapOf(
                "firstName" to "FirstName",
                "lastName" to "LastName"
            ),
            issuingDID = did,
            connectionId = issuer.recall<Connection>("connection-with-${holder.name}").connectionId.toString(),
            validityPeriod = 3600.0,
            automaticIssuance = false
        )

        issuer.attemptsTo(
            Post.to("/issue-credentials/credential-offers")
                .with {
                    it.body(credentialOfferRequest)
                }
        )

        val credentialRecord = SerenityRest.lastResponse().get<IssueCredentialRecord>()

        issuer.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED)
        )

        issuer.remember("thid", credentialRecord.thid)
        holder.remember("thid", credentialRecord.thid)
    }

    @When("{actor} receives the credential offer and accepts")
    fun bobRequestsTheCredential(holder: Actor) {
        wait(
            {
                credentialEvent = ListenToEvents.`as`(holder).credentialEvents.lastOrNull {
                    it.data.thid == holder.recall<String>("thid")
                }
                credentialEvent != null &&
                    credentialEvent!!.data.protocolState == IssueCredentialRecord.ProtocolState.offerReceived
            },
            "Holder was unable to receive the credential offer from Issuer! Protocol state did not achieve OfferReceived state."
        )

        val recordId = ListenToEvents.`as`(holder).credentialEvents.last().data.recordId
        holder.remember("recordId", recordId)

        holder.attemptsTo(
            Post.to("/issue-credentials/records/$recordId/accept-offer")
                .with {
                    it.body(
                        AcceptCredentialOfferRequest(holder.recall("longFormDid"))
                    )
                }
        )
        holder.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK)
        )
    }

    @When("{actor} issues the credential")
    fun acmeIssuesTheCredential(issuer: Actor) {
        wait(
            {
                credentialEvent = ListenToEvents.`as`(issuer).credentialEvents.lastOrNull {
                    it.data.thid == issuer.recall<String>("thid")
                }
                credentialEvent != null &&
                    credentialEvent!!.data.protocolState == IssueCredentialRecord.ProtocolState.requestReceived
            },
            "Issuer was unable to receive the credential request from Holder! Protocol state did not achieve RequestReceived state."
        )
        val recordId = credentialEvent!!.data.recordId
        issuer.attemptsTo(
            Post.to("/issue-credentials/records/$recordId/issue-credential")
        )
        issuer.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK)
        )

        wait(
            {
                credentialEvent = ListenToEvents.`as`(issuer).credentialEvents.lastOrNull {
                    it.data.thid == issuer.recall<String>("thid")
                }
                credentialEvent != null &&
                    credentialEvent!!.data.protocolState == IssueCredentialRecord.ProtocolState.credentialSent
            },
            "Issuer was unable to issue the credential! " +
                "Protocol state did not achieve ${IssueCredentialRecord.ProtocolState.credentialSent} state."
        )
    }

    @Then("{actor} receives the issued credential")
    fun bobHasTheCredentialIssued(holder: Actor) {
        wait(
            {
                credentialEvent = ListenToEvents.`as`(holder).credentialEvents.lastOrNull {
                    it.data.thid == holder.recall<String>("thid")
                }
                credentialEvent != null &&
                    credentialEvent!!.data.protocolState == IssueCredentialRecord.ProtocolState.credentialReceived
            },
            "Holder was unable to receive the credential from Issuer! " +
                "Protocol state did not achieve ${IssueCredentialRecord.ProtocolState.credentialReceived} state."
        )
        holder.remember("issuedCredential", ListenToEvents.`as`(holder).credentialEvents.last().data)
    }
}
