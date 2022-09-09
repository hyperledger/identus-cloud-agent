package io.iohk.atala.castor.httpserver.apimarshaller

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import io.iohk.atala.castor.openapi.api.DIDAuthenticationApiMarshaller
import io.iohk.atala.castor.openapi.model.*
import spray.json.RootJsonFormat
import zio.*

object DIDAuthenticationApiMarshallerImpl extends JsonSupport {

  val layer: ULayer[DIDAuthenticationApiMarshaller] = ZLayer.succeed {
    new DIDAuthenticationApiMarshaller {
      implicit def fromEntityUnmarshallerAuthenticationChallengeSubmissionRequest
          : FromEntityUnmarshaller[AuthenticationChallengeSubmissionRequest] =
        implicitly[RootJsonFormat[AuthenticationChallengeSubmissionRequest]]

      implicit def fromEntityUnmarshallerCreateAuthenticationChallengeRequest
          : FromEntityUnmarshaller[CreateAuthenticationChallengeRequest] =
        implicitly[RootJsonFormat[CreateAuthenticationChallengeRequest]]

      implicit def toEntityMarshallerAuthenticationChallengeSubmissionResponse
          : ToEntityMarshaller[AuthenticationChallengeSubmissionResponse] =
        implicitly[RootJsonFormat[AuthenticationChallengeSubmissionResponse]]

      implicit def toEntityMarshallerCreateAuthenticationChallengeResponse
          : ToEntityMarshaller[CreateAuthenticationChallengeResponse] =
        implicitly[RootJsonFormat[CreateAuthenticationChallengeResponse]]

      implicit def toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse] =
        implicitly[RootJsonFormat[ErrorResponse]]
    }
  }

}
