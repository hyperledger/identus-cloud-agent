package io.iohk.atala.agent.server.http.service

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import io.iohk.atala.agent.custodian.service.ManagedDIDService
import zio.*
import io.iohk.atala.agent.openapi.api.DIDRegistrarApiService
import io.iohk.atala.agent.openapi.model.{
  CreateManagedDIDResponse,
  CreateManagedDidRequest,
  CreateDIDRequest,
  DIDOperationResponse,
  DIDResponse,
  ErrorResponse
}
import io.iohk.atala.agent.server.http.model.{OASDomainModelHelper, OASErrorModelHelper}

class DIDRegistrarApiServiceImpl(service: ManagedDIDService)(using runtime: Runtime[Any])
    extends DIDRegistrarApiService,
      AkkaZioSupport,
      OASDomainModelHelper,
      OASErrorModelHelper {

  // TODO: implement
  override def createManagedDid(createManagedDidRequest: CreateManagedDidRequest)(implicit
      toEntityMarshallerCreateManagedDIDResponse: ToEntityMarshaller[CreateManagedDIDResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  // TODO: implement
  override def publishManagedDid(didRef: String)(implicit
      toEntityMarshallerDIDOperationResponse: ToEntityMarshaller[DIDOperationResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

}

object DIDRegistrarApiServiceImpl {
  val layer: URLayer[ManagedDIDService, DIDRegistrarApiService] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
      svc <- ZIO.service[ManagedDIDService]
    } yield DIDRegistrarApiServiceImpl(svc)(using rt)
  }
}
