package steps.did

import common.TestConstants
import interactions.Get
import interactions.Post
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import io.iohk.atala.automation.utils.Wait
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus
import org.hyperledger.identus.client.models.*
import java.util.UUID

class UpdateDidSteps {

    @When("{actor} updates PRISM DID by adding new key with {curve} curve and {purpose} purpose")
    fun actorUpdatesPrismDidByAddingNewKeys(actor: Actor, curve: Curve, purpose: Purpose) {
        val newDidKeyId = UUID.randomUUID().toString()
        val didKey = ManagedDIDKeyTemplate(
            id = newDidKeyId,
            purpose = purpose,
            curve = curve
        )
        val updatePrismDidAction = UpdateManagedDIDRequestAction(
            actionType = ActionType.ADD_KEY,
            addKey = didKey,
        )
        actor.remember("newDidKeyId", newDidKeyId)
        actor.remember("updatePrismDidAction", updatePrismDidAction)
    }

    @When("{actor} updates PRISM DID by removing keys")
    fun actorUpdatesPrismDidByRemovingKeys(actor: Actor) {
        val updatePrismDidAction = UpdateManagedDIDRequestAction(
            actionType = ActionType.REMOVE_KEY,
            removeKey = RemoveEntryById(TestConstants.PRISM_DID_AUTH_KEY.id),
        )
        actor.remember("updatePrismDidAction", updatePrismDidAction)
    }

    @When("{actor} updates PRISM DID with new services")
    fun actorUpdatesPrismDidWithNewServices(actor: Actor) {
        val updatePrismDidAction = UpdateManagedDIDRequestAction(
            actionType = ActionType.ADD_SERVICE,
            addService = TestConstants.PRISM_DID_UPDATE_NEW_SERVICE,
        )
        actor.remember("updatePrismDidAction", updatePrismDidAction)
    }

    @When("{actor} updates PRISM DID by removing services")
    fun actorUpdatesPrismDidByRemovingServices(actor: Actor) {
        val updatePrismDidAction = UpdateManagedDIDRequestAction(
            actionType = ActionType.REMOVE_SERVICE,
            removeService = RemoveEntryById(TestConstants.PRISM_DID_UPDATE_NEW_SERVICE.id),
        )
        actor.remember("updatePrismDidAction", updatePrismDidAction)
    }

    @When("{actor} updates PRISM DID by updating services")
    fun actorUpdatesPrismDidByUpdatingServices(actor: Actor) {
        val newService = UpdateManagedDIDServiceAction(
            id = TestConstants.PRISM_DID_SERVICE_FOR_UPDATE.id,
            type = TestConstants.PRISM_DID_SERVICE_FOR_UPDATE.type,
            serviceEndpoint = Json(
                TestConstants.PRISM_DID_UPDATE_NEW_SERVICE_URL,
            ),
        )
        val updatePrismDidAction = UpdateManagedDIDRequestAction(
            actionType = ActionType.UPDATE_SERVICE,
            updateService = newService,
        )
        actor.remember("updatePrismDidAction", updatePrismDidAction)
    }

    @When("{actor} submits PRISM DID update operation")
    fun actorSubmitsPrismDidUpdateOperation(actor: Actor) {
        actor.attemptsTo(
            Post.to("/did-registrar/dids/${actor.recall<String>("shortFormDid")}/updates")
                .with {
                    it.body(UpdateManagedDIDRequest(listOf(actor.recall("updatePrismDidAction"))))
                },
        )
        val didOperationResponse = SerenityRest.lastResponse().get<DIDOperationResponse>()
        actor.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_ACCEPTED),
            Ensure.that(didOperationResponse.scheduledOperation.didRef).isNotEmpty(),
            Ensure.that(didOperationResponse.scheduledOperation.id).isNotEmpty(),
        )
    }

    @Then("{actor} sees PRISM DID was successfully updated with new keys of {purpose} purpose")
    fun actorSeesDidSuccessfullyUpdatedWithNewKeys(actor: Actor, purpose: Purpose) {
        val newDidKeyId = actor.recall<String>("newDidKeyId")
        var i = 0
        Wait.until(
            errorMessage = "ERROR: DID UPDATE operation did not succeed on the ledger!",
        ) {
            actor.attemptsTo(
                Get.resource("/dids/${actor.recall<String>("shortFormDid")}"),
            )
            val didKey = "${actor.recall<String>("shortFormDid")}#${newDidKeyId}"
            val didDocument = SerenityRest.lastResponse().get<DIDResolutionResult>().didDocument!!
            val foundVerificationMethod = didDocument.verificationMethod!!.map { it.id }.any { it == didKey }

            foundVerificationMethod && when(purpose) {
                Purpose.ASSERTION_METHOD -> didDocument.assertionMethod!!.any { it == didKey }
                Purpose.AUTHENTICATION -> didDocument.authentication!!.any { it == didKey }
                Purpose.CAPABILITY_DELEGATION -> didDocument.capabilityDelegation!!.any { it == didKey }
                Purpose.CAPABILITY_INVOCATION -> didDocument.capabilityInvocation!!.any { it == didKey }
                Purpose.KEY_AGREEMENT -> didDocument.keyAgreement!!.any { it == didKey }
            }
        }
    }

    @Then("{actor} sees PRISM DID was successfully updated and keys removed")
    fun actorSeesDidSuccessfullyUpdatedAndKeysRemoved(actor: Actor) {
        Wait.until(
            errorMessage = "ERROR: DID UPDATE operation did not succeed on the ledger!",
        ) {
            actor.attemptsTo(
                Get.resource("/dids/${actor.recall<String>("shortFormDid")}"),
            )
            val authUris = SerenityRest.lastResponse().get<DIDResolutionResult>().didDocument!!.authentication!!
            val verificationMethods = SerenityRest.lastResponse()
                .get<DIDResolutionResult>().didDocument!!.verificationMethod!!.map { it.id }
            authUris.none {
                it == "${actor.recall<String>("shortFormDid")}#${TestConstants.PRISM_DID_AUTH_KEY.id}"
            } && verificationMethods.none {
                it == "${actor.recall<String>("shortFormDid")}#${TestConstants.PRISM_DID_AUTH_KEY.id}"
            }
        }
    }

    @Then("{actor} sees PRISM DID was successfully updated with new services")
    fun actorSeesDidSuccessfullyUpdatedWithNewServices(actor: Actor) {
        Wait.until(
            errorMessage = "ERROR: DID UPDATE operation did not succeed on the ledger!",
        ) {
            actor.attemptsTo(
                Get.resource("/dids/${actor.recall<String>("shortFormDid")}"),
            )
            val serviceIds =
                SerenityRest.lastResponse().get<DIDResolutionResult>().didDocument!!.service!!.map { it.id }
            serviceIds.any {
                it == "${actor.recall<String>("shortFormDid")}#${TestConstants.PRISM_DID_UPDATE_NEW_SERVICE.id}"
            }
        }
    }

    @Then("{actor} sees PRISM DID was successfully updated by removing services")
    fun actorSeesDidSuccessfullyUpdatedByRemovingServices(actor: Actor) {
        Wait.until(
            errorMessage = "ERROR: DID UPDATE operation did not succeed on the ledger!",
        ) {
            actor.attemptsTo(
                Get.resource("/dids/${actor.recall<String>("shortFormDid")}"),
            )
            val serviceIds =
                SerenityRest.lastResponse().get<DIDResolutionResult>().didDocument!!.service!!.map { it.id }
            serviceIds.none {
                it == "${actor.recall<String>("shortFormDid")}#${TestConstants.PRISM_DID_UPDATE_NEW_SERVICE.id}"
            }
        }
    }

    @Then("{actor} sees PRISM DID was successfully updated by updating services")
    fun actorSeesDidSuccessfullyUpdatedByUpdatingServices(actor: Actor) {
        Wait.until(
            errorMessage = "ERROR: DID UPDATE operation did not succeed on the ledger!",
        ) {
            actor.attemptsTo(
                Get.resource("/dids/${actor.recall<String>("shortFormDid")}"),
            )
            val service = SerenityRest.lastResponse().get<DIDResolutionResult>().didDocument!!.service!!
            service.any { it.serviceEndpoint.value.contains(TestConstants.PRISM_DID_UPDATE_NEW_SERVICE_URL) }
        }
    }
}
