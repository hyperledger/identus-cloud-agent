package steps.proofs

import abilities.ListenToEvents
import interactions.Patch
import interactions.Post
import interactions.body
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import io.iohk.atala.automation.utils.Wait
import models.PresentationStatusAdapter
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus.*
import org.hyperledger.identus.client.models.*
import kotlin.time.Duration.Companion.seconds

class PresentProofSteps {

    @When("{actor} sends a request for proof presentation to {actor}")
    fun verifierSendsARequestForProofPresentationToHolder(verifier: Actor, holder: Actor) {
        val verifierConnectionToHolder = verifier.recall<Connection>("connection-with-${holder.name}").connectionId
        val presentationRequest = RequestPresentationInput(
            connectionId = verifierConnectionToHolder,
            options = Options(
                challenge = "11c91493-01b3-4c4d-ac36-b336bab5bddf",
                domain = "https://example-verifier.com",
            ),
            proofs = listOf(
                ProofRequestAux(
                    schemaId = "https://schema.org/Person",
                    trustIssuers = listOf("did:web:atalaprism.io/users/testUser"),
                ),
            ),
        )
        verifier.attemptsTo(
            Post.to("/present-proof/presentations").body(presentationRequest),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
        )
        val presentationStatus = SerenityRest.lastResponse().get<PresentationStatus>()
        verifier.remember("thid", presentationStatus.thid)
        holder.remember("thid", presentationStatus.thid)
    }

    @When("{actor} receives the presentation proof request")
    fun holderReceivesTheRequest(holder: Actor) {
        Wait.until(
            timeout = 30.seconds,
            errorMessage = "ERROR: Bob did not achieve any presentation request!",
        ) {
            val proofEvent = ListenToEvents.with(holder).presentationEvents.lastOrNull {
                it.data.thid == holder.recall<String>("thid")
            }
            holder.remember("presentationId", proofEvent?.data?.presentationId)
            proofEvent?.data?.status == PresentationStatusAdapter.Status.REQUEST_RECEIVED
        }
    }

    @When("{actor} makes the presentation of the proof")
    fun holderMakesThePresentationOfTheProofToVerifier(holder: Actor) {
        val requestPresentationAction = RequestPresentationAction(
            proofId = listOf(holder.recall<IssueCredentialRecord>("issuedCredential").recordId),
            action = RequestPresentationAction.Action.REQUEST_MINUS_ACCEPT,
        )
        val presentationId: String = holder.recall("presentationId")
        holder.attemptsTo(
            Patch.to("/present-proof/presentations/$presentationId").body(requestPresentationAction),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
        )
    }

    @When("{actor} rejects the proof")
    fun holderRejectsProof(holder: Actor) {
        val presentationId: String = holder.recall("presentationId")
        holder.attemptsTo(
            Patch.to("/present-proof/presentations/$presentationId").with {
                it.body(
                    RequestPresentationAction(action = RequestPresentationAction.Action.REQUEST_MINUS_REJECT),
                )
            },
        )
    }

    @Then("{actor} sees the proof is rejected")
    fun bobSeesProofIsRejected(bob: Actor) {
        Wait.until(
            timeout = 30.seconds,
            errorMessage = "ERROR: Faber did not receive presentation from Bob!",
        ) {
            val proofEvent = ListenToEvents.with(bob).presentationEvents.lastOrNull {
                it.data.thid == bob.recall<String>("thid")
            }
            proofEvent?.data?.status == PresentationStatusAdapter.Status.REQUEST_REJECTED
        }
    }

    @Then("{actor} has the proof verified")
    fun faberHasTheProofVerified(faber: Actor) {
        Wait.until(
            timeout = 60.seconds,
            errorMessage = "Presentation did not achieve PresentationVerified state!",
        ) {
            val proofEvent = ListenToEvents.with(faber).presentationEvents.lastOrNull {
                it.data.thid == faber.recall<String>("thid")
            }
            proofEvent?.data?.status == PresentationStatusAdapter.Status.PRESENTATION_VERIFIED
        }
    }

    @Then("{actor} sees the proof returned verification failed")
    fun verifierSeesTheProofReturnedVerificationFailed(verifier: Actor) {
        Wait.until(
            timeout = 120.seconds,
            errorMessage = "Presentation did not achieve PresentationVerificationFailed state!",
        ) {
            val proofEvent = ListenToEvents.with(verifier).presentationEvents.lastOrNull {
                it.data.thid == verifier.recall<String>("thid")
            }
            proofEvent?.data?.status == PresentationStatusAdapter.Status.PRESENTATION_VERIFICATION_FAILED
        }
    }
}
