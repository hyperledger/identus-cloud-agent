package io.iohk.atala.castor.httpserver.api.service

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.castor.openapi.api.DIDApiService
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
import zio.*

// TODO: replace with actual implementation
class DIDApiServiceImpl(service: DIDService)(using runtime: Runtime[Any]) extends DIDApiService with AkkaZioSupport {

  override def createPublishedDid(createDIDOperation: CreateDIDOperation)(implicit
      toEntityMarshallerPublishDIDResponse: ToEntityMarshaller[PublishDIDResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def createUnpublishedDid(createDIDWithProof: CreateDIDWithProof)(implicit
      toEntityMarshallerPublishDIDResponse: ToEntityMarshaller[PublishDIDResponse]
  ): Route = ???

  override def deactivateDID(didRef: String, deactivateDIDOperation: DeactivateDIDOperation)(implicit
      toEntityMarshallerDeactivateDIDResponse: ToEntityMarshaller[DeactivateDIDResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def getDID(didRef: String)(implicit
      toEntityMarshallerResolveDIDResponse: ToEntityMarshaller[ResolveDIDResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def recoverPublishedDid(didRef: String, recoverDIDOperation: RecoverDIDOperation)(implicit
      toEntityMarshallerResolveDIDResponse: ToEntityMarshaller[ResolveDIDResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def recoverUnpublishedDid(didRef: String, recoverDIDWithProof: RecoverDIDWithProof)(implicit
      toEntityMarshallerResolveDIDResponse: ToEntityMarshaller[ResolveDIDResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def updatePublishedDid(didRef: String, updateDIDOperation: UpdateDIDOperation)(implicit
      toEntityMarshallerResolveDIDResponse: ToEntityMarshaller[ResolveDIDResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def updateUnpublishedDid(didRef: String, updateDIDWithProof: UpdateDIDWithProof)(implicit
      toEntityMarshallerPublishDIDResponse: ToEntityMarshaller[PublishDIDResponse],
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
