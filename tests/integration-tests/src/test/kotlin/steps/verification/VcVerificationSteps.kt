package steps.verification

import io.cucumber.java.en.When
import io.iohk.atala.automation.serenity.ensure.Ensure
import io.restassured.http.Header
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.interactions.Post
import org.apache.http.HttpStatus.SC_OK

class VcVerificationSteps {

    @When("{actor} verifies VcVerificationRequest")
    fun agentVerifiesVerifiableCredential(actor: Actor) {
        val signedJwtCredential =
            "eyJhbGciOiJFUzI1NksifQ.eyJpc3MiOiJkaWQ6cHJpc206NDE1ODg1OGI1ZjBkYWMyZTUwNDdmMjI4NTk4OWVlMzlhNTNkZWJhNzY0NjFjN2FmMDM5NjU0ZGYzYjU5MjI1YyIsImF1ZCI6ImRpZDpwcmlzbTp2ZXJpZmllciIsIm5iZiI6MTI2MjMwNDAwMCwiZXhwIjoxMjYzMjU0NDAwLCJ2YyI6eyJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy92MSIsImh0dHBzOi8vd3d3LnczLm9yZy8yMDE4L2NyZWRlbnRpYWxzL2V4YW1wbGVzL3YxIl0sInR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJVbml2ZXJzaXR5RGVncmVlQ3JlZGVudGlhbCJdLCJjcmVkZW50aWFsU2NoZW1hIjp7ImlkIjoiZGlkOndvcms6TURQOEFzRmhIemh3VXZHTnVZa1g3VDtpZD0wNmUxMjZkMS1mYTQ0LTQ4ODItYTI0My0xZTMyNmZiZTIxZGI7dmVyc2lvbj0xLjAiLCJ0eXBlIjoiSnNvblNjaGVtYVZhbGlkYXRvcjIwMTgifSwiY3JlZGVudGlhbFN1YmplY3QiOnsidXNlck5hbWUiOiJCb2IiLCJhZ2UiOjQyLCJlbWFpbCI6ImVtYWlsIn0sImNyZWRlbnRpYWxTdGF0dXMiOnsiaWQiOiJkaWQ6d29yazpNRFA4QXNGaEh6aHdVdkdOdVlrWDdUO2lkPTA2ZTEyNmQxLWZhNDQtNDg4Mi1hMjQzLTFlMzI2ZmJlMjFkYjt2ZXJzaW9uPTEuMCIsInR5cGUiOiJTdGF0dXNMaXN0MjAyMUVudHJ5Iiwic3RhdHVzUHVycG9zZSI6IlJldm9jYXRpb24iLCJzdGF0dXNMaXN0SW5kZXgiOjAsInN0YXR1c0xpc3RDcmVkZW50aWFsIjoiaHR0cHM6Ly9leGFtcGxlLmNvbS9jcmVkZW50aWFscy9zdGF0dXMvMyJ9LCJyZWZyZXNoU2VydmljZSI6eyJpZCI6Imh0dHBzOi8vZXhhbXBsZS5lZHUvcmVmcmVzaC8zNzMyIiwidHlwZSI6Ik1hbnVhbFJlZnJlc2hTZXJ2aWNlMjAxOCJ9fSwianRpIjoiaHR0cDovL2V4YW1wbGUuZWR1L2NyZWRlbnRpYWxzLzM3MzIifQ.HBxrn8Nu6y1RvUAU8XcwUDPOiiHhC1OgHN757lWai6i8P-pHL4TBzIDartYtrMiZUKpNx9Onb19sJYywtqFkpg"

        actor.attemptsTo(
            Post.to("/verification/credential").with {
                it.body(
                    """  
                    [{
                        "credential" : "$signedJwtCredential",
                        "verifications" : [
                          {
                            "verification" : {
                              "NotBeforeCheck" : {}
                            },
                            "parameter" : {
                              "DateTimeParameter" : {
                                "dateTime" : "2010-01-01T00:00:00Z"
                              }
                            }
                          },
                          {
                            "verification" : {
                              "ExpirationCheck" : {}
                            },
                            "parameter" : {
                              "DateTimeParameter" : {
                                "dateTime" : "2010-01-01T00:00:00Z"
                              }
                            }
                          }
                        ]
                      },
                      
                      
                      {
                        "credential" : "$signedJwtCredential",
                        "verifications" : [
                          {
                            "verification" : {
                              "AudienceCheck" : {}
                            },
                            "parameter" : {
                              "DidParameter" : {
                                "aud" : "did:prism:verifier"
                              }
                            }
                          },
                          {
                            "verification" : {
                              "IssuerIdentification" : {}
                            },
                            "parameter" : {
                              "DidParameter" : {
                                "aud" : "did:prism:4158858b5f0dac2e5047f2285989ee39a53deba76461c7af039654df3b59225c"
                              }
                            }
                          }
                        ]
                      },
                      
                      
                      {
                        "credential" : "$signedJwtCredential",
                        "verifications" : [
                          {
                            "verification" : {
                              "ExpirationCheck" : {}
                            },
                            "parameter" : {
                              "DateTimeParameter" : {
                                "dateTime" : "2010-01-13T00:00:00Z"
                              }
                            }
                          }
                        ]
                      }
                    ]""",
                )
                it.header(Header("apiKey", "pylnapbvyudwmfrt"))
            },
        )
        val vcVerificationResponses = SerenityRest.lastResponse().body().asString()
        actor.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
            Ensure.that(vcVerificationResponses).isEqualTo(
                """
                [   
                    {
                        "credential":"$signedJwtCredential",
                        "result":[
                            {
                                "verification":{ 
                                    "NotBeforeCheck":{}
                                },
                                "success":true
                            },
                            {
                                "verification":{ 
                                    "ExpirationCheck":{}
                                },
                                "success":true
                            }
                        ]
                    },
                    {
                        "credential":"$signedJwtCredential",
                        "result":[
                            {
                                "verification":{
                                    "AudienceCheck":{}
                                },
                                "success":true
                            },
                            {
                                "verification":{
                                    "IssuerIdentification":{}
                                },
                                "success":true
                            }
                        ]
                    },
                    {
                        "credential":"$signedJwtCredential",
                        "result":[
                            {
                                "verification":{
                                    "ExpirationCheck":{}
                                },
                                "success":false
                            }
                        ]
                    }
                ]
                """.trimIndent()
                    .replace("\\s+".toRegex(), ""),

            ),
        )
    }
}
