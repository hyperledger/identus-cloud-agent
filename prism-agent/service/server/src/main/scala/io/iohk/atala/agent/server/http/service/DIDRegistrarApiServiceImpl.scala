package io.iohk.atala.agent.server.http.service

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import zio.*
import io.iohk.atala.agent.openapi.api.DIDRegistrarApiService
import io.iohk.atala.agent.openapi.model.*
import io.iohk.atala.agent.server.http.model.{HttpServiceError, OASDomainModelHelper, OASErrorModelHelper}
import io.iohk.atala.agent.walletapi.model.error.{PublishManagedDIDError, UpdateManagedDIDError}
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.shared.utils.Traverse.*

class DIDRegistrarApiServiceImpl(service: ManagedDIDService)(using runtime: Runtime[Any])
    extends DIDRegistrarApiService,
      AkkaZioSupport,
      OASDomainModelHelper,
      OASErrorModelHelper {

  override def createManagedDid(createManagedDidRequest: CreateManagedDidRequest)(implicit
      toEntityMarshallerCreateManagedDIDResponse: ToEntityMarshaller[CreateManagedDIDResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      didTemplate <- ZIO
        .fromEither(createManagedDidRequest.documentTemplate.toDomain)
        .mapError(HttpServiceError.InvalidPayload.apply)
      longFormDID <- service
        .createAndStoreDID(didTemplate)
        .mapError(HttpServiceError.DomainError.apply)
    } yield CreateManagedDIDResponse(
      longFormDid = longFormDID.toString
    )

    onZioSuccess(result.mapError(_.toOAS).either) {
      case Left(error)   => complete(error.status -> error)
      case Right(result) => createManagedDid200(result)
    }
  }

  override def deactivateManagedDid(didRef: String)(implicit
      toEntityMarshallerDIDOperationResponse: ToEntityMarshaller[DIDOperationResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      prismDID <- ZIO.fromEither(PrismDID.fromString(didRef)).mapError(HttpServiceError.InvalidPayload.apply)
      outcome <- service
        .deactivateManagedDID(prismDID.asCanonical)
        .mapError(HttpServiceError.DomainError[UpdateManagedDIDError].apply)
    } yield outcome

    onZioSuccess(result.mapBoth(_.toOAS, _.toOAS).either) {
      case Left(error)   => complete(error.status -> error)
      case Right(result) => deactivateManagedDid202(result)
    }
  }

  override def listManagedDid()(implicit
      toEntityMarshallerManagedDIDCollection: ToEntityMarshaller[ManagedDIDCollection],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    // TODO: proper pagination
    val result = for {
      dids <- service.listManagedDID
        .map(_.map(_.toOAS))
        .mapError(HttpServiceError.DomainError.apply)
    } yield ???

    onZioSuccess(result.mapError(_.toOAS).either) {
      case Left(error)   => complete(error.status -> error)
      case Right(result) => listManagedDid200(result)
    }
  }

  override def publishManagedDid(didRef: String)(implicit
      toEntityMarshallerDIDOperationResponse: ToEntityMarshaller[DIDOperationResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      prismDID <- ZIO.fromEither(PrismDID.fromString(didRef)).mapError(HttpServiceError.InvalidPayload.apply)
      outcome <- service
        .publishStoredDID(prismDID.asCanonical)
        .mapError(HttpServiceError.DomainError[PublishManagedDIDError].apply)
    } yield outcome

    onZioSuccess(result.mapBoth(_.toOAS, _.toOAS).either) {
      case Left(error)   => complete(error.status -> error)
      case Right(result) => publishManagedDid202(result)
    }
  }

  def updateManagedDid(didRef: String, updateManagedDIDRequest: UpdateManagedDIDRequest)(implicit
      toEntityMarshallerDIDOperationResponse: ToEntityMarshaller[DIDOperationResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      prismDID <- ZIO.fromEither(PrismDID.fromString(didRef)).mapError(HttpServiceError.InvalidPayload.apply)
      actions <- ZIO
        .fromEither(updateManagedDIDRequest.actions.traverse(_.toDomain))
        .mapError(HttpServiceError.InvalidPayload.apply)
      outcome <- service
        .updateManagedDID(prismDID.asCanonical, actions)
        .mapError(HttpServiceError.DomainError[UpdateManagedDIDError].apply)
    } yield outcome

    onZioSuccess(result.mapBoth(_.toOAS, _.toOAS).either) {
      case Left(error)   => complete(error.status -> error)
      case Right(result) => updateManagedDid202(result)
    }
  }

}

object DIDRegistrarApiServiceImpl {
  val layer: URLayer[ManagedDIDService, DIDRegistrarApiService] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
      svc <- ZIO.service[ManagedDIDService]
    } yield DIDRegistrarApiServiceImpl(svc)(using rt)
  }
}
