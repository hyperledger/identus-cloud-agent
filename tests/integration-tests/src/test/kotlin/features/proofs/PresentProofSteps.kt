package features.proofs

import common.ListenToEvents
import common.Utils.wait
import interactions.Patch
import interactions.Post
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import io.iohk.atala.prism.models.*
import models.PresentationEvent
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus.SC_CREATED

class PresentProofSteps {

    private var proofEvent: PresentationEvent? = null

    @When("{actor} sends a request for proof presentation to {actor}")
    fun faberSendsARequestForProofPresentationToBob(faber: Actor, bob: Actor) {
        val presentationRequest = RequestPresentationInput(
            connectionId = faber.recall<Connection>("connection-with-${bob.name}").connectionId.toString(),
            options = Options(
                challenge = "11c91493-01b3-4c4d-ac36-b336bab5bddf",
                domain = "https://example-verifier.com"
            ),
            proofs = listOf(
                ProofRequestAux(
                    schemaId = "https://schema.org/Person",
                    trustIssuers = listOf("did:web:atalaprism.io/users/testUser")
                )
            )
        )
        faber.attemptsTo(
            Post.to("/present-proof/presentations")
                .with {
                    it.body(
                        presentationRequest
                    )
                }
        )
        faber.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED)
        )
        val presentationStatus = SerenityRest.lastResponse().get<PresentationStatus>()
        faber.remember("thid", presentationStatus.thid)
        bob.remember("thid", presentationStatus.thid)
    }

    @When("{actor} receives the request")
    fun bobReceivesTheRequest(bob: Actor) {
        wait(
            {
                proofEvent = ListenToEvents.`as`(bob).presentationEvents.lastOrNull {
                    it.data.thid == bob.recall<String>("thid")
                }
                proofEvent != null &&
                    proofEvent!!.data.status == PresentationStatus.Status.requestReceived
            },
            "ERROR: Bob did not achieve any presentation request!"
        )
        bob.remember("presentationId", proofEvent!!.data.presentationId)
    }

    @When("{actor} makes the presentation of the proof to {actor}")
    fun bobMakesThePresentationOfTheProof(bob: Actor, faber: Actor) {
        val requestPresentationAction = RequestPresentationAction(
            proofId = listOf(bob.recall<IssueCredentialRecord>("issuedCredential").recordId),
            action = RequestPresentationAction.Action.requestMinusAccept
        )

        bob.attemptsTo(
            Patch.to("/present-proof/presentations/${bob.recall<String>("presentationId")}").with {
                it.body(
                    requestPresentationAction
                )
            }
        )
    }

    @When("{actor} rejects the proof")
    fun bobRejectsProof(bob: Actor) {
        bob.attemptsTo(
            Patch.to("/present-proof/presentations/${bob.recall<String>("presentationId")}").with {
                it.body(
                    RequestPresentationAction(
                        action = RequestPresentationAction.Action.requestMinusReject
                    )
                )
            }
        )
    }

    @Then("{actor} sees the proof is rejected")
    fun bobSeesProofIsRejected(bob: Actor) {
        wait(
            {
                proofEvent = ListenToEvents.`as`(bob).presentationEvents.lastOrNull {
                    it.data.thid == bob.recall<String>("thid")
                }
                proofEvent != null &&
                    proofEvent!!.data.status == PresentationStatus.Status.requestRejected
            },
            "ERROR: Faber did not receive presentation from Bob!"
        )
    }

    @Then("{actor} has the proof verified")
    fun faberHasTheProofVerified(faber: Actor) {
        wait(
            {
                proofEvent = ListenToEvents.`as`(faber).presentationEvents.lastOrNull {
                    it.data.thid == faber.recall<String>("thid")
                }

                proofEvent != null &&
                    proofEvent!!.data.status == PresentationStatus.Status.presentationVerified
            },
            "ERROR: presentation did not achieve PresentationVerified state!"
        )
    }
}
