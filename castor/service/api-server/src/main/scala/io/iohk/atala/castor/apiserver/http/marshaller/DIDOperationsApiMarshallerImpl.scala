package io.iohk.atala.castor.apiserver.http.marshaller

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import io.iohk.atala.castor.openapi.api.DIDOperationsApiMarshaller
import io.iohk.atala.castor.openapi.model.{DidOperation, ErrorResponse}
import zio.*

object DIDOperationsApiMarshallerImpl {

  val layer: ULayer[DIDOperationsApiMarshaller] = ZLayer.succeed {
    // TODO: replace with actual implementation
    new DIDOperationsApiMarshaller {
      implicit def toEntityMarshallerDidOperation: ToEntityMarshaller[DidOperation] = ???

      implicit def toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse] = ???

      implicit def toEntityMarshallerDidOperationarray: ToEntityMarshaller[Seq[DidOperation]] = ???
    }
  }

}
