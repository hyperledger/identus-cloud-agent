package steps.proofs

import abilities.ListenToEvents.Companion.presentationProofStatus
import io.cucumber.java.en.Then
import io.iohk.atala.automation.serenity.interactions.PollingWait
import models.PresentationStatusAdapter.Status.*
import net.serenitybdd.screenplay.Actor
import org.hamcrest.CoreMatchers.equalTo

class VerifierProofSteps {

    @Then("{actor} sees the proof returned verification failed")
    fun verifierSeesTheProofReturnedVerificationFailed(verifier: Actor) {
        verifier.attemptsTo(
            PollingWait.until(presentationProofStatus(verifier), equalTo(PRESENTATION_VERIFICATION_FAILED)),
        )
    }

    @Then("{actor} sees the proof is rejected")
    fun verifierSeesProofIsRejected(verifier: Actor) {
        verifier.attemptsTo(
            PollingWait.until(presentationProofStatus(verifier), equalTo(REQUEST_REJECTED)),
        )
    }

    @Then("{actor} has the proof verified")
    fun verifierHasTheProofVerified(verifier: Actor) {
        verifier.attemptsTo(
            PollingWait.until(presentationProofStatus(verifier), equalTo(PRESENTATION_VERIFIED)),
        )
    }
}
