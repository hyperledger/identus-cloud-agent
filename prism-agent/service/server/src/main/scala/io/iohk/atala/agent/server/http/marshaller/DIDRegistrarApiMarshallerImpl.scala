package io.iohk.atala.agent.server.http.marshaller

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import io.iohk.atala.agent.openapi.api.DIDRegistrarApiMarshaller
import io.iohk.atala.agent.openapi.model.{
  CreateManagedDIDResponse,
  CreateManagedDidRequest,
  DIDOperationResponse,
  ErrorResponse,
  ManagedDIDPage,
  UpdateManagedDIDRequest
}
import spray.json.RootJsonFormat
import zio.*
import io.iohk.atala.agent.openapi.model.ManagedDID

object DIDRegistrarApiMarshallerImpl extends JsonSupport {

  val layer: ULayer[DIDRegistrarApiMarshaller] = ZLayer.succeed {
    new DIDRegistrarApiMarshaller {
      override implicit def fromEntityUnmarshallerCreateManagedDidRequest
          : FromEntityUnmarshaller[CreateManagedDidRequest] = summon[RootJsonFormat[CreateManagedDidRequest]]

      override implicit def fromEntityUnmarshallerUpdateManagedDIDRequest
          : FromEntityUnmarshaller[UpdateManagedDIDRequest] = summon[RootJsonFormat[UpdateManagedDIDRequest]]

      override implicit def toEntityMarshallerDIDOperationResponse: ToEntityMarshaller[DIDOperationResponse] =
        summon[RootJsonFormat[DIDOperationResponse]]

      override implicit def toEntityMarshallerManagedDID: ToEntityMarshaller[ManagedDID] =
        summon[RootJsonFormat[ManagedDID]]

      override implicit def toEntityMarshallerCreateManagedDIDResponse: ToEntityMarshaller[CreateManagedDIDResponse] =
        summon[RootJsonFormat[CreateManagedDIDResponse]]

      override implicit def toEntityMarshallerManagedDIDPage: ToEntityMarshaller[ManagedDIDPage] =
        summon[RootJsonFormat[ManagedDIDPage]]

      override implicit def toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse] =
        summon[RootJsonFormat[ErrorResponse]]
    }
  }

}
