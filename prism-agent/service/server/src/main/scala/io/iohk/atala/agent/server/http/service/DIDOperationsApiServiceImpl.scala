package io.iohk.atala.agent.server.http.service

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import io.iohk.atala.agent.core.service.DIDOperationService
import io.iohk.atala.agent.openapi.api.DIDOperationsApiService
import io.iohk.atala.agent.openapi.model.{DidOperation, DidOperationStatus, DidOperationType, ErrorResponse}
import zio.*

// TODO: replace with actual implementation
class DIDOperationsApiServiceImpl(service: DIDOperationService)(using runtime: Runtime[Any])
    extends DIDOperationsApiService
    with AkkaZioSupport {

  private val mockDIDOperation = DidOperation(
    id = "123",
    didRef = "did:prism:1:abcdef123456",
    `type` = "PUBLISH",
    status = "EXECUTED"
  )

  override def getDidOperation(didOperationRef: String)(implicit
      toEntityMarshallerDidOperation: ToEntityMarshaller[DidOperation],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    onZioSuccess(ZIO.unit) { _ =>
      getDidOperation200(mockDIDOperation)
    }
  }

  override def getDidOperationsByDidRef(didRef: String)(implicit
      toEntityMarshallerDidOperationarray: ToEntityMarshaller[Seq[DidOperation]],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    onZioSuccess(ZIO.unit) { _ =>
      getDidOperationsByDidRef200(Seq.fill(4)(mockDIDOperation))
    }
  }

}

object DIDOperationsApiServiceImpl {
  val layer: URLayer[DIDOperationService, DIDOperationsApiService] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
      svc <- ZIO.service[DIDOperationService]
    } yield DIDOperationsApiServiceImpl(svc)(using rt)
  }
}
