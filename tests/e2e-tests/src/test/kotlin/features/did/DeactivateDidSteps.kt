package features.did

import common.TestConstants
import common.Utils
import common.Utils.lastResponseObject
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.Actor
import interactions.Get
import interactions.Post
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import org.apache.http.HttpStatus
import org.hamcrest.Matchers

class DeactivateDidSteps {

    @When("{actor} deactivates PRISM DID")
    fun actorIssuesDeactivateDidOperation(actor: Actor) {
        actor.attemptsTo(
            Post.to("/did-registrar/dids/${actor.recall<String>("shortFormDid")}/deactivations"),
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
                    Get.resource("/dids/${actor.recall<String>("shortFormDid")}"),
                )
                lastResponseObject("didDocumentMetadata.deactivated", String::class) == "true"
            },
            "ERROR: DID deactivate operation did not succeed on the ledger!",
            timeout = TestConstants.DID_UPDATE_PUBLISH_MAX_WAIT_5_MIN,
        )
    }
}
