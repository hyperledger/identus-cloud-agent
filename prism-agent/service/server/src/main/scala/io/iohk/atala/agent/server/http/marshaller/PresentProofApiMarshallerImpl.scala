package io.iohk.atala.agent.server.http.marshaller

import zio.*
import spray.json.RootJsonFormat
import io.iohk.atala.pollux.core.service.CredentialService
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import io.iohk.atala.agent.openapi.model.*
import io.iohk.atala.agent.openapi.api.PresentProofApiMarshaller

// FIXME CLEANUP
object PresentProofApiMarshallerImpl extends JsonSupport {
  val layer: ULayer[PresentProofApiMarshaller] = ZLayer.succeed {
    new PresentProofApiMarshaller {

      implicit def fromEntityUnmarshallerRequestPresentationInput: FromEntityUnmarshaller[RequestPresentationInput] =
        summon[RootJsonFormat[RequestPresentationInput]]
      // implicit def fromEntityUnmarshallerSendPresentationInput: FromEntityUnmarshaller[SendPresentationInput] =
      //   summon[RootJsonFormat[SendPresentationInput]]
      implicit def toEntityMarshallerPresentationStatus: ToEntityMarshaller[PresentationStatus] =
        summon[RootJsonFormat[PresentationStatus]]
      implicit def toEntityMarshallerRequestPresentationOutput: ToEntityMarshaller[RequestPresentationOutput] =
        summon[RootJsonFormat[RequestPresentationOutput]]

      implicit def fromEntityUnmarshallerUpdatePresentationRequest: FromEntityUnmarshaller[UpdatePresentationRequest] =
        summon[RootJsonFormat[UpdatePresentationRequest]]

      implicit def toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse] =
        summon[RootJsonFormat[ErrorResponse]]

    }
  }
}
