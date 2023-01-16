package features.present_proof

import api_models.Connection
import api_models.Credential
import api_models.PresentationProof
import common.Utils.lastResponseList
import common.Utils.lastResponseObject
import common.Utils.wait
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.interactions.Get
import net.serenitybdd.screenplay.rest.interactions.Patch
import net.serenitybdd.screenplay.rest.interactions.Post
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import org.apache.http.HttpStatus.SC_CREATED
import org.apache.http.HttpStatus.SC_OK

class PresentProofSteps {
    @When("{actor} sends a request for proof presentation to {actor}")
    fun faberSendsARequestForProofPresentationToBob(faber: Actor, bob: Actor) {
        faber.attemptsTo(
            Post.to("/present-proof/presentations")
                .with {
                    it.body(
                        """
                        { "connectionId": "${faber.recall<Connection>("connection-with-${bob.name}").connectionId}", "proofs":[] }
                        """.trimIndent(),
                    )
                },
        )
        faber.should(
            ResponseConsequence.seeThatResponse("Presentation proof request created") {
                it.statusCode(SC_CREATED)
            },
        )
        faber.remember("presentationId", lastResponseObject("", PresentationProof::class).presentationId)
    }

    @When("{actor} makes the presentation of the proof to {actor}")
    fun bobMakesThePresentationOfTheProof(bob: Actor, faber: Actor) {
        wait(
            {
                bob.attemptsTo(
                    Get.resource("/present-proof/presentations"),
                )
                bob.should(
                    ResponseConsequence.seeThatResponse("Get presentations") {
                        it.statusCode(SC_OK)
                    },
                )
                lastResponseList("", PresentationProof::class).findLast {
                    it.status == "RequestReceived"
                } != null
            },
            "ERROR: Bob did not achieve any presentation request!",
        )

        val presentationId = lastResponseList("", PresentationProof::class).findLast {
            it.status == "RequestReceived"
        }!!.presentationId
        bob.attemptsTo(
            Patch.to("/present-proof/presentations/$presentationId").with {
                it.body(
                    """
                        { "action": "request-accept", "proofId": ["${bob.recall<Credential>("issuedCredential").recordId}"] }
                    """.trimIndent(),
                )
            },
        )
    }

    @When("{actor} acknowledges the proof")
    fun faberAcknowledgesTheProof(faber: Actor) {
        wait(
            {
                faber.attemptsTo(
                    Get.resource("/present-proof/presentations"),
                )
                faber.should(
                    ResponseConsequence.seeThatResponse("Get presentations") {
                        it.statusCode(SC_OK)
                    },
                )
                val presentation = lastResponseList("", PresentationProof::class).find {
                    it.presentationId == faber.recall<String>("presentationId")
                }
                presentation != null &&
                    (presentation.status == "PresentationReceived" || presentation.status == "PresentationVerified")
            },
            "ERROR: Faber did not receive presentation from Bob!",
        )
    }

    @Then("{actor} has the proof verified")
    fun faberHasTheProofVerified(faber: Actor) {
        wait(
            {
                faber.attemptsTo(
                    Get.resource("/present-proof/presentations"),
                )
                faber.should(
                    ResponseConsequence.seeThatResponse("Get presentations") {
                        it.statusCode(SC_OK)
                    },
                )
                val presentation = lastResponseList("", PresentationProof::class).find {
                    it.presentationId == faber.recall<String>("presentationId")
                }
                presentation != null && presentation.status == "PresentationVerified"
            },
            "ERROR: presentation did not achieve PresentationVerified state!",
        )
    }
}
