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
