package features.issue_credentials

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.rest.interactions.Get
import net.serenitybdd.screenplay.rest.interactions.Post
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import api_models.Connection
import api_models.Credential
import common.Utils.lastResponse
import common.Utils.wait
import extentions.Agents.agents

class IssueCredentialsSteps {

    @Given("{string} and {string} have an existing connection")
    fun acmeAndBobHaveAnExistingConnection(issuer: String, holder: String) {
        // Acme(Issuer) initiates a connection
        // and sends it to Bob(Holder) out-of-band, e.g. using QR-code
        agents[issuer]!!.attemptsTo(
            Post.to("/connections")
                .with {
                    it.header("Content-Type", "application/json")
                    it.body("""{"label": "Connect with ${agents[holder]!!.name}"}""")
                }
        )
        agents[issuer]!!.should(
            ResponseConsequence.seeThatResponse("Creates connection request") {
                it.statusCode(201)
            }
        )
        val acmeConnection = lastResponse().getObject("", Connection::class.java)

        // Here out of band transfer of connection QR code is happening
        // and Bob (Holder) gets an invitation URL
        // they're accepting connection invitation by POST request specifying achieved invitation
        agents[holder]!!.remember("invitationUrl", acmeConnection.invitation.invitationUrl.split("=")[1])

        // Bob accepts connection using achieved out-of-band invitation
        agents[holder]!!.attemptsTo(
            Post.to("/connection-invitations")
                .with {
                    it.header("Content-Type", "application/json")
                    it.body("""{"invitation": "${agents[holder]!!.recall<String>("invitationUrl")}"}""")
                }
        )
        agents[holder]!!.should(
            ResponseConsequence.seeThatResponse("Accepts connection request") {
                it.statusCode(200)
            }
        )
        val bobConnection = lastResponse().getObject("", Connection::class.java)

        // Acme(Issuer) checks their connections to check if invitation was accepted by Bob(Holder)
        // and sends final connection response
        wait(
            {
                agents[issuer]!!.attemptsTo(
                    Get.resource("/connections/${acmeConnection.connectionId}"),
                )
                agents[issuer]!!.should(
                    ResponseConsequence.seeThatResponse("Get issuer connections") {
                        it.statusCode(200)
                    }
                )
                lastResponse()
                    .getObject("", Connection::class.java).state == "ConnectionResponseSent"
            },
            "Issuer did not sent final connection confirmation! Connection didn't reach ConnectionResponseSent state."
        )
        agents[issuer]!!.remember("did", lastResponse().getObject("", Connection::class.java).myDid)
        agents[issuer]!!.remember("holderDid", lastResponse().getObject("", Connection::class.java).theirDid)

        // Bob (Holder) receives final connection response
        wait(
            {
                agents[holder]!!.attemptsTo(
                    Get.resource("/connections/${bobConnection.connectionId}")
                )
                agents[holder]!!.should(
                    ResponseConsequence.seeThatResponse("Get holder connections") {
                        it.statusCode(200)
                    }
                )
                lastResponse().getObject("", Connection::class.java).state == "ConnectionResponseReceived"
            },
            "Holder did not receive final connection confirmation! Connection didn't reach ConnectionResponseReceived state."
        )

        agents[holder]!!.remember("did", lastResponse().getObject("", Connection::class.java).myDid)
        agents[holder]!!.remember("issuerDid", lastResponse().getObject("", Connection::class.java).theirDid)
        // Connection established. Both parties exchanged their DIDs with each other
    }

    @Given("{string} offers a credential")
    fun acmeOffersACredential(issuer: String) {
        val newCredential = Credential(
            schemaId = "schema:1234",
            subjectId = agents[issuer]!!.recall("holderDid"),
            validityPeriod = 3600,
            automaticIssuance = false,
            awaitConfirmation = false,
            claims = linkedMapOf(
                "firstName" to "FirstName",
                "lastName" to "LastName"
            )
        )

        agents[issuer]!!.attemptsTo(
            Post.to("/issue-credentials/credential-offers")
                .with {
                    it.header("Content-Type", "application/json")
                    it.body(newCredential)
                }
        )
        agents[issuer]!!.should(
            ResponseConsequence.seeThatResponse("The issue credential offer created") {
                it.statusCode(201)
            }
        )

        // TODO: add check that newCredential object corresponds to the output of restapi call here
    }

    @When("{string} requests the credential")
    fun bobRequestsTheCredential(holder: String) {
        wait(
            {
                agents[holder]!!.attemptsTo(
                    Get.resource("/issue-credentials/records")
                )
                agents[holder]!!.should(
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
        agents[holder]!!.remember("recordId", recordId)

        agents[holder]!!.attemptsTo(
            Post.to("/issue-credentials/records/${recordId}/accept-offer")
        )
        agents[holder]!!.should(
            ResponseConsequence.seeThatResponse("Accept offer") {
                it.statusCode(200)
            }
        )
    }

    @When("{string} issues the credential")
    fun acmeIssuesTheCredential(issuer: String) {
        wait(
            {
                agents[issuer]!!.attemptsTo(
                    Get.resource("/issue-credentials/records")
                )
                agents[issuer]!!.should(
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
        agents[issuer]!!.attemptsTo(
            Post.to("/issue-credentials/records/${recordId}/issue-credential")
        )
        agents[issuer]!!.should(
            ResponseConsequence.seeThatResponse("Issue credential") {
                it.statusCode(200)
            }
        )

        wait(
            {
                agents[issuer]!!.attemptsTo(
                    Get.resource("/issue-credentials/records/${recordId}")
                )
                agents[issuer]!!.should(
                    ResponseConsequence.seeThatResponse("Credential records") {
                        it.statusCode(200)
                    }
                )
                lastResponse().getObject("", Credential::class.java).protocolState == "CredentialSent"
            },
            "Issuer was unable to issue the credential! Protocol state did not achieve CredentialSent state."
        )
    }

    @Then("{string} has the credential issued")
    fun bobHasTheCredentialIssued(holder: String) {
        wait(
            {
                agents[holder]!!.attemptsTo(
                    Get.resource("/issue-credentials/records/${agents[holder]!!.recall<String>("recordId")}")
                )
                agents[holder]!!.should(
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
