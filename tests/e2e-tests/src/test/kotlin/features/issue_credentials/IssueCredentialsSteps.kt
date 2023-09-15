package features.issue_credentials

import api_models.*
import common.ListenToEvents
import common.Utils.lastResponseObject
import common.Utils.wait
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.Actor
import interactions.Post
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import org.apache.http.HttpStatus.SC_CREATED
import org.apache.http.HttpStatus.SC_OK

class IssueCredentialsSteps {

    var credentialEvent: CredentialEvent? = null

    @When("{actor} offers a credential to {actor} with {string} form DID")
    fun acmeOffersACredential(issuer: Actor, holder: Actor, didForm: String) {

        val did: String = if (didForm == "short")
            issuer.recall("shortFormDid") else issuer.recall("longFormDid")

        val newCredential = Credential(
            schemaId = null,
            validityPeriod = 3600.0,
            automaticIssuance = false,
            awaitConfirmation = false,
            claims = linkedMapOf(
                "firstName" to "FirstName",
                "lastName" to "LastName",
            ),
            issuingDID = did,
            connectionId = issuer.recall<Connection>("connection-with-${holder.name}").connectionId,
        )
        issuer.attemptsTo(
            Post.to("/issue-credentials/credential-offers")
                .with {
                    it.body(newCredential)
                },
        )
        issuer.should(
            ResponseConsequence.seeThatResponse {
                it.statusCode(SC_CREATED)
            },
        )
        issuer.remember("thid", lastResponseObject("", Credential::class).thid)
        holder.remember("thid", lastResponseObject("", Credential::class).thid)
    }

    @When("{actor} receives the credential offer and accepts")
    fun bobRequestsTheCredential(holder: Actor) {
        wait(
            {
                credentialEvent = ListenToEvents.`as`(holder).credentialEvents.lastOrNull {
                    it.data.thid == holder.recall<String>("thid")
                }
                credentialEvent != null &&
                        credentialEvent!!.data.protocolState == CredentialState.OFFER_RECEIVED
            },
            "Holder was unable to receive the credential offer from Issuer! Protocol state did not achieve OfferReceived state.",
        )

        val recordId = ListenToEvents.`as`(holder).credentialEvents.last().data.recordId
        holder.remember("recordId", recordId)

        holder.attemptsTo(
            Post.to("/issue-credentials/records/$recordId/accept-offer")
                .with {
                    it.body("""
                        { "subjectId": "${holder.recall<String>("longFormDid")}" }
                    """.trimIndent())
                },
        )
        holder.should(
            ResponseConsequence.seeThatResponse("Accept offer") {
                it.statusCode(SC_OK)
            },
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
                        credentialEvent!!.data.protocolState == CredentialState.REQUEST_RECEIVED
            },
            "Issuer was unable to receive the credential request from Holder! Protocol state did not achieve RequestReceived state.",
        )
        val recordId = credentialEvent!!.data.recordId
        issuer.attemptsTo(
            Post.to("/issue-credentials/records/$recordId/issue-credential"),
        )
        issuer.should(
            ResponseConsequence.seeThatResponse("Issue credential") {
                it.statusCode(SC_OK)
            },
        )

        wait(
            {
                credentialEvent = ListenToEvents.`as`(issuer).credentialEvents.lastOrNull {
                    it.data.thid == issuer.recall<String>("thid")
                }
                credentialEvent != null &&
                        credentialEvent!!.data.protocolState == CredentialState.CREDENTIAL_SENT
            },
            "Issuer was unable to issue the credential! " +
                    "Protocol state did not achieve ${CredentialState.CREDENTIAL_SENT} state.",
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
                        credentialEvent!!.data.protocolState == CredentialState.CREDENTIAL_RECEIVED
            },
            "Holder was unable to receive the credential from Issuer! " +
                    "Protocol state did not achieve ${CredentialState.CREDENTIAL_RECEIVED} state.",
        )
        holder.remember("issuedCredential", ListenToEvents.`as`(holder).credentialEvents.last().data)
    }
}
