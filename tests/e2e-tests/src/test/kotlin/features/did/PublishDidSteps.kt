package features.did

import api_models.*
import common.Utils.lastResponseList
import common.Utils.lastResponseObject
import common.Utils.wait
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.interactions.Get
import net.serenitybdd.screenplay.rest.interactions.Post
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import org.apache.http.HttpStatus.*
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.*
import java.time.Duration

class PublishDidSteps {

    @Given("{actor} creates unpublished DID")
    fun acmeCreatesUnpublishedDid(acme: Actor) {
        val publicKeys = listOf(PublicKey("key1", Purpose.AUTHENTICATION))
        val services = listOf(Service("https://foo.bar.com", "LinkedDomains", listOf("https://foo.bar.com")))
        val documentTemplate = DocumentTemplate(publicKeys, services)
        acme.attemptsTo(
            Post.to("/did-registrar/dids")
                .with {
                    it.body(CreateManagedDidRequest(documentTemplate))
                }
        )
        acme.should(
            ResponseConsequence.seeThatResponse {
                it.statusCode(SC_OK)
                it.body("longFormDid", not(emptyString()))
            }
        )
        val longFormDid = lastResponseObject("longFormDid", String::class)

        acme.attemptsTo(
            Get.resource("/did-registrar/dids")
        )
        acme.should(
            ResponseConsequence.seeThatResponse {
                it.statusCode(SC_OK)
            }
        )
        acme.remember(
            "shortFormDid",
            lastResponseList("", ManagedDid::class).find {
                it.longFormDid == longFormDid
            }!!.did
        )
    }

    @When("{actor} publishes DID to ledger")
    fun hePublishesDidToLedger(acme: Actor) {
        acme.attemptsTo(
            Post.to("/did-registrar/dids/${acme.recall<String>("shortFormDid")}/publications")
        )
        acme.should(
            ResponseConsequence.seeThatResponse {
                it.statusCode(SC_ACCEPTED)
                it.body("scheduledOperation.didRef", not(emptyString()))
                it.body("scheduledOperation.id", not(emptyString()))
            }
        )
    }

    @Then("{actor} sees DID successfully published to ledger")
    fun heSeesDidSuccessfullyPublishedToLedger(acme: Actor) {
        wait(
            {
                acme.attemptsTo(
                    Get.resource("/dids/${acme.recall<String>("shortFormDid")}")
                )
                SerenityRest.lastResponse().statusCode == SC_OK
            },
            "ERROR: DID was not published to ledger!",
            timeout = Duration.ofSeconds(600L)
        )
        acme.should(
            ResponseConsequence.seeThatResponse {
                it.statusCode(SC_OK)
                it.body("did.id", equalTo(acme.recall<String>("shortFormDid")))
            }
        )
    }

    @Then("{actor} resolves DID document corresponds to W3C standard")
    fun heSeesDidDocumentCorrespondsToW3cStandard(acme: Actor) {
        val didDocument = lastResponseObject("", DidDocument::class).did!!
        assertThat(didDocument)
            .hasFieldOrProperty("assertionMethod")
            .hasFieldOrProperty("authentication")
            .hasFieldOrProperty("capabilityInvocation")
            .hasFieldOrProperty("controller")
            .hasFieldOrProperty("id")
            .hasFieldOrProperty("keyAgreement")
            .hasFieldOrProperty("service")
            .hasFieldOrProperty("verificationMethod")

        assertThat(didDocument.id == acme.recall<String>("shortFormDid"))

        assertThat(didDocument.authentication!![0].publicKeyJwk)
            .hasNoNullFieldsOrProperties()

        assertThat(lastResponseObject("", DidDocument::class).metadata!!)
            .hasFieldOrPropertyWithValue("deactivated", "false")
    }
}
