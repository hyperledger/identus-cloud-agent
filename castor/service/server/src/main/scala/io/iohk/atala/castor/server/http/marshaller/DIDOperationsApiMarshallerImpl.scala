package io.iohk.atala.castor.server.http.marshaller

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import io.iohk.atala.castor.openapi.api.DIDOperationsApiMarshaller
import io.iohk.atala.castor.openapi.model.{DidOperation, ErrorResponse}
import spray.json.RootJsonFormat
import zio.*

object DIDOperationsApiMarshallerImpl extends JsonSupport {

  val layer: ULayer[DIDOperationsApiMarshaller] = ZLayer.succeed {
    new DIDOperationsApiMarshaller {
      implicit def toEntityMarshallerDidOperation: ToEntityMarshaller[DidOperation] =
        summon[RootJsonFormat[DidOperation]]

      implicit def toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse] =
        summon[RootJsonFormat[ErrorResponse]]

      implicit def toEntityMarshallerDidOperationarray: ToEntityMarshaller[Seq[DidOperation]] =
        summon[RootJsonFormat[Seq[DidOperation]]]
    }
  }

}
