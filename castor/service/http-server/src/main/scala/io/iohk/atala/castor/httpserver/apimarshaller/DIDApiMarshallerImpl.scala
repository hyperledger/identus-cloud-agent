package io.iohk.atala.castor.httpserver.apimarshaller

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import io.iohk.atala.castor.openapi.api.DIDApiMarshaller
import io.iohk.atala.castor.openapi.model.{
  CreateDIDOperation,
  CreateDIDWithProof,
  DeactivateDIDOperation,
  DeactivateDIDResponse,
  ErrorResponse,
  PublishDIDResponse,
  RecoverDIDOperation,
  RecoverDIDWithProof,
  ResolveDIDResponse,
  UpdateDIDOperation,
  UpdateDIDWithProof
}
import spray.json.RootJsonFormat
import zio.*

object DIDApiMarshallerImpl extends JsonSupport {

  val layer: ULayer[DIDApiMarshaller] = ZLayer.succeed {
    // TODO: replace with actual implementation
    new DIDApiMarshaller {
      implicit def fromEntityUnmarshallerCreateDIDWithProof: FromEntityUnmarshaller[CreateDIDWithProof] =
        ???

      implicit def fromEntityUnmarshallerCreateDIDOperation: FromEntityUnmarshaller[CreateDIDOperation] =
        ???

      implicit def fromEntityUnmarshallerRecoverDIDWithProof: FromEntityUnmarshaller[RecoverDIDWithProof] =
        ???

      implicit def fromEntityUnmarshallerDeactivateDIDOperation: FromEntityUnmarshaller[DeactivateDIDOperation] =
        ???

      implicit def fromEntityUnmarshallerUpdateDIDWithProof: FromEntityUnmarshaller[UpdateDIDWithProof] =
        ???

      implicit def fromEntityUnmarshallerRecoverDIDOperation: FromEntityUnmarshaller[RecoverDIDOperation] =
        ???

      implicit def fromEntityUnmarshallerUpdateDIDOperation: FromEntityUnmarshaller[UpdateDIDOperation] =
        ???

      implicit def toEntityMarshallerDeactivateDIDResponse: ToEntityMarshaller[DeactivateDIDResponse] =
        ???

      implicit def toEntityMarshallerResolveDIDResponse: ToEntityMarshaller[ResolveDIDResponse] =
        ???

      implicit def toEntityMarshallerPublishDIDResponse: ToEntityMarshaller[PublishDIDResponse] =
        ???

      implicit def toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse] =
        ???
    }
  }

}
