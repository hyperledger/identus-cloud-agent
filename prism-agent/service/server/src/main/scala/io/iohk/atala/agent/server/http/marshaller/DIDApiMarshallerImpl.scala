package io.iohk.atala.agent.server.http.marshaller

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import io.iohk.atala.agent.openapi.api.DIDApiMarshaller
import io.iohk.atala.agent.openapi.model.{
  CreateDIDRequest,
  DeactivateDIDRequest,
  RecoverDIDRequest,
  UpdateDIDRequest,
  DIDOperationResponse,
  DIDResponse,
  ErrorResponse
}
import spray.json.RootJsonFormat
import zio.*

object DIDApiMarshallerImpl extends JsonSupport {

  val layer: ULayer[DIDApiMarshaller] = ZLayer.succeed {
    new DIDApiMarshaller {
      implicit def fromEntityUnmarshallerCreateDIDRequest: FromEntityUnmarshaller[CreateDIDRequest] =
        summon[RootJsonFormat[CreateDIDRequest]]

      implicit def fromEntityUnmarshallerDeactivateDIDRequest: FromEntityUnmarshaller[DeactivateDIDRequest] =
        summon[RootJsonFormat[DeactivateDIDRequest]]

      implicit def fromEntityUnmarshallerRecoverDIDRequest: FromEntityUnmarshaller[RecoverDIDRequest] =
        summon[RootJsonFormat[RecoverDIDRequest]]

      implicit def fromEntityUnmarshallerUpdateDIDRequest: FromEntityUnmarshaller[UpdateDIDRequest] =
        summon[RootJsonFormat[UpdateDIDRequest]]

      implicit def toEntityMarshallerDIDOperationResponse: ToEntityMarshaller[DIDOperationResponse] =
        summon[RootJsonFormat[DIDOperationResponse]]

      implicit def toEntityMarshallerDIDResponse: ToEntityMarshaller[DIDResponse] = summon[RootJsonFormat[DIDResponse]]

      implicit def toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse] =
        summon[RootJsonFormat[ErrorResponse]]

    }
  }

}
