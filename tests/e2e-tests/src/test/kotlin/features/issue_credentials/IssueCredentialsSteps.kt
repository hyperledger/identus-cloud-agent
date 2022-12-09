package features.issue_credentials

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.rest.interactions.Get
import net.serenitybdd.screenplay.rest.interactions.Post
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import api_models.Connection
import api_models.Credential
import common.Agents.Acme
import common.Agents.Bob
import common.Utils.lastResponse
import common.Utils.wait
import features.connection.ConnectionSteps

class IssueCredentialsSteps {

    @Given("Acme and Bob have an existing connection")
    fun acmeAndBobHaveAnExistingConnection() {
        val connectionSteps = ConnectionSteps()
        connectionSteps.inviterGeneratesAConnectionInvitation()
        connectionSteps.inviteeReceivesTheConnectionInvitation()
        connectionSteps.inviteeSendsAConnectionRequestToInviter()
        connectionSteps.inviterReceivesTheConnectionRequest()
        connectionSteps.inviterSendsAConnectionResponseToInvitee()
        connectionSteps.inviteeReceivesTheConnectionResponse()
        connectionSteps.inviterAndInviteeHaveAConnection()
    }

    @Given("Acme offers a credential")
    fun acmeOffersACredential() {
        val newCredential = Credential(
            schemaId = "schema:1234",
            subjectId = Acme.recall<Connection>("connection").theirDid,
            validityPeriod = 3600,
            automaticIssuance = false,
            awaitConfirmation = false,
            claims = linkedMapOf(
                "firstName" to "FirstName",
                "lastName" to "LastName"
            )
        )

        Acme.attemptsTo(
            Post.to("/issue-credentials/credential-offers")
                .with {
                    it.header("Content-Type", "application/json")
                    it.body(newCredential)
                }
        )
        Acme.should(
            ResponseConsequence.seeThatResponse("The issue credential offer created") {
                it.statusCode(201)
            }
        )

        // TODO: add check that newCredential object corresponds to the output of restapi call here
    }

    @When("Bob requests the credential")
    fun bobRequestsTheCredential() {
        wait(
            {
                Bob.attemptsTo(
                    Get.resource("/issue-credentials/records")
                )
                Bob.should(
                    ResponseConsequence.seeThatResponse("Credential records") {
                        it.statusCode(200)
                    }
                )
                lastResponse().getList("items", Credential::class.java).findLast { it.protocolState == "OfferReceived" } != null
            },
            "Holder was unable to receive the credential offer from Issuer! Protocol state did not achieve OfferReceived state."
        )

        val recordId = lastResponse().getList("items", Credential::class.java)
            .findLast { it.protocolState == "OfferReceived" }!!.recordId
        Bob.remember("recordId", recordId)

        Bob.attemptsTo(
            Post.to("/issue-credentials/records/${recordId}/accept-offer")
        )
        Bob.should(
            ResponseConsequence.seeThatResponse("Accept offer") {
                it.statusCode(200)
            }
        )
    }

    @When("Acme issues the credential")
    fun acmeIssuesTheCredential() {
        wait(
            {
                Acme.attemptsTo(
                    Get.resource("/issue-credentials/records")
                )
                Acme.should(
                    ResponseConsequence.seeThatResponse("Credential records") {
                        it.statusCode(200)
                    }
                )
                lastResponse().getList("items", Credential::class.java)
                    .findLast { it.protocolState == "RequestReceived" } != null
            },
            "Issuer was unable to receive the credential request from Holder! Protocol state did not achieve RequestReceived state."
        )
        val recordId = lastResponse().getList("items", Credential::class.java)
            .findLast { it.protocolState == "RequestReceived" }!!.recordId
        Acme.attemptsTo(
            Post.to("/issue-credentials/records/${recordId}/issue-credential")
        )
        Acme.should(
            ResponseConsequence.seeThatResponse("Issue credential") {
                it.statusCode(200)
            }
        )

        wait(
            {
                Acme.attemptsTo(
                    Get.resource("/issue-credentials/records/${recordId}")
                )
                Acme.should(
                    ResponseConsequence.seeThatResponse("Credential records") {
                        it.statusCode(200)
                    }
                )
                lastResponse().getObject("", Credential::class.java).protocolState == "CredentialSent"
            },
            "Issuer was unable to issue the credential! Protocol state did not achieve CredentialSent state."
        )
    }

    @Then("Bob has the credential issued")
    fun bobHasTheCredentialIssued() {
        wait(
            {
                Bob.attemptsTo(
                    Get.resource("/issue-credentials/records/${Bob.recall<String>("recordId")}")
                )
                Bob.should(
                    ResponseConsequence.seeThatResponse("Credential records") {
                        it.statusCode(200)
                    }
                )
                lastResponse().getObject("", Credential::class.java).protocolState == "CredentialReceived"
            },
            "Holder was unable to receive the credential from Issuer! Protocol state did not achieve CredentialReceived state."
        )
        val achievedCredential = lastResponse().getObject("", Credential::class.java)
        println(achievedCredential)
    }
}
