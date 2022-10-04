package io.iohk.atala.agent.server.http.marshaller

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import io.iohk.atala.agent.openapi.api.DIDAuthenticationApiMarshaller
import io.iohk.atala.agent.openapi.model.*
import spray.json.RootJsonFormat
import zio.*

object DIDAuthenticationApiMarshallerImpl extends JsonSupport {

  val layer: ULayer[DIDAuthenticationApiMarshaller] = ZLayer.succeed {
    new DIDAuthenticationApiMarshaller {
      implicit def fromEntityUnmarshallerAuthenticationChallengeSubmissionRequest
          : FromEntityUnmarshaller[AuthenticationChallengeSubmissionRequest] =
        summon[RootJsonFormat[AuthenticationChallengeSubmissionRequest]]

      implicit def fromEntityUnmarshallerCreateAuthenticationChallengeRequest
          : FromEntityUnmarshaller[CreateAuthenticationChallengeRequest] =
        summon[RootJsonFormat[CreateAuthenticationChallengeRequest]]

      implicit def toEntityMarshallerAuthenticationChallengeSubmissionResponse
          : ToEntityMarshaller[AuthenticationChallengeSubmissionResponse] =
        summon[RootJsonFormat[AuthenticationChallengeSubmissionResponse]]

      implicit def toEntityMarshallerCreateAuthenticationChallengeResponse
          : ToEntityMarshaller[CreateAuthenticationChallengeResponse] =
        summon[RootJsonFormat[CreateAuthenticationChallengeResponse]]

      implicit def toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse] =
        summon[RootJsonFormat[ErrorResponse]]
    }
  }

}
