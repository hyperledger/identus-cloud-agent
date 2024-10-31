package steps.did

import abilities.ListenToEvents
import common.DidPurpose
import interactions.Get
import interactions.Post
import interactions.body
import io.cucumber.java.en.*
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import io.iohk.atala.automation.serenity.interactions.PollingWait
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus
import org.apache.http.HttpStatus.SC_CREATED
import org.apache.http.HttpStatus.SC_OK
import org.hamcrest.CoreMatchers.equalTo
import org.hyperledger.identus.client.models.*

class PublishDidSteps {

    @Given("{actor} has a published DID for {}")
    fun agentHasAPublishedDID(agent: Actor, didPurpose: DidPurpose) {
        if (agent.recallAll().containsKey("hasPublishedDid") && actualDidHasSamePurpose(agent, didPurpose)) {
            return
        }
        agentHasAnUnpublishedDID(agent, didPurpose)
        hePublishesDidToLedger(agent)
    }

    @Given("{actor} has an unpublished DID for {}")
    fun agentHasAnUnpublishedDID(agent: Actor, didPurpose: DidPurpose) {
        if (agent.recallAll().containsKey("shortFormDid") || agent.recallAll().containsKey("longFormDid")) {
            // is not published and has the same purpose
            if (!agent.recallAll().containsKey("hasPublishedDid") && actualDidHasSamePurpose(agent, didPurpose)) {
                return
            }
        }
        agentCreatesUnpublishedDid(agent, didPurpose)
    }

    private fun actualDidHasSamePurpose(agent: Actor, didPurpose: DidPurpose): Boolean {
        val actualPurpose: DidPurpose = agent.recall<DidPurpose?>("didPurpose") ?: return false
        return actualPurpose == didPurpose
    }

    @Given("{actor} creates unpublished DID")
    fun agentCreatesEmptyUnpublishedDid(actor: Actor) {
        agentCreatesUnpublishedDid(actor, DidPurpose.CUSTOM)
    }

    @Given("{actor} creates unpublished DID for {}")
    fun agentCreatesUnpublishedDid(actor: Actor, didPurpose: DidPurpose) {
        val createDidRequest = CreateManagedDidRequest(
            CreateManagedDidRequestDocumentTemplate(didPurpose.publicKeys, services = didPurpose.services),
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
        actor.remember("kidSecp256K1", "auth-1")
        actor.remember("kidEd25519", "auth-2")
        actor.remember("shortFormDid", did.did)
        actor.remember("didPurpose", didPurpose)
        actor.forget<String>("hasPublishedDid")
    }

    @When("{actor} prepares a custom PRISM DID")
    fun actorPreparesCustomDid(actor: Actor) {
        val customDid = DidPurpose.CUSTOM
        actor.remember("customDid", customDid)
    }

    @When("{actor} adds a '{curve}' key for '{purpose}' purpose with '{}' name to the custom PRISM DID")
    fun actorAddsKeyToCustomDid(actor: Actor, curve: Curve, purpose: Purpose, name: String) {
        val customDid = actor.recall<DidPurpose>("customDid")
        customDid.publicKeys.add(ManagedDIDKeyTemplate(name, purpose, curve))
    }

    @When("{actor} creates the custom PRISM DID")
    fun actorCreatesTheCustomPrismDid(actor: Actor) {
        val customDid = actor.recall<DidPurpose>("customDid")
        agentCreatesUnpublishedDid(actor, customDid)
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
}
