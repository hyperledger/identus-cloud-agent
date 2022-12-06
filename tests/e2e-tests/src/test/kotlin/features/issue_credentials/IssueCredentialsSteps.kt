package features.issue_credentials

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.rest.interactions.Get
import net.serenitybdd.screenplay.rest.interactions.Post
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import api_models.Connection
import api_models.Credential
import extentions.WithAgents

class IssueCredentialsSteps : WithAgents() {

    @Given("Acme and Bob have an existing connection")
    fun acmeAndBobHaveAnExistingConnection() {
        // Acme(Issuer) initiates a connection
        // and sends it to Bob(Holder) out-of-band, e.g. using QR-code
        acme.attemptsTo(
            Post.to("/connections")
                .with {
                    it.header("Content-Type", "application/json")
                    it.body("""{"label": "Connect with ${bob.name}"}""")
                }
        )
        acme.should(
            ResponseConsequence.seeThatResponse("Creates connection request") {
                it.statusCode(201)
            }
        )
        val acmeConnection = lastResponse().getObject("", Connection::class.java)

        // Here out of band transfer of connection QR code is happening
        // and Bob (Holder) gets an invitation URL
        // they're accepting connection invitation by POST request specifying achieved invitation
        bob.remember("invitationUrl", acmeConnection.invitation.invitationUrl.split("=")[1])

        // Bob accepts connection using achieved out-of-band invitation
        bob.attemptsTo(
            Post.to("/connection-invitations")
                .with {
                    it.header("Content-Type", "application/json")
                    it.body("""{"invitation": "${bob.recall<String>("invitationUrl")}"}""")
                }
        )
        bob.should(
            ResponseConsequence.seeThatResponse("Accepts connection request") {
                it.statusCode(200)
            }
        )
        val bobConnection = lastResponse().getObject("", Connection::class.java)

        // Acme(Issuer) checks their connections to check if invitation was accepted by Bob(Holder)
        // and sends final connection response
        wait(
            {
                acme.attemptsTo(
                    Get.resource("/connections/${acmeConnection.connectionId}"),
                )
                acme.should(
                    ResponseConsequence.seeThatResponse("Get issuer connections") {
                        it.statusCode(200)
                    }
                )
                lastResponse()
                    .getObject("", Connection::class.java).state == "ConnectionResponseSent"
            },
            "Issuer did not sent final connection confirmation! Connection didn't reach ConnectionResponseSent state."
        )
        acme.remember("did", lastResponse().getObject("", Connection::class.java).myDid)
        acme.remember("holderDid", lastResponse().getObject("", Connection::class.java).theirDid)

        // Bob (Holder) receives final connection response
        wait(
            {
                bob.attemptsTo(
                    Get.resource("/connections/${bobConnection.connectionId}")
                )
                bob.should(
                    ResponseConsequence.seeThatResponse("Get holder connections") {
                        it.statusCode(200)
                    }
                )
                lastResponse().getObject("", Connection::class.java).state == "ConnectionResponseReceived"
            },
            "Holder did not receive final connection confirmation! Connection didn't reach ConnectionResponseReceived state."
        )

        bob.remember("did", lastResponse().getObject("", Connection::class.java).myDid)
        bob.remember("issuerDid", lastResponse().getObject("", Connection::class.java).theirDid)
        // Connection established. Both parties exchanged their DIDs with each other
    }

    @Given("Acme offers a credential")
    fun acmeOffersACredential() {
        val newCredential = Credential(
            schemaId = "schema:1234",
            subjectId = acme.recall("holderDid"),
            validityPeriod = 3600,
            automaticIssuance = false,
            awaitConfirmation = false,
            claims = linkedMapOf(
                "firstName" to "FirstName",
                "lastName" to "LastName"
            )
        )

        acme.attemptsTo(
            Post.to("/issue-credentials/credential-offers")
                .with {
                    it.header("Content-Type", "application/json")
                    it.body(newCredential)
                }
        )
        acme.should(
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
                bob.attemptsTo(
                    Get.resource("/issue-credentials/records")
                )
                bob.should(
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
        bob.remember("recordId", recordId)

        bob.attemptsTo(
            Post.to("/issue-credentials/records/${recordId}/accept-offer")
        )
        bob.should(
            ResponseConsequence.seeThatResponse("Accept offer") {
                it.statusCode(200)
            }
        )
    }

    @When("Acme issues the credential")
    fun acmeIssuesTheCredential() {
        wait(
            {
                acme.attemptsTo(
                    Get.resource("/issue-credentials/records")
                )
                acme.should(
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
        acme.attemptsTo(
            Post.to("/issue-credentials/records/${recordId}/issue-credential")
        )
        acme.should(
            ResponseConsequence.seeThatResponse("Issue credential") {
                it.statusCode(200)
            }
        )

        wait(
            {
                acme.attemptsTo(
                    Get.resource("/issue-credentials/records/${recordId}")
                )
                acme.should(
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
                bob.attemptsTo(
                    Get.resource("/issue-credentials/records/${bob.recall<String>("recordId")}")
                )
                bob.should(
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
