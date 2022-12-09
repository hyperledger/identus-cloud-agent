package io.iohk.atala.agent.server.http.service

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import io.iohk.atala.agent.openapi.api._
import io.iohk.atala.agent.openapi.model._
import io.iohk.atala.agent.server.http.model.HttpServiceError
import io.iohk.atala.agent.server.http.model.OASDomainModelHelper
import io.iohk.atala.agent.server.http.model.OASErrorModelHelper
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.connect.core.service.ConnectionService
import io.iohk.atala.mercury.PeerDID
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation
import zio._
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.agent.walletapi.model.error.CreateManagedDIDError
import io.iohk.atala.agent.server.config.AgentConfig
import io.iohk.atala.agent.server.config.AppConfig

class ConnectionsManagementApiServiceImpl(
    connectionService: ConnectionService,
    managedDIDService: ManagedDIDService,
    agentConfig: AgentConfig
)(using
    runtime: zio.Runtime[Any]
) extends ConnectionsManagementApiService,
      AkkaZioSupport,
      OASDomainModelHelper,
      OASErrorModelHelper {

  override def createConnection(request: CreateConnectionRequest)(implicit
      toEntityMarshallerConnection: ToEntityMarshaller[Connection],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      pairwiseDid <- managedDIDService
        .createAndStorePeerDID(agentConfig.didCommServiceEndpointUrl)
      record <- connectionService
        .createConnectionInvitation(request.label, pairwiseDid.did)
        .mapError(HttpServiceError.DomainError[ConnectionServiceError].apply(_).toOAS)
    } yield record

    onZioSuccess(result.map(_.toOAS).either) {
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
        .mapError(HttpServiceError.DomainError[ConnectionServiceError].apply)
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
        .mapError(HttpServiceError.DomainError[ConnectionServiceError].apply)
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
        .mapError(HttpServiceError.DomainError[ConnectionServiceError].apply(_).toOAS)
      pairwiseDid <- managedDIDService
        .createAndStorePeerDID(agentConfig.didCommServiceEndpointUrl)
      record <- connectionService
        .acceptConnectionInvitation(record.id, pairwiseDid.did)
        .mapError(HttpServiceError.DomainError[ConnectionServiceError].apply(_).toOAS)
    } yield record

    onZioSuccess(result.map(_.map(_.toOAS)).either) {
      case Left(error)         => complete(error.status -> error)
      case Right(Some(result)) => acceptConnectionInvitation200(result)
      case Right(None) => acceptConnectionInvitation500(notFoundErrorResponse(Some("Connection record not found")))
    }
  }

  override def deleteConnection(connectionId: String): Route = ???
}

object ConnectionsManagementApiServiceImpl {
  val layer: URLayer[ConnectionService & ManagedDIDService & AppConfig, ConnectionsManagementApiService] =
    ZLayer.fromZIO {
      for {
        rt <- ZIO.runtime[Any]
        svc <- ZIO.service[ConnectionService]
        managedDIDService <- ZIO.service[ManagedDIDService]
        appConfig <- ZIO.service[AppConfig]
      } yield ConnectionsManagementApiServiceImpl(svc, managedDIDService, appConfig.agent)(using rt)
    }
}
