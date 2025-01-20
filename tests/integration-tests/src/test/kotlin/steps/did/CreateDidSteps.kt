package steps.did

import abilities.ListenToEvents
import common.DidDocumentTemplate
import common.DidType
import common.DidType.CUSTOM
import interactions.Get
import interactions.Post
import interactions.body
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import io.iohk.atala.automation.serenity.interactions.PollingWait
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus
import org.apache.http.HttpStatus.SC_CREATED
import org.apache.http.HttpStatus.SC_OK
import org.hamcrest.CoreMatchers.equalTo
import org.hyperledger.identus.client.models.CreateManagedDidRequest
import org.hyperledger.identus.client.models.CreateManagedDidRequestDocumentTemplate
import org.hyperledger.identus.client.models.Curve
import org.hyperledger.identus.client.models.DIDOperationResponse
import org.hyperledger.identus.client.models.DIDResolutionResult
import org.hyperledger.identus.client.models.ManagedDID
import org.hyperledger.identus.client.models.ManagedDIDKeyTemplate
import org.hyperledger.identus.client.models.Purpose

class CreateDidSteps {

    @Given("{actor} has a published DID for '{}'")
    fun agentHasAPublishedDID(agent: Actor, didType: DidType) {
        if (agent.recallAll().containsKey("hasPublishedDid") && actualDidHasSamePurpose(agent, didType)) {
            return
        }
        agentHasAnUnpublishedDID(agent, didType)
        hePublishesDidToLedger(agent)
    }

    @Given("{actor} has an unpublished DID for '{}'")
    fun agentHasAnUnpublishedDID(agent: Actor, didType: DidType) {
        if (agent.recallAll().containsKey("shortFormDid") || agent.recallAll().containsKey("longFormDid")) {
            // is not published and has the same purpose
            if (!agent.recallAll().containsKey("hasPublishedDid") && actualDidHasSamePurpose(agent, didType)) {
                return
            }
        }
        agentCreatesUnpublishedDid(agent, didType)
    }

    @Given("{actor} creates empty unpublished DID")
    fun agentCreatesEmptyUnpublishedDid(actor: Actor) {
        agentCreatesUnpublishedDid(actor, CUSTOM)
    }

    @Given("{actor} creates unpublished DID for '{}'")
    fun agentCreatesUnpublishedDid(actor: Actor, didType: DidType) {
        createDid(actor, didType, didType.documentTemplate)
    }

    @When("{actor} prepares a custom PRISM DID")
    fun actorPreparesCustomDid(actor: Actor) {
        val customDid = CUSTOM.documentTemplate
        actor.remember("customDid", customDid)
    }

    @When("{actor} adds a '{curve}' key for '{purpose}' purpose with '{}' name to the custom PRISM DID")
    fun actorAddsKeyToCustomDid(actor: Actor, curve: Curve, purpose: Purpose, name: String) {
        val documentTemplate = actor.recall<DidDocumentTemplate>("customDid")
        documentTemplate.publicKeys.add(ManagedDIDKeyTemplate(name, purpose, curve))
        actor.remember("customDid", documentTemplate)
    }

    @When("{actor} creates the custom PRISM DID")
    fun actorCreatesTheCustomPrismDid(actor: Actor) {
        val documentTemplate = actor.recall<DidDocumentTemplate>("customDid")
        createDid(actor, CUSTOM, documentTemplate)
    }

    @When("{actor} publishes DID to ledger")
    fun hePublishesDidToLedger(actor: Actor) {
        val shortFormDid = actor.recall<String>("shortFormDid")
        actor.attemptsTo(
            Post.to("/did-registrar/dids/$shortFormDid/publications"),
        )

        val didOperationResponse = SerenityRest.lastResponse().get<DIDOperationResponse>()
        actor.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_ACCEPTED),
            Ensure.that(didOperationResponse.scheduledOperation.didRef).isNotEmpty(),
            Ensure.that(didOperationResponse.scheduledOperation.id).isNotEmpty(),
            PollingWait.until(ListenToEvents.didStatus(actor), equalTo("PUBLISHED")),
            Get.resource("/dids/${actor.recall<String>("shortFormDid")}"),
        )

        val didDocument = SerenityRest.lastResponse().get<DIDResolutionResult>().didDocument!!

        actor.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
            Ensure.that(didDocument.id).isEqualTo(actor.recall("shortFormDid")),
        )
        actor.remember("didVerification", didDocument.verificationMethod)
        actor.remember("hasPublishedDid", true)
    }

    @Then("{actor} resolves DID document corresponds to W3C standard")
    fun heSeesDidDocumentCorrespondsToW3cStandard(actor: Actor) {
        val didResolutionResult = SerenityRest.lastResponse().get<DIDResolutionResult>()
        val didDocument = didResolutionResult.didDocument!!
        val shortFormDid = actor.recall<String>("shortFormDid")
        actor.attemptsTo(
            Ensure.that(didDocument.id).isEqualTo(shortFormDid),
            Ensure.that(didResolutionResult.didDocumentMetadata.deactivated!!).isFalse(),
        )
    }

    private fun actualDidHasSamePurpose(agent: Actor, didType: DidType): Boolean {
        val actualPurpose: DidType = agent.recall<DidType>("didPurpose") ?: return false
        return actualPurpose == didType
    }

    private fun createDid(actor: Actor, didType: DidType, documentTemplate: DidDocumentTemplate) {
        val createDidRequest = CreateManagedDidRequest(
            CreateManagedDidRequestDocumentTemplate(
                publicKeys = documentTemplate.publicKeys,
                services = documentTemplate.services,
            ),
        )

        actor.attemptsTo(
            Post.to("/did-registrar/dids").body(createDidRequest),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
        )

        val managedDid = SerenityRest.lastResponse().get<ManagedDID>()

        actor.attemptsTo(
            Ensure.that(managedDid.longFormDid!!).isNotEmpty(),
            Get.resource("/did-registrar/dids/${managedDid.longFormDid}"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
        )

        val did = SerenityRest.lastResponse().get<ManagedDID>()

        actor.remember("longFormDid", managedDid.longFormDid)
        actor.remember("shortFormDid", did.did)
        actor.remember("didPurpose", didType)
        actor.forget<String>("hasPublishedDid")
    }
}
