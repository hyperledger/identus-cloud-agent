package steps.oid4vci

import interactions.Post
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus
import org.hyperledger.identus.client.models.*

// TODO
// Agent does not support Holder capability.
// These steps should be updated when Agent can act as holder.
class IssueCredentialSteps {
    @When("{actor} creates an offer using {string} configuration with {string} form DID")
    fun issuerCreateCredentialOffer(issuer: Actor, configurationId: String, didForm: String) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        val claims = linkedMapOf(
            "name" to "Alice",
            "age" to 42,
        )
        val did: String = if (didForm == "short") {
            issuer.recall("shortFormDid")
        } else {
            issuer.recall("longFormDid")
        }
        issuer.attemptsTo(
            Post.to("/oid4vci/issuers/${credentialIssuer.id}/credential-offers")
                .with {
                    it.body(
                        CredentialOfferRequest(
                            credentialConfigurationId = configurationId,
                            issuingDID = did,
                            claims = claims
                        )
                    )
                },
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_CREATED),
        )
        val offer = SerenityRest.lastResponse().get<CredentialOfferResponse>().credentialOffer
        issuer.remember("oid4vciOffer", offer)
    }
}