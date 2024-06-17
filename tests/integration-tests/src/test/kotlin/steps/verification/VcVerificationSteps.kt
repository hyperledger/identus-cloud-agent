package steps.verification

import com.google.gson.Gson
import io.cucumber.java.en.When
import io.iohk.atala.automation.serenity.ensure.Ensure
import io.restassured.http.Header
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.interactions.Post
import org.apache.http.HttpStatus.SC_OK
import org.hyperledger.identus.client.models.*
import java.time.OffsetDateTime

class VcVerificationSteps {

    @When("{actor} verifies VcVerificationRequest")
    fun agentVerifiesVerifiableCredential(actor: Actor) {
        val signedJwtCredential =
            "eyJhbGciOiJFUzI1NksifQ.eyJpc3MiOiJkaWQ6cHJpc206NDE1ODg1OGI1ZjBkYWMyZTUwNDdmMjI4NTk4OWVlMzlhNTNkZWJhNzY0NjFjN2FmMDM5NjU0ZGYzYjU5MjI1YyIsImF1ZCI6ImRpZDpwcmlzbTp2ZXJpZmllciIsIm5iZiI6MTI2MjMwNDAwMCwiZXhwIjoxMjYzMjU0NDAwLCJ2YyI6eyJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy92MSIsImh0dHBzOi8vd3d3LnczLm9yZy8yMDE4L2NyZWRlbnRpYWxzL2V4YW1wbGVzL3YxIl0sInR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJVbml2ZXJzaXR5RGVncmVlQ3JlZGVudGlhbCJdLCJjcmVkZW50aWFsU2NoZW1hIjp7ImlkIjoiZGlkOndvcms6TURQOEFzRmhIemh3VXZHTnVZa1g3VDtpZD0wNmUxMjZkMS1mYTQ0LTQ4ODItYTI0My0xZTMyNmZiZTIxZGI7dmVyc2lvbj0xLjAiLCJ0eXBlIjoiSnNvblNjaGVtYVZhbGlkYXRvcjIwMTgifSwiY3JlZGVudGlhbFN1YmplY3QiOnsidXNlck5hbWUiOiJCb2IiLCJhZ2UiOjQyLCJlbWFpbCI6ImVtYWlsIn0sImNyZWRlbnRpYWxTdGF0dXMiOnsiaWQiOiJkaWQ6d29yazpNRFA4QXNGaEh6aHdVdkdOdVlrWDdUO2lkPTA2ZTEyNmQxLWZhNDQtNDg4Mi1hMjQzLTFlMzI2ZmJlMjFkYjt2ZXJzaW9uPTEuMCIsInR5cGUiOiJTdGF0dXNMaXN0MjAyMUVudHJ5Iiwic3RhdHVzUHVycG9zZSI6IlJldm9jYXRpb24iLCJzdGF0dXNMaXN0SW5kZXgiOjAsInN0YXR1c0xpc3RDcmVkZW50aWFsIjoiaHR0cHM6Ly9leGFtcGxlLmNvbS9jcmVkZW50aWFscy9zdGF0dXMvMyJ9LCJyZWZyZXNoU2VydmljZSI6eyJpZCI6Imh0dHBzOi8vZXhhbXBsZS5lZHUvcmVmcmVzaC8zNzMyIiwidHlwZSI6Ik1hbnVhbFJlZnJlc2hTZXJ2aWNlMjAxOCJ9fSwianRpIjoiaHR0cDovL2V4YW1wbGUuZWR1L2NyZWRlbnRpYWxzLzM3MzIifQ.HBxrn8Nu6y1RvUAU8XcwUDPOiiHhC1OgHN757lWai6i8P-pHL4TBzIDartYtrMiZUKpNx9Onb19sJYywtqFkpg"

        val request =
            arrayOf(
                VcVerificationRequest(
                    signedJwtCredential,
                    listOf(
                        ParameterizableVcVerification(
                            VcVerification.NOT_BEFORE_CHECK,
                            DateTimeParameter(dateTime = OffsetDateTime.parse("2010-01-01T00:00:00Z")),
                        ),
                        ParameterizableVcVerification(
                            VcVerification.EXPIRATION_CHECK,
                            DateTimeParameter(dateTime = OffsetDateTime.parse("2010-01-01T00:00:00Z")),
                        ),
                        ParameterizableVcVerification(
                            VcVerification.AUDIENCE_CHECK,
                            DidParameter(did = "did:prism:verifier"),
                        ),
                        ParameterizableVcVerification(
                            VcVerification.ISSUER_IDENTIFICATION,
                            DidParameter(did = "did:prism:4158858b5f0dac2e5047f2285989ee39a53deba76461c7af039654df3b59225c"),
                        ),
                    ),
                ),
                VcVerificationRequest(
                    signedJwtCredential,
                    listOf(
                        ParameterizableVcVerification(
                            VcVerification.EXPIRATION_CHECK,
                            DateTimeParameter(dateTime = OffsetDateTime.parse("2010-01-13T00:00:00Z")),
                        ),
                        ParameterizableVcVerification(
                            VcVerification.NOT_BEFORE_CHECK,
                            DateTimeParameter(dateTime = OffsetDateTime.parse("2009-01-01T00:00:00Z")),
                        ),
                        ParameterizableVcVerification(
                            VcVerification.AUDIENCE_CHECK,
                            DidParameter(did = "BAD AUDIENCE"),
                        ),
                        ParameterizableVcVerification(
                            VcVerification.ISSUER_IDENTIFICATION,
                            DidParameter(did = "BAD ISSUER"),
                        ),
                    ),
                ),
            )

        val post =
            Post.to("/verification/credential").with {
                it.header(Header("apiKey", "pylnapbvyudwmfrt"))
                it.body(request)
            }

        actor.attemptsTo(
            post,
        )

        val expected = listOf(
            VcVerificationResponse(
                signedJwtCredential,
                listOf(
                    VcVerificationResult(VcVerification.NOT_BEFORE_CHECK, true),
                    VcVerificationResult(VcVerification.EXPIRATION_CHECK, true),
                    VcVerificationResult(VcVerification.AUDIENCE_CHECK, true),
                    VcVerificationResult(VcVerification.ISSUER_IDENTIFICATION, true),
                ),
            ),
            VcVerificationResponse(
                signedJwtCredential,
                listOf(
                    VcVerificationResult(VcVerification.EXPIRATION_CHECK, false),
                    VcVerificationResult(VcVerification.NOT_BEFORE_CHECK, false),
                    VcVerificationResult(VcVerification.AUDIENCE_CHECK, false),
                    VcVerificationResult(VcVerification.ISSUER_IDENTIFICATION, false),
                ),
            ),
        )
        actor.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
            Ensure.that(
                SerenityRest.lastResponse().body().asString(),
            ).isEqualTo(
                Gson().toJson(expected),
            ),
        )
    }
}
