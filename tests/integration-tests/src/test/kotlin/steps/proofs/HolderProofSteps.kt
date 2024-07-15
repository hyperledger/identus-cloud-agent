package steps.proofs

import abilities.ListenToEvents
import interactions.*
import io.cucumber.java.en.When
import io.iohk.atala.automation.serenity.interactions.PollingWait
import models.PresentationStatusAdapter.Status.REQUEST_RECEIVED
import net.serenitybdd.screenplay.Actor
import org.hamcrest.CoreMatchers.equalTo
import org.hyperledger.identus.client.models.RequestPresentationAction

class HolderProofSteps {

    @When("{actor} rejects the proof")
    fun holderRejectsProof(holder: Actor) {
        val presentationId: String = holder.recall("presentationId")
        holder.attemptsTo(
            Patch.to("/present-proof/presentations/$presentationId").body(
                RequestPresentationAction(action = RequestPresentationAction.Action.REQUEST_MINUS_REJECT),
            ),
        )
    }

    @When("{actor} receives the presentation proof request")
    fun holderReceivesTheRequest(holder: Actor) {
        holder.attemptsTo(
            PollingWait.until(
                ListenToEvents.presentationProofStatus(holder),
                equalTo(REQUEST_RECEIVED),
            ),
        )
        val presentationId = ListenToEvents.with(holder).presentationEvents.last().data.presentationId
        holder.remember("presentationId", presentationId)
    }
}
