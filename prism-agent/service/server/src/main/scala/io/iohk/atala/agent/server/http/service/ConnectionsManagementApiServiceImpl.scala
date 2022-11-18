package io.iohk.atala.agent.server.http.service

import io.iohk.atala.agent.openapi.api._
import io.iohk.atala.agent.openapi.model._
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import zio._
import akka.http.scaladsl.server.Route

class ConnectionsManagementApiServiceImpl(using runtime: zio.Runtime[Any])
    extends ConnectionsManagementApiService,
      AkkaZioSupport {
  override def createConnection(createConnectionRequest: CreateConnectionRequest)(implicit
      toEntityMarshallerConnection: ToEntityMarshaller[Connection]
  ): Route = ???

  override def deleteConnection(connectionId: String): Route = ???

  override def getConnection(connectionId: String)(implicit
      toEntityMarshallerConnection: ToEntityMarshaller[Connection]
  ): Route = ???

  override def getConnections()(implicit
      toEntityMarshallerConnectionCollection: ToEntityMarshaller[ConnectionCollection]
  ): Route = ???

  override def receiveConnectionInvitation(receiveConnectionInvitationRequest: ReceiveConnectionInvitationRequest)(
      implicit toEntityMarshallerConnection: ToEntityMarshaller[Connection]
  ): Route = ???
}

object ConnectionsManagementApiServiceImpl {
  val layer: ULayer[ConnectionsManagementApiService] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
    } yield ConnectionsManagementApiServiceImpl(using rt)
  }
}
