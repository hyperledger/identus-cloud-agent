package features.did

import common.TestConstants
import common.Utils.wait
import interactions.Get
import interactions.Post
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import io.iohk.atala.prism.models.*
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus

class UpdateDidSteps {

    @When("{actor} updates PRISM DID by adding new keys")
    fun actorUpdatesPrismDidByAddingNewKeys(actor: Actor) {
        val updatePrismDidAction = UpdateManagedDIDRequestAction(
            actionType = ActionType.aDDKEY,
            ManagedDIDKeyTemplate("auth-2", Purpose.authentication)
        )
        actor.remember("updatePrismDidAction", updatePrismDidAction)
    }

    @When("{actor} updates PRISM DID by removing keys")
    fun actorUpdatesPrismDidByRemovingKeys(actor: Actor) {
        val updatePrismDidAction = UpdateManagedDIDRequestAction(
            actionType = ActionType.rEMOVEKEY,
            removeKey = RemoveEntryById("auth-1")
        )
        actor.remember("updatePrismDidAction", updatePrismDidAction)
    }

    @When("{actor} updates PRISM DID with new services")
    fun actorUpdatesPrismDidWithNewServices(actor: Actor) {
        val updatePrismDidAction = UpdateManagedDIDRequestAction(
            actionType = ActionType.aDDSERVICE,
            addService = Service(
                "https://new.service.com",
                listOf("LinkedDomains"),
                Json("https://new.service.com/")
            )
        )
        actor.remember("updatePrismDidAction", updatePrismDidAction)
    }

    @When("{actor} updates PRISM DID by removing services")
    fun actorUpdatesPrismDidByRemovingServices(actor: Actor) {
        val updatePrismDidAction = UpdateManagedDIDRequestAction(
            actionType = ActionType.rEMOVESERVICE,
            removeService = RemoveEntryById("https://new.service.com")
        )
        actor.remember("updatePrismDidAction", updatePrismDidAction)
    }

    @When("{actor} updates PRISM DID by updating services")
    fun actorUpdatesPrismDidByUpdatingServices(actor: Actor) {
        val newService = UpdateManagedDIDServiceAction(
            id = TestConstants.PRISM_DID_SERVICE_FOR_UPDATE.id,
            type = TestConstants.PRISM_DID_SERVICE_FOR_UPDATE.type,
            serviceEndpoint = Json(
                TestConstants.PRISM_DID_UPDATE_NEW_SERVICE_URL
            )
        )
        val updatePrismDidAction = UpdateManagedDIDRequestAction(
            actionType = ActionType.uPDATESERVICE,
            updateService = newService
        )
        actor.remember("updatePrismDidAction", updatePrismDidAction)
    }

    @When("{actor} submits PRISM DID update operation")
    fun actorSubmitsPrismDidUpdateOperation(actor: Actor) {
        actor.attemptsTo(
            Post.to("/did-registrar/dids/${actor.recall<String>("shortFormDid")}/updates")
                .with {
                    it.body(UpdateManagedDIDRequest(listOf(actor.recall("updatePrismDidAction"))))
                }
        )
        val didOperationResponse = SerenityRest.lastResponse().get<DIDOperationResponse>()
        actor.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_ACCEPTED),
            Ensure.that(didOperationResponse.scheduledOperation.didRef).isNotEmpty(),
            Ensure.that(didOperationResponse.scheduledOperation.id).isNotEmpty()
        )
    }

    @Then("{actor} sees PRISM DID was successfully updated with new keys")
    fun actorSeesDidSuccessfullyUpdatedWithNewKeys(actor: Actor) {
        wait(
            {
                actor.attemptsTo(
                    Get.resource("/dids/${actor.recall<String>("shortFormDid")}")
                )
                val authUris = SerenityRest.lastResponse().get<DIDResolutionResult>().didDocument!!.authentication!!
                val verificationMethods = SerenityRest.lastResponse().get<DIDResolutionResult>().didDocument!!.verificationMethod!!.map { it.id }
                authUris.any {
                    it == "${actor.recall<String>("shortFormDid")}#${TestConstants.PRISM_DID_UPDATE_NEW_AUTH_KEY.id}"
                } && verificationMethods.any {
                    it == "${actor.recall<String>("shortFormDid")}#${TestConstants.PRISM_DID_AUTH_KEY.id}"
                }
            },
            "ERROR: DID UPDATE operation did not succeed on the ledger!",
            timeout = TestConstants.DID_UPDATE_PUBLISH_MAX_WAIT_5_MIN
        )
    }

    @Then("{actor} sees PRISM DID was successfully updated and keys removed")
    fun actorSeesDidSuccessfullyUpdatedAndKeysRemoved(actor: Actor) {
        wait(
            {
                actor.attemptsTo(
                    Get.resource("/dids/${actor.recall<String>("shortFormDid")}")
                )
                val authUris = SerenityRest.lastResponse().get<DIDResolutionResult>().didDocument!!.authentication!!
                val verificationMethods = SerenityRest.lastResponse().get<DIDResolutionResult>().didDocument!!.verificationMethod!!.map { it.id }
                authUris.none {
                    it == "${actor.recall<String>("shortFormDid")}#${TestConstants.PRISM_DID_AUTH_KEY.id}"
                } && verificationMethods.none {
                    it == "${actor.recall<String>("shortFormDid")}#${TestConstants.PRISM_DID_AUTH_KEY.id}"
                }
            },
            "ERROR: DID UPDATE operation did not succeed on the ledger!",
            timeout = TestConstants.DID_UPDATE_PUBLISH_MAX_WAIT_5_MIN
        )
    }

    @Then("{actor} sees PRISM DID was successfully updated with new services")
    fun actorSeesDidSuccessfullyUpdatedWithNewServices(actor: Actor) {
        wait(
            {
                actor.attemptsTo(
                    Get.resource("/dids/${actor.recall<String>("shortFormDid")}")
                )
                val serviceIds = SerenityRest.lastResponse().get<DIDResolutionResult>().didDocument!!.service!!.map { it.id }
                serviceIds.any {
                    it == "${actor.recall<String>("shortFormDid")}#${TestConstants.PRISM_DID_UPDATE_NEW_SERVICE.id}"
                }
            },
            "ERROR: DID UPDATE operation did not succeed on the ledger!",
            timeout = TestConstants.DID_UPDATE_PUBLISH_MAX_WAIT_5_MIN
        )
    }

    @Then("{actor} sees PRISM DID was successfully updated by removing services")
    fun actorSeesDidSuccessfullyUpdatedByRemovingServices(actor: Actor) {
        wait(
            {
                actor.attemptsTo(
                    Get.resource("/dids/${actor.recall<String>("shortFormDid")}")
                )
                val serviceIds = SerenityRest.lastResponse().get<DIDResolutionResult>().didDocument!!.service!!.map { it.id }
                serviceIds.none {
                    it == "${actor.recall<String>("shortFormDid")}#${TestConstants.PRISM_DID_UPDATE_NEW_SERVICE.id}"
                }
            },
            "ERROR: DID UPDATE operation did not succeed on the ledger!",
            timeout = TestConstants.DID_UPDATE_PUBLISH_MAX_WAIT_5_MIN
        )
    }

    @Then("{actor} sees PRISM DID was successfully updated by updating services")
    fun actorSeesDidSuccessfullyUpdatedByUpdatingServices(actor: Actor) {
        wait(
            {
                actor.attemptsTo(
                    Get.resource("/dids/${actor.recall<String>("shortFormDid")}")
                )
                val service = SerenityRest.lastResponse().get<DIDResolutionResult>().didDocument!!.service!!
                service.any { it.serviceEndpoint.value.contains(TestConstants.PRISM_DID_UPDATE_NEW_SERVICE_URL) }
            },
            "ERROR: DID UPDATE operation did not succeed on the ledger!",
            timeout = TestConstants.DID_UPDATE_PUBLISH_MAX_WAIT_5_MIN
        )
    }
}
