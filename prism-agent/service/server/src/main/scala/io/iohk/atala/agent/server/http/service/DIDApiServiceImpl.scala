package io.iohk.atala.agent.server.http.service

import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.castor.core.model.did.w3c.W3CModelHelper.*
import io.iohk.atala.castor.core.model.did.w3c.makeW3CResolver
import io.iohk.atala.agent.openapi.api.DIDApiService
import io.iohk.atala.agent.openapi.model.*
import io.iohk.atala.agent.server.http.model.{HttpServiceError, OASDomainModelHelper, OASErrorModelHelper}
import io.iohk.atala.castor.core.model.error.DIDOperationError
import zio.*
import io.iohk.atala.agent.server.http.model.OASModelPatches

class DIDApiServiceImpl(service: DIDService)(using runtime: Runtime[Any])
    extends DIDApiService,
      AkkaZioSupport,
      OASDomainModelHelper,
      OASErrorModelHelper {

  override def getDid(didRef: String)(implicit
      toEntityMarshallerDIDResponse: ToEntityMarshaller[DIDResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = makeW3CResolver(service)(didRef).mapError(HttpServiceError.DomainError.apply)
    onZioSuccess(result.mapBoth(_.toOAS, _.toOAS).either) {
      case Left(error)     => complete(error.status -> error)
      case Right(response) => getDid200(response)
    }
  }

  override def getDidRepresentation(didRef: String, accept: Option[String])(implicit
      toEntityMarshallerDIDResolutionResult: ToEntityMarshaller[OASModelPatches.DIDResolutionResult]
  ): Route = ??? // TODO

}

object DIDApiServiceImpl {
  val layer: URLayer[DIDService, DIDApiService] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
      svc <- ZIO.service[DIDService]
    } yield DIDApiServiceImpl(svc)(using rt)
  }
}
