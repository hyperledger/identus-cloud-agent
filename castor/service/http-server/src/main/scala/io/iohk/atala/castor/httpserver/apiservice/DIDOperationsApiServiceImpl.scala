package io.iohk.atala.castor.httpserver.apiservice

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import io.iohk.atala.castor.core.service.DIDOperationService
import io.iohk.atala.castor.openapi.api.DIDOperationsApiService
import io.iohk.atala.castor.openapi.model.{
  ErrorResponse,
  GetDIDOperationResponse,
  GetDIDOperationsByDIDRefResponseInner
}
import zio.*

// TODO: replace with actual implementation
class DIDOperationsApiServiceImpl(service: DIDOperationService)(using runtime: Runtime[Any])
    extends DIDOperationsApiService
    with AkkaZioSupport {

  override def getDidOperation(didOperationRef: String)(implicit
      toEntityMarshallerGetDIDOperationResponse: ToEntityMarshaller[GetDIDOperationResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def getDidOperationsByDidRef(didRef: String)(implicit
      toEntityMarshallerGetDIDOperationsByDIDRefResponseInnerarray: ToEntityMarshaller[
        Seq[GetDIDOperationsByDIDRefResponseInner]
      ],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

}

object DIDOperationsApiServiceImpl {
  val layer: URLayer[DIDOperationService, DIDOperationsApiService] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
      svc <- ZIO.service[DIDOperationService]
    } yield DIDOperationsApiServiceImpl(svc)(using rt)
  }
}
