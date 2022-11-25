package io.iohk.atala.agent.server.http.marshaller

import zio.*
import spray.json.RootJsonFormat
import io.iohk.atala.pollux.core.service.CredentialService
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import io.iohk.atala.agent.openapi.model.*
import io.iohk.atala.agent.openapi.api.PresentProofApiMarshaller

object PresentProofApiMarshallerImpl extends JsonSupport {
  val layer: ULayer[PresentProofApiMarshaller] = ZLayer.succeed {
    new PresentProofApiMarshaller {

      implicit def fromEntityUnmarshallerRequestPresentationInput: FromEntityUnmarshaller[RequestPresentationInput] =
        summon[RootJsonFormat[RequestPresentationInput]]

      implicit def toEntityMarshallerPresentationStatusarray: ToEntityMarshaller[Seq[PresentationStatus]] =
        summon[RootJsonFormat[Seq[PresentationStatus]]]
      implicit def toEntityMarshallerRequestPresentationOutput: ToEntityMarshaller[RequestPresentationOutput] =
        summon[RootJsonFormat[RequestPresentationOutput]]

      implicit def fromEntityUnmarshallerUpdatePresentationRequest: FromEntityUnmarshaller[UpdatePresentationRequest] =
        summon[RootJsonFormat[UpdatePresentationRequest]]

      implicit def toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse] =
        summon[RootJsonFormat[ErrorResponse]]

    }
  }
}
