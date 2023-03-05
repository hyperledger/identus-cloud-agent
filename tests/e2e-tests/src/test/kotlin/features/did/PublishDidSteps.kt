package features.did

import api_models.*
import common.TestConstants
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

class PublishDidSteps {

    @Given("{actor} have published PRISM DID")
    fun actorHavePublishedPrismDid(actor: Actor) {
        actor.attemptsTo(
            Get.resource("/did-registrar/dids"),
        )
        actor.should(
            ResponseConsequence.seeThatResponse {
                it.statusCode(SC_OK)
            },
        )
        val publishedDids = lastResponseList("contents", ManagedDid::class).filter {
            it.status == ManagedDidStatuses.PUBLISHED
        }
        val did = publishedDids.firstOrNull {
            actor.attemptsTo(
                Get.resource("/dids/${it.did}"),
            )
            lastResponseObject("metadata.deactivated", String::class) == "false"
        }
        if (did == null) {
            createsUnpublishedDid(actor)
            hePublishesDidToLedger(actor)
        } else {
            actor.remember("shortFormDid", did.did)
        }
    }

    @Given("{actor} creates unpublished DID")
    fun createsUnpublishedDid(actor: Actor) {
        val publicKeys = listOf(
            TestConstants.PRISM_DID_AUTH_KEY,
            TestConstants.PRISM_DID_ASSERTION_KEY,
        )
        val services = listOf(
            TestConstants.PRISM_DID_SERVICE,
            TestConstants.PRISM_DID_SERVICE_FOR_UPDATE,
            TestConstants.PRISM_DID_SERVICE_TO_REMOVE,
        )
        val documentTemplate = DocumentTemplate(publicKeys, services)
        actor.attemptsTo(
            Post.to("/did-registrar/dids")
                .with {
                    it.body(CreatePrismDidRequest(documentTemplate))
                },
        )
        actor.should(
            ResponseConsequence.seeThatResponse {
                it.statusCode(SC_OK)
                it.body("longFormDid", not(emptyString()))
            },
        )
        val longFormDid = lastResponseObject("longFormDid", String::class)
        actor.remember("longFormDid", longFormDid)

        actor.attemptsTo(
            Get.resource("/did-registrar/dids/$longFormDid"),
        )
        actor.should(
            ResponseConsequence.seeThatResponse {
                it.statusCode(SC_OK)
            },
        )
        actor.remember(
            "shortFormDid",
            lastResponseObject("", ManagedDid::class).did,
        )
    }

    @When("{actor} publishes DID to ledger")
    fun hePublishesDidToLedger(actor: Actor) {
        actor.attemptsTo(
            Post.to("/did-registrar/dids/${actor.recall<String>("shortFormDid")}/publications"),
        )
        actor.should(
            ResponseConsequence.seeThatResponse {
                it.statusCode(SC_ACCEPTED)
                it.body("scheduledOperation.didRef", not(emptyString()))
                it.body("scheduledOperation.id", not(emptyString()))
            },
        )
        wait(
            {
                actor.attemptsTo(
                    Get.resource("/dids/${actor.recall<String>("shortFormDid")}"),
                )
                SerenityRest.lastResponse().statusCode == SC_OK
            },
            "ERROR: DID was not published to ledger!",
            timeout = TestConstants.DID_UPDATE_PUBLISH_MAX_WAIT_5_MIN,
        )
        actor.should(
            ResponseConsequence.seeThatResponse {
                it.statusCode(SC_OK)
                it.body("did.id", equalTo(actor.recall<String>("shortFormDid")))
            },
        )
    }

    @Then("{actor} resolves DID document corresponds to W3C standard")
    fun heSeesDidDocumentCorrespondsToW3cStandard(actor: Actor) {
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

        val shortFormDid = actor.recall<String>("shortFormDid")

        assertThat(didDocument.id == shortFormDid)

        assertThat(didDocument.authentication!![0])
            .hasFieldOrPropertyWithValue("type", "REFERENCED")
            .hasFieldOrPropertyWithValue("uri", "$shortFormDid#${TestConstants.PRISM_DID_AUTH_KEY.id}")

        assertThat(didDocument.verificationMethod!![0])
            .hasFieldOrPropertyWithValue("controller", shortFormDid)
            .hasFieldOrPropertyWithValue("id", "$shortFormDid#${TestConstants.PRISM_DID_ASSERTION_KEY.id}")
            .hasFieldOrProperty("publicKeyJwk")

        assertThat(lastResponseObject("", DidDocument::class).metadata!!)
            .hasFieldOrPropertyWithValue("deactivated", false)
            .hasFieldOrProperty("canonicalId")
    }
}
