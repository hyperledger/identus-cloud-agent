package features.did

import api_models.*
import common.TestConstants
import common.Utils.lastResponseList
import common.Utils.wait
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.interactions.Get
import net.serenitybdd.screenplay.rest.interactions.Post
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import org.apache.http.HttpStatus
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.not

class UpdateDidSteps {

    @When("{actor} updates PRISM DID by adding new keys")
    fun actorUpdatesPrismDidByAddingNewKeys(actor: Actor) {
        val updatePrismDidAction = UpdatePrismDidAction(
            actionType = "ADD_KEY",
            addKey = TestConstants.PRISM_DID_UPDATE_NEW_AUTH_KEY,
        )
        actor.remember("updatePrismDidAction", updatePrismDidAction)
    }

    @When("{actor} updates PRISM DID by removing keys")
    fun actorUpdatesPrismDidByRemovingKeys(actor: Actor) {
        val updatePrismDidAction = UpdatePrismDidAction(
            actionType = "REMOVE_KEY",
            removeKey = TestConstants.PRISM_DID_AUTH_KEY,
        )
        actor.remember("updatePrismDidAction", updatePrismDidAction)
    }

    @When("{actor} updates PRISM DID with new services")
    fun actorUpdatesPrismDidWithNewServices(actor: Actor) {
        val updatePrismDidAction = UpdatePrismDidAction(
            actionType = "ADD_SERVICE",
            addService = TestConstants.PRISM_DID_UPDATE_NEW_SERVICE,
        )
        actor.remember("updatePrismDidAction", updatePrismDidAction)
    }

    @When("{actor} updates PRISM DID by removing services")
    fun actorUpdatesPrismDidByRemovingServices(actor: Actor) {
        val updatePrismDidAction = UpdatePrismDidAction(
            actionType = "REMOVE_SERVICE",
            removeService = TestConstants.PRISM_DID_UPDATE_NEW_SERVICE,
        )
        actor.remember("updatePrismDidAction", updatePrismDidAction)
    }

    @When("{actor} updates PRISM DID by updating services")
    fun actorUpdatesPrismDidByUpdatingServices(actor: Actor) {
        val newService = Service(
            id = TestConstants.PRISM_DID_SERVICE_FOR_UPDATE.id,
            type = TestConstants.PRISM_DID_SERVICE_FOR_UPDATE.type,
            serviceEndpoint = listOf(
                TestConstants.PRISM_DID_UPDATE_NEW_SERVICE_URL,
            ),
        )
        val updatePrismDidAction = UpdatePrismDidAction(
            actionType = "UPDATE_SERVICE",
            updateService = newService,
        )
        actor.remember("updatePrismDidAction", updatePrismDidAction)
    }

    @When("{actor} submits PRISM DID update operation")
    fun actorSubmitsPrismDidUpdateOperation(actor: Actor) {
        actor.attemptsTo(
            Post.to("/did-registrar/dids/${actor.recall<String>("shortFormDid")}/updates")
                .with {
                    it.body(UpdatePrismDidRequest(listOf(actor.recall("updatePrismDidAction"))))
                },
        )
        actor.should(
            ResponseConsequence.seeThatResponse {
                it.statusCode(HttpStatus.SC_ACCEPTED)
                it.body("scheduledOperation.didRef", not(emptyString()))
                it.body("scheduledOperation.id", not(emptyString()))
            },
        )
    }

    @Then("{actor} sees PRISM DID was successfully updated with new keys")
    fun actorSeesDidSuccessfullyUpdatedWithNewKeys(actor: Actor) {
        wait(
            {
                actor.attemptsTo(
                    Get.resource("/dids/${actor.recall<String>("shortFormDid")}"),
                )
                val authUris = lastResponseList("did.authentication.uri", String::class)
                val verificationMethods = lastResponseList("did.verificationMethod.id", String::class)
                authUris.any {
                    it == "${actor.recall<String>("shortFormDid")}#${TestConstants.PRISM_DID_UPDATE_NEW_AUTH_KEY.id}"
                } && verificationMethods.any {
                    it == "${actor.recall<String>("shortFormDid")}#${TestConstants.PRISM_DID_AUTH_KEY.id}"
                }
            },
            "ERROR: DID UPDATE operation did not succeed on the ledger!",
            timeout = TestConstants.DID_UPDATE_PUBLISH_MAX_WAIT_5_MIN,
        )
    }

    @Then("{actor} sees PRISM DID was successfully updated and keys removed")
    fun actorSeesDidSuccessfullyUpdatedAndKeysRemoved(actor: Actor) {
        wait(
            {
                actor.attemptsTo(
                    Get.resource("/dids/${actor.recall<String>("shortFormDid")}"),
                )
                val authUris = lastResponseList("did.authentication.uri", String::class)
                val verificationMethods = lastResponseList("did.verificationMethod.id", String::class)
                authUris.none {
                    it == "${actor.recall<String>("shortFormDid")}#${TestConstants.PRISM_DID_AUTH_KEY.id}"
                } && verificationMethods.none {
                    it == "${actor.recall<String>("shortFormDid")}#${TestConstants.PRISM_DID_AUTH_KEY.id}"
                }
            },
            "ERROR: DID UPDATE operation did not succeed on the ledger!",
            timeout = TestConstants.DID_UPDATE_PUBLISH_MAX_WAIT_5_MIN,
        )
    }

    @Then("{actor} sees PRISM DID was successfully updated with new services")
    fun actorSeesDidSuccessfullyUpdatedWithNewServices(actor: Actor) {
        wait(
            {
                actor.attemptsTo(
                    Get.resource("/dids/${actor.recall<String>("shortFormDid")}"),
                )
                val serviceIds = lastResponseList("did.service.id", String::class)
                serviceIds.any {
                    it == "${actor.recall<String>("shortFormDid")}#${TestConstants.PRISM_DID_UPDATE_NEW_SERVICE.id}"
                }
            },
            "ERROR: DID UPDATE operation did not succeed on the ledger!",
            timeout = TestConstants.DID_UPDATE_PUBLISH_MAX_WAIT_5_MIN,
        )
    }

    @Then("{actor} sees PRISM DID was successfully updated by removing services")
    fun actorSeesDidSuccessfullyUpdatedByRemovingServices(actor: Actor) {
        wait(
            {
                actor.attemptsTo(
                    Get.resource("/dids/${actor.recall<String>("shortFormDid")}"),
                )
                val serviceIds = lastResponseList("did.service.id", String::class)
                serviceIds.none {
                    it == "${actor.recall<String>("shortFormDid")}#${TestConstants.PRISM_DID_UPDATE_NEW_SERVICE.id}"
                }
            },
            "ERROR: DID UPDATE operation did not succeed on the ledger!",
            timeout = TestConstants.DID_UPDATE_PUBLISH_MAX_WAIT_5_MIN,
        )
    }

    @Then("{actor} sees PRISM DID was successfully updated by updating services")
    fun actorSeesDidSuccessfullyUpdatedByUpdatingServices(actor: Actor) {
        wait(
            {
                actor.attemptsTo(
                    Get.resource("/dids/${actor.recall<String>("shortFormDid")}"),
                )
                val service = lastResponseList("did.service", Service::class)
                service.any { it.serviceEndpoint.contains(TestConstants.PRISM_DID_UPDATE_NEW_SERVICE_URL) }
            },
            "ERROR: DID UPDATE operation did not succeed on the ledger!",
            timeout = TestConstants.DID_UPDATE_PUBLISH_MAX_WAIT_5_MIN,
        )
    }
}
