package features.present_proof

import api_models.*
import common.ListenToEvents
import common.Utils.lastResponseObject
import common.Utils.wait
import interactions.Get
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.Actor
import interactions.Post
import interactions.Patch
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import org.apache.http.HttpStatus.SC_CREATED
import org.apache.http.HttpStatus.SC_OK

class PresentProofSteps {

    var proofEvent: PresentationEvent? = null

    @When("{actor} sends a request for proof presentation to {actor}")
    fun faberSendsARequestForProofPresentationToBob(faber: Actor, bob: Actor) {
        faber.attemptsTo(
            Post.to("/present-proof/presentations")
                .with {
                    it.body(
                        """
                            {
                                "description":"Request presentation of credential",
                                "connectionId": "${faber.recall<Connection>("connection-with-${bob.name}").connectionId}",
                                "options":{
                                    "challenge": "11c91493-01b3-4c4d-ac36-b336bab5bddf",
                                    "domain": "https://example-verifier.com"
                                },
                                "proofs":[
                                    {
                                        "schemaId": "https://schema.org/Person",
                                        "trustIssuers": [
                                            "did:web:atalaprism.io/users/testUser"
                                        ]
                                    }
                                ]
                            }
                        """.trimIndent(),
                    )
                },
        )
        faber.should(
            ResponseConsequence.seeThatResponse {
                it.statusCode(SC_CREATED)
            },
        )

        val presentationId = lastResponseObject("", PresentationProof::class).presentationId
        faber.remember("presentationId", presentationId)
        faber.attemptsTo(
            Get.resource("/present-proof/presentations/${presentationId}"),
        )
        faber.should(
            ResponseConsequence.seeThatResponse("Get presentations") {
                it.statusCode(SC_OK)
            },
        )
        faber.remember("thid", lastResponseObject("", PresentationProof::class).thid)
        bob.remember("thid", lastResponseObject("", PresentationProof::class).thid)
    }

    @When("{actor} receives the request")
    fun bobReceivesTheRequest(bob: Actor) {
        wait(
            {
                proofEvent = ListenToEvents.`as`(bob).presentationEvents.lastOrNull {
                    it.data.thid == bob.recall<String>("thid")
                }
                proofEvent != null &&
                        proofEvent!!.data.status == PresentationProofStatus.REQUEST_RECEIVED
            },
            "ERROR: Bob did not achieve any presentation request!",
        )
        bob.remember("presentationId", proofEvent!!.data.presentationId)
    }

    @When("{actor} makes the presentation of the proof to {actor}")
    fun bobMakesThePresentationOfTheProof(bob: Actor, faber: Actor) {
        bob.attemptsTo(
            Patch.to("/present-proof/presentations/${bob.recall<String>("presentationId")}").with {
                it.body(
                    """
                        { "action": "request-accept", "proofId": ["${bob.recall<Credential>("issuedCredential").recordId}"] }
                    """.trimIndent(),
                )
            },
        )
    }

    @When("{actor} rejects the proof")
    fun bobRejectsProof(bob: Actor) {
        bob.attemptsTo(
            Patch.to("/present-proof/presentations/${bob.recall<String>("presentationId")}").with {
                it.body("""{ "action": "request-reject" }""")
            },
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
                        proofEvent!!.data.status == PresentationProofStatus.REQUEST_REJECTED
            },
            "ERROR: Faber did not receive presentation from Bob!",
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
                        proofEvent!!.data.status == PresentationProofStatus.PRESENTATION_VERIFIED
            },
            "ERROR: presentation did not achieve PresentationVerified state!",
        )
    }
}
