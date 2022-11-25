package io.iohk.atala.agent.server.http.service

import io.iohk.atala.agent.openapi.api._
import io.iohk.atala.agent.openapi.model._
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import zio._
import io.iohk.atala.connect.core.service.ConnectionService
import io.iohk.atala.agent.server.http.model.OASDomainModelHelper
import io.iohk.atala.agent.server.http.model.OASErrorModelHelper
import io.iohk.atala.agent.server.http.model.HttpServiceError
import io.iohk.atala.connect.core.model.error.ConnectionError
import io.iohk.atala.mercury.PeerDID
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation

class ConnectionsManagementApiServiceImpl(connectionService: ConnectionService)(using runtime: zio.Runtime[Any])
    extends ConnectionsManagementApiService,
      AkkaZioSupport,
      OASDomainModelHelper,
      OASErrorModelHelper {

  override def createConnection(request: CreateConnectionRequest)(implicit
      toEntityMarshallerConnection: ToEntityMarshaller[Connection],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      record <- connectionService
        .createConnectionInvitation(request.label)
        .mapError(HttpServiceError.DomainError[ConnectionError].apply)
    } yield record

    onZioSuccess(result.mapBoth(_.toOAS, _.toOAS).either) {
      case Left(error)   => complete(error.status -> error)
      case Right(result) => createConnection201(result)
    }
  }

  override def getConnections()(implicit
      toEntityMarshallerConnectionCollection: ToEntityMarshaller[ConnectionCollection],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      outcome <- connectionService
        .getConnectionRecords()
        .mapError(HttpServiceError.DomainError[ConnectionError].apply)
    } yield outcome

    onZioSuccess(result.mapBoth(_.toOAS, _.map(_.toOAS)).either) {
      case Left(error) => complete(error.status -> error)
      case Right(result) =>
        getConnections200(
          ConnectionCollection(
            self = "/collections",
            kind = "Collection",
            contents = result
          )
        )
    }
  }

  override def getConnection(connectionId: String)(implicit
      toEntityMarshallerConnection: ToEntityMarshaller[Connection],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      recordId <- connectionId.toUUID
      outcome <- connectionService
        .getConnectionRecord(recordId)
        .mapError(HttpServiceError.DomainError[ConnectionError].apply)
    } yield outcome

    onZioSuccess(result.mapBoth(_.toOAS, _.map(_.toOAS)).either) {
      case Left(error)         => complete(error.status -> error)
      case Right(Some(result)) => getConnection200(result)
      case Right(None)         => getConnection404(notFoundErrorResponse(Some("Connection record not found")))
    }
  }

  override def acceptConnectionInvitation(request: AcceptConnectionInvitationRequest)(implicit
      toEntityMarshallerConnection: ToEntityMarshaller[Connection],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      record <- connectionService
        .receiveConnectionInvitation(request.invitation)
        .mapError(HttpServiceError.DomainError[ConnectionError].apply)
      record <- connectionService
        .acceptConnectionInvitation(record.id)
        .mapError(HttpServiceError.DomainError[ConnectionError].apply)
    } yield record

    onZioSuccess(result.mapBoth(_.toOAS, _.map(_.toOAS)).either) {
      case Left(error)         => complete(error.status -> error)
      case Right(Some(result)) => acceptConnectionInvitation200(result)
      case Right(None) => acceptConnectionInvitation500(notFoundErrorResponse(Some("Connection record not found")))
    }
  }

  override def deleteConnection(connectionId: String): Route = ???
}

object ConnectionsManagementApiServiceImpl {
  val layer: URLayer[ConnectionService, ConnectionsManagementApiService] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
      svc <- ZIO.service[ConnectionService]
    } yield ConnectionsManagementApiServiceImpl(svc)(using rt)
  }
}
