package io.iohk.atala.castor.apiserver.http.marshaller

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import io.iohk.atala.castor.openapi.api.DIDApiMarshaller
import io.iohk.atala.castor.openapi.model.{
  CreateDIDRequest,
  DeactivateDIDRequest,
  RecoverDIDRequest,
  UpdateDIDRequest,
  DIDResponseWithAsyncOutcome,
  DIDResponse,
  ErrorResponse
}
import spray.json.RootJsonFormat
import zio.*

object DIDApiMarshallerImpl extends JsonSupport {

  val layer: ULayer[DIDApiMarshaller] = ZLayer.succeed {
    // TODO: replace with actual implementation
    new DIDApiMarshaller {
      implicit def fromEntityUnmarshallerCreateDIDRequest: FromEntityUnmarshaller[CreateDIDRequest] = ???

      implicit def fromEntityUnmarshallerDeactivateDIDRequest: FromEntityUnmarshaller[DeactivateDIDRequest] = ???

      implicit def fromEntityUnmarshallerRecoverDIDRequest: FromEntityUnmarshaller[RecoverDIDRequest] = ???

      implicit def fromEntityUnmarshallerUpdateDIDRequest: FromEntityUnmarshaller[UpdateDIDRequest] = ???

      implicit def toEntityMarshallerDIDResponseWithAsyncOutcome: ToEntityMarshaller[DIDResponseWithAsyncOutcome] = ???

      implicit def toEntityMarshallerDIDResponse: ToEntityMarshaller[DIDResponse] = ???

      implicit def toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse] = ???
    }
  }

}
