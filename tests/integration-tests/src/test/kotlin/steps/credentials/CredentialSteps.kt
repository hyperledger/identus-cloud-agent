package steps.credentials

import abilities.ListenToEvents
import interactions.Post
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.iohk.atala.automation.serenity.ensure.Ensure
import io.iohk.atala.automation.serenity.interactions.PollingWait
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus.*
import org.hamcrest.CoreMatchers.equalTo
import org.hyperledger.identus.client.models.*
import org.hyperledger.identus.client.models.IssueCredentialRecord.ProtocolState.*

class CredentialSteps {

    @When("{actor} receives the credential offer")
    fun holderReceivesCredentialOffer(holder: Actor) {
        holder.attemptsTo(
            PollingWait.until(
                ListenToEvents.credentialState(holder),
                equalTo(OFFER_RECEIVED),
            ),
        )
        val recordId = ListenToEvents.with(holder).credentialEvents.last().data.recordId
        holder.remember("recordId", recordId)
    }

    @When("{actor} tries to issue the credential")
    fun issuerTriesToIssueTheCredential(issuer: Actor) {
        issuer.attemptsTo(
            PollingWait.until(
                ListenToEvents.credentialState(issuer),
                equalTo(REQUEST_RECEIVED),
            ),
        )

        val recordId = ListenToEvents.with(issuer).credentialEvents.last().data.recordId

        issuer.attemptsTo(
            Post.to("/issue-credentials/records/$recordId/issue-credential"),
        )
    }

    @When("{actor} issues the credential")
    fun issuerIssuesTheCredential(issuer: Actor) {
        issuerTriesToIssueTheCredential(issuer)

        issuer.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
        )

        issuer.attemptsTo(
            PollingWait.until(
                ListenToEvents.credentialState(issuer),
                equalTo(CREDENTIAL_SENT),
            ),
        )
        issuer.remember("issuedCredential", ListenToEvents.with(issuer).credentialEvents.last().data)
    }

    @Then("{actor} receives the issued credential")
    fun holderReceivesTheIssuedCredential(holder: Actor) {
        holder.attemptsTo(
            PollingWait.until(
                ListenToEvents.credentialState(holder),
                equalTo(CREDENTIAL_RECEIVED),
            ),
        )
        holder.remember("issuedCredential", ListenToEvents.with(holder).credentialEvents.last().data)
    }

    @Then("{actor} should see that credential issuance has failed")
    fun issuerShouldSeeThatCredentialIssuanceHasFailed(issuer: Actor) {
        issuer.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_UNPROCESSABLE_ENTITY),
        )
    }
}
