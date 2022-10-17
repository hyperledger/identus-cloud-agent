package io.iohk.atala.agent.server.http.service

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import io.iohk.atala.agent.custodian.service.CustodialDIDService
import zio.*
import io.iohk.atala.agent.openapi.api.DIDRegistrarApiService
import io.iohk.atala.agent.openapi.model.{
  CreateCustodialDidRequest,
  CreateCustodialUnpublishedDIDResponse,
  CreateDIDRequest,
  DIDOperationResponse,
  DIDResponse,
  ErrorResponse
}
import io.iohk.atala.agent.server.http.model.{OASDomainModelHelper, OASErrorModelHelper}

class DIDRegistrarApiServiceImpl(service: CustodialDIDService)(using runtime: Runtime[Any])
    extends DIDRegistrarApiService,
      AkkaZioSupport,
      OASDomainModelHelper,
      OASErrorModelHelper {

  // TODO: implement
  override def createCustodialDid(createCustodialDidRequest: CreateCustodialDidRequest)(implicit
      toEntityMarshallerDIDOperationResponse: ToEntityMarshaller[DIDOperationResponse],
      toEntityMarshallerCreateCustodialUnpublishedDIDResponse: ToEntityMarshaller[
        CreateCustodialUnpublishedDIDResponse
      ],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

}

object DIDRegistrarApiServiceImpl {
  val layer: URLayer[CustodialDIDService, DIDRegistrarApiService] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
      svc <- ZIO.service[CustodialDIDService]
    } yield DIDRegistrarApiServiceImpl(svc)(using rt)
  }
}
