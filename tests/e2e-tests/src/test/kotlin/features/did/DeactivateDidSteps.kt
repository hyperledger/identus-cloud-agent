package features.did

import api_models.UpdatePrismDidRequest
import common.TestConstants
import common.Utils
import common.Utils.lastResponseObject
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.interactions.Get
import net.serenitybdd.screenplay.rest.interactions.Post
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import org.apache.http.HttpStatus
import org.hamcrest.Matchers

class DeactivateDidSteps {

    @Given("{actor} have published PRISM DID for deactivation")
    fun actorHavePublishedPrismDidForDeactivation(actor: Actor) {
        if (TestConstants.PRISM_DID_FOR_DEACTIVATION == null) {
            val publishDidSteps = PublishDidSteps()
            publishDidSteps.createsUnpublishedDid(actor)
            publishDidSteps.hePublishesDidToLedger(actor)
            TestConstants.PRISM_DID_FOR_DEACTIVATION = actor.recall("shortFormDid")
        }
    }

    @When("{actor} deactivates PRISM DID")
    fun actorIssuesDeactivateDidOperation(actor: Actor) {
        actor.attemptsTo(
            Post.to("/did-registrar/dids/${TestConstants.PRISM_DID_FOR_UPDATES}/deactivations")
                .with {
                    it.body(UpdatePrismDidRequest(listOf(actor.recall("updatePrismDidAction"))))
                },
        )
        actor.should(
            ResponseConsequence.seeThatResponse {
                it.statusCode(HttpStatus.SC_ACCEPTED)
                it.body("scheduledOperation.didRef", Matchers.not(Matchers.emptyString()))
                it.body("scheduledOperation.id", Matchers.not(Matchers.emptyString()))
            },
        )
    }

    @Then("{actor} sees that PRISM DID is successfully deactivated")
    fun actorSeesThatPrismDidIsSuccessfullyDeactivated(actor: Actor) {
        Utils.wait(
            {
                actor.attemptsTo(
                    Get.resource("/dids/${TestConstants.PRISM_DID_FOR_UPDATES}"),
                )
                lastResponseObject("metadata.deactivated", String::class) == "true"
            },
            "ERROR: DID deactivate operation did not succeed on the ledger!",
            timeout = TestConstants.DID_UPDATE_PUBLISH_MAX_WAIT_5_MIN,
        )
    }
}