package features.did

import common.TestConstants
import common.Utils.wait
import interactions.Get
import interactions.Post
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import io.iohk.atala.prism.models.DIDOperationResponse
import io.iohk.atala.prism.models.DIDResolutionResult
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus

class DeactivateDidSteps {

    @When("{actor} deactivates PRISM DID")
    fun actorIssuesDeactivateDidOperation(actor: Actor) {
        actor.attemptsTo(
            Post.to("/did-registrar/dids/${actor.recall<String>("shortFormDid")}/deactivations")
        )

        val didOperationResponse = SerenityRest.lastResponse().get<DIDOperationResponse>()

        actor.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_ACCEPTED),
            Ensure.that(didOperationResponse.scheduledOperation.didRef).isNotEmpty(),
            Ensure.that(didOperationResponse.scheduledOperation.id).isNotEmpty()
        )
    }

    @Then("{actor} sees that PRISM DID is successfully deactivated")
    fun actorSeesThatPrismDidIsSuccessfullyDeactivated(actor: Actor) {
        wait(
            {
                actor.attemptsTo(
                    Get.resource("/dids/${actor.recall<String>("shortFormDid")}")
                )
                SerenityRest.lastResponse().get<DIDResolutionResult>().didDocumentMetadata.deactivated!!
            },
            "ERROR: DID deactivate operation did not succeed on the ledger!",
            timeout = TestConstants.DID_UPDATE_PUBLISH_MAX_WAIT_5_MIN
        )
    }
}
