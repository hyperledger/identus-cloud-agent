package steps.verification

import interactions.Post
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import org.hyperledger.identus.client.models.*
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus.SC_OK
import java.time.OffsetDateTime

class VcVerificationSteps {

    @When("{actor} verifies VcVerificationRequest")
    fun agentVerifiesVerifiableCredential(actor: Actor) {
        val signedJwtCredential =
            "eyJhbGciOiJFUzI1NksifQ.eyJhdWQiOiJkaWQ6cHJpc206dmVyaWZpZXIiLCJuYmYiOjEyNjIzMDQwMDAsImlzcyI6ImRpZDpwcmlzbTo3NzYxMjBlZWIxMjhjZTdkZmQ5NDUwZmZhMTg4MWU5OTYxOWFhMGM5MDRiMDBjODJiYjE3YjU2ODE3Y2IwMmFlIiwiZXhwIjoxMjYzMjU0NDAwLCJ2YyI6eyJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy92MSIsImh0dHBzOi8vd3d3LnczLm9yZy8yMDE4L2NyZWRlbnRpYWxzL2V4YW1wbGVzL3YxIl0sInR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJVbml2ZXJzaXR5RGVncmVlQ3JlZGVudGlhbCJdLCJjcmVkZW50aWFsU2NoZW1hIjp7ImlkIjoiZGlkOndvcms6TURQOEFzRmhIemh3VXZHTnVZa1g3VDtpZD0wNmUxMjZkMS1mYTQ0LTQ4ODItYTI0My0xZTMyNmZiZTIxZGI7dmVyc2lvbj0xLjAiLCJ0eXBlIjoiSnNvblNjaGVtYVZhbGlkYXRvcjIwMTgifSwiY3JlZGVudGlhbFN1YmplY3QiOnsidXNlck5hbWUiOiJCb2IiLCJhZ2UiOjQyLCJlbWFpbCI6ImVtYWlsIn0sImNyZWRlbnRpYWxTdGF0dXMiOnsiaWQiOiJkaWQ6d29yazpNRFA4QXNGaEh6aHdVdkdOdVlrWDdUO2lkPTA2ZTEyNmQxLWZhNDQtNDg4Mi1hMjQzLTFlMzI2ZmJlMjFkYjt2ZXJzaW9uPTEuMCIsInR5cGUiOiJTdGF0dXNMaXN0MjAyMUVudHJ5Iiwic3RhdHVzUHVycG9zZSI6IlJldm9jYXRpb24iLCJzdGF0dXNMaXN0SW5kZXgiOjAsInN0YXR1c0xpc3RDcmVkZW50aWFsIjoiaHR0cHM6Ly9leGFtcGxlLmNvbS9jcmVkZW50aWFscy9zdGF0dXMvMyJ9LCJyZWZyZXNoU2VydmljZSI6eyJpZCI6Imh0dHBzOi8vZXhhbXBsZS5lZHUvcmVmcmVzaC8zNzMyIiwidHlwZSI6Ik1hbnVhbFJlZnJlc2hTZXJ2aWNlMjAxOCJ9fSwianRpIjoiaHR0cDovL2V4YW1wbGUuZWR1L2NyZWRlbnRpYWxzLzM3MzIifQ.JCHIAQdjmOxOdZ1SIf5Nd8FObiARXT6cDcGM3UyQ961Kv4Rb3ZtgpNM-cf2aj5ZFyFko-t7uCsSvrVrYKUYcWg"
        val request =
            listOf(
                VcVerificationRequest(
                    signedJwtCredential,
                    listOf(
                        ParameterizableVcVerification(VcVerification.SIGNATURE_VERIFICATION),
                        ParameterizableVcVerification(
                            VcVerification.NOT_BEFORE_CHECK,
                            VcVerificationParameter(dateTime = OffsetDateTime.now()),
                        ),
                        ParameterizableVcVerification(
                            VcVerification.EXPIRATION_CHECK,
                            VcVerificationParameter(dateTime = OffsetDateTime.now()),
                        ),
                    ),
                ),
                VcVerificationRequest(
                    signedJwtCredential,
                    listOf(
                        ParameterizableVcVerification(
                            VcVerification.AUDIENCE_CHECK,
                            VcVerificationParameter(aud = "did:prism:verifier"),
                        ),
                    ),
                ),
            )
        actor.attemptsTo(
            Post.to("/verification/credential").with {
                it.body(request)
            },
        )
        val vcVerificationResponses = SerenityRest.lastResponse().get<List<VcVerificationResponse>>()

        actor.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
            Ensure.that(vcVerificationResponses).containsExactlyInAnyOrderElementsFrom(
                listOf(
                    VcVerificationResponse(
                        signedJwtCredential,
                        listOf(
                            VcVerificationResult(VcVerification.SIGNATURE_VERIFICATION, false),
                            VcVerificationResult(VcVerification.NOT_BEFORE_CHECK, true),
                            VcVerificationResult(VcVerification.EXPIRATION_CHECK, true),
                        ),
                    ),
                    VcVerificationResponse(
                        signedJwtCredential,
                        listOf(
                            VcVerificationResult(VcVerification.AUDIENCE_CHECK, true),
                        ),
                    ),
                ),
            ),
        )
    }
}
