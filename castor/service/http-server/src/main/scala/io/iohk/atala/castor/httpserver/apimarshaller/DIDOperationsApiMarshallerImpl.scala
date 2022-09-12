package io.iohk.atala.castor.httpserver.apimarshaller

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import io.iohk.atala.castor.openapi.api.DIDOperationsApiMarshaller
import io.iohk.atala.castor.openapi.model.{
  ErrorResponse,
  GetDIDOperationResponse,
  GetDIDOperationsByDIDRefResponseInner
}
import zio.*

object DIDOperationsApiMarshallerImpl {

  val layer: ULayer[DIDOperationsApiMarshaller] = ZLayer.succeed {
    // TODO: replace with actual implementation
    new DIDOperationsApiMarshaller {
      override implicit def toEntityMarshallerGetDIDOperationsByDIDRefResponseInnerarray
          : ToEntityMarshaller[Seq[GetDIDOperationsByDIDRefResponseInner]] = ???

      override implicit def toEntityMarshallerGetDIDOperationResponse: ToEntityMarshaller[GetDIDOperationResponse] = ???

      override implicit def toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse] = ???
    }
  }

}
