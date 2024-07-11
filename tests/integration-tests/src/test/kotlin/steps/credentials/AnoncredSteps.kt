package steps.credentials

import interactions.Post
import interactions.body
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus.SC_CREATED
import org.apache.http.HttpStatus.SC_OK
import org.hyperledger.identus.client.models.*

class AnoncredSteps {

    @When("{actor} accepts anoncred credential offer")
    fun holderAcceptsCredentialOfferForAnoncred(holder: Actor) {
        val recordId = holder.recall<String>("recordId")
        holder.attemptsTo(
            Post.to("/issue-credentials/records/$recordId/accept-offer").body("{}"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
        )
    }

    @When("{actor} offers anoncred to {actor}")
    fun acmeOffersAnoncredToBob(issuer: Actor, holder: Actor) {
        val credentialOfferRequest = CreateIssueCredentialRecordRequest(
            credentialDefinitionId = issuer.recall<CredentialDefinitionResponse>("anoncredsCredentialDefinition").guid,
            claims = linkedMapOf(
                "name" to "Bob",
                "age" to "21",
                "sex" to "M",
            ),
            issuingDID = issuer.recall("shortFormDid"),
            connectionId = issuer.recall<Connection>("connection-with-${holder.name}").connectionId,
            validityPeriod = 3600.0,
            credentialFormat = "AnonCreds",
            automaticIssuance = false,
        )

        issuer.attemptsTo(
            Post.to("/issue-credentials/credential-offers").body(credentialOfferRequest),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
        )

        val credentialRecord = SerenityRest.lastResponse().get<IssueCredentialRecord>()
        issuer.remember("thid", credentialRecord.thid)
        holder.remember("thid", credentialRecord.thid)
    }
}
