package io.iohk.atala.castor.apiserver.http.service

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.castor.openapi.api.DIDApiService
import io.iohk.atala.castor.openapi.model.{
  CreateDIDRequest,
  DIDResponse,
  DIDResponseWithAsyncOutcome,
  DeactivateDIDRequest,
  ErrorResponse,
  RecoverDIDRequest,
  UpdateDIDRequest
}
import zio.*

// TODO: replace with actual implementation
class DIDApiServiceImpl(service: DIDService)(using runtime: Runtime[Any]) extends DIDApiService with AkkaZioSupport {

  override def createDid(createDIDRequest: CreateDIDRequest)(implicit
      toEntityMarshallerDIDResponseWithAsyncOutcome: ToEntityMarshaller[DIDResponseWithAsyncOutcome],
      toEntityMarshallerDIDResponse: ToEntityMarshaller[DIDResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def deactivateDID(didRef: String, deactivateDIDRequest: DeactivateDIDRequest)(implicit
      toEntityMarshallerDIDResponseWithAsyncOutcome: ToEntityMarshaller[DIDResponseWithAsyncOutcome],
      toEntityMarshallerDIDResponse: ToEntityMarshaller[DIDResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def getDid(didRef: String)(implicit
      toEntityMarshallerDIDResponse: ToEntityMarshaller[DIDResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def recoverDid(didRef: String, recoverDIDRequest: RecoverDIDRequest)(implicit
      toEntityMarshallerDIDResponseWithAsyncOutcome: ToEntityMarshaller[DIDResponseWithAsyncOutcome],
      toEntityMarshallerDIDResponse: ToEntityMarshaller[DIDResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def updateDid(didRef: String, updateDIDRequest: UpdateDIDRequest)(implicit
      toEntityMarshallerDIDResponseWithAsyncOutcome: ToEntityMarshaller[DIDResponseWithAsyncOutcome],
      toEntityMarshallerDIDResponse: ToEntityMarshaller[DIDResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

}

object DIDApiServiceImpl {
  val layer: URLayer[DIDService, DIDApiService] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
      svc <- ZIO.service[DIDService]
    } yield DIDApiServiceImpl(svc)(using rt)
  }
}
