package features.issue_credentials

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.rest.interactions.Get
import net.serenitybdd.screenplay.rest.interactions.Post
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import api_models.Connection
import api_models.Credential
import common.Utils.lastResponseList
import common.Utils.lastResponseObject
import common.Utils.wait
import net.serenitybdd.screenplay.Actor

class IssueCredentialsSteps {
    @Given("{actor} offers a credential to {actor}")
    fun acmeOffersACredential(issuer: Actor, holder: Actor) {
        val newCredential = Credential(
            schemaId = "schema:1234",
            subjectId = issuer.recall<Connection>("connection-with-${holder.name}").theirDid,
            validityPeriod = 3600,
            automaticIssuance = false,
            awaitConfirmation = false,
            claims = linkedMapOf(
                "firstName" to "FirstName",
                "lastName" to "LastName"
            )
        )

        issuer.attemptsTo(
            Post.to("/issue-credentials/credential-offers")
                .with {
                    it.body(newCredential)
                }
        )
        issuer.should(
            ResponseConsequence.seeThatResponse("The issue credential offer created") {
                it.statusCode(201)
            }
        )

        // TODO: add check that newCredential object corresponds to the output of restapi call here
    }

    @When("{actor} requests the credential")
    fun bobRequestsTheCredential(holder: Actor) {
        wait(
            {
                holder.attemptsTo(
                    Get.resource("/issue-credentials/records")
                )
                holder.should(
                    ResponseConsequence.seeThatResponse("Credential records") {
                        it.statusCode(200)
                    }
                )
                lastResponseList("items", Credential::class).findLast { it.protocolState == "OfferReceived" } != null
            },
            "Holder was unable to receive the credential offer from Issuer! Protocol state did not achieve OfferReceived state."
        )

        val recordId = lastResponseList("items", Credential::class)
            .findLast { it.protocolState == "OfferReceived" }!!.recordId
        holder.remember("recordId", recordId)

        holder.attemptsTo(
            Post.to("/issue-credentials/records/${recordId}/accept-offer")
        )
        holder.should(
            ResponseConsequence.seeThatResponse("Accept offer") {
                it.statusCode(200)
            }
        )
    }

    @When("{actor} issues the credential")
    fun acmeIssuesTheCredential(issuer: Actor) {
        wait(
            {
                issuer.attemptsTo(
                    Get.resource("/issue-credentials/records")
                )
                issuer.should(
                    ResponseConsequence.seeThatResponse("Credential records") {
                        it.statusCode(200)
                    }
                )
                lastResponseList("items", Credential::class)
                    .findLast { it.protocolState == "RequestReceived" } != null
            },
            "Issuer was unable to receive the credential request from Holder! Protocol state did not achieve RequestReceived state."
        )
        val recordId = lastResponseList("items", Credential::class)
            .findLast { it.protocolState == "RequestReceived" }!!.recordId
        issuer.attemptsTo(
            Post.to("/issue-credentials/records/${recordId}/issue-credential")
        )
        issuer.should(
            ResponseConsequence.seeThatResponse("Issue credential") {
                it.statusCode(200)
            }
        )

        wait(
            {
                issuer.attemptsTo(
                    Get.resource("/issue-credentials/records/${recordId}")
                )
                issuer.should(
                    ResponseConsequence.seeThatResponse("Credential records") {
                        it.statusCode(200)
                    }
                )
                lastResponseObject("", Credential::class).protocolState == "CredentialSent"
            },
            "Issuer was unable to issue the credential! Protocol state did not achieve CredentialSent state."
        )
    }

    @Then("{actor} has the credential issued")
    fun bobHasTheCredentialIssued(holder: Actor) {
        wait(
            {
                holder.attemptsTo(
                    Get.resource("/issue-credentials/records/${holder.recall<String>("recordId")}")
                )
                holder.should(
                    ResponseConsequence.seeThatResponse("Credential records") {
                        it.statusCode(200)
                    }
                )
                lastResponseObject("", Credential::class).protocolState == "CredentialReceived"
            },
            "Holder was unable to receive the credential from Issuer! Protocol state did not achieve CredentialReceived state."
        )
        holder.remember("issuedCredential", lastResponseObject("", Credential::class))
    }
}
