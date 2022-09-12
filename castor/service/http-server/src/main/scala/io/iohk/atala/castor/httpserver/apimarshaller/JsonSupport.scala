package io.iohk.atala.castor.httpserver.apimarshaller

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import io.iohk.atala.castor.openapi.model.{
  AuthenticationChallengeSubmissionRequest,
  AuthenticationChallengeSubmissionResponse,
  CreateAuthenticationChallengeRequest,
  CreateAuthenticationChallengeResponse,
  ErrorResponse
}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  given RootJsonFormat[AuthenticationChallengeSubmissionRequest] = jsonFormat3(
    AuthenticationChallengeSubmissionRequest.apply
  )
  given RootJsonFormat[CreateAuthenticationChallengeRequest] = jsonFormat3(CreateAuthenticationChallengeRequest.apply)
  given RootJsonFormat[AuthenticationChallengeSubmissionResponse] = jsonFormat2(
    AuthenticationChallengeSubmissionResponse.apply
  )
  given RootJsonFormat[CreateAuthenticationChallengeResponse] = jsonFormat2(CreateAuthenticationChallengeResponse.apply)
  given RootJsonFormat[ErrorResponse] = jsonFormat5(ErrorResponse.apply)

}
