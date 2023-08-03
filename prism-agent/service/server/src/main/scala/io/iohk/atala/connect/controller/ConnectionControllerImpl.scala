package io.iohk.atala.connect.controller

import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.connect.controller.ConnectionController.toHttpError
import io.iohk.atala.connect.controller.http.{
  AcceptConnectionInvitationRequest,
  Connection,
  ConnectionsPage,
  CreateConnectionRequest
}
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.connect.core.service.ConnectionService
import zio.*

import java.util.UUID

class ConnectionControllerImpl(
    service: ConnectionService,
    managedDIDService: ManagedDIDService,
    appConfig: AppConfig
) extends ConnectionController {

  override def createConnection(request: CreateConnectionRequest)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, Connection] = {
    val result = for {
      pairwiseDid <- managedDIDService.createAndStorePeerDID(appConfig.agent.didCommServiceEndpointUrl)
      connection <- service.createConnectionInvitation(request.label, pairwiseDid.did)
    } yield Connection.fromDomain(connection)

    result.mapError(toHttpError)
  }

  override def getConnection(
      connectionId: UUID
  )(implicit rc: RequestContext): IO[ErrorResponse, Connection] = {
    val result = for {
      maybeConnection <- service.getConnectionRecord(connectionId)
      connection <- ZIO
        .fromOption(maybeConnection)
        .mapError(_ => ConnectionServiceError.RecordIdNotFound(connectionId))
    } yield Connection.fromDomain(connection)

    result.mapError(toHttpError)
  }

  object CustomMetricsAspect {
    import java.util.concurrent.TimeUnit
    import zio.metrics.*
    def attachDurationGaugeMetric(name: String): ZIOAspect[Nothing, Any, Nothing, Any, Nothing, Any] =
      new ZIOAspect[Nothing, Any, Nothing, Any, Nothing, Any] {
        override def apply[R, E, A](
            zio: ZIO[R, E, A]
        )(implicit trace: Trace): ZIO[R, E, A] =
          def currTime = Clock.currentTime(TimeUnit.MILLISECONDS)

          for {
            timeBefore <- currTime
            res <- zio
            timeAfter <- currTime
            _ <- ZIO.succeed((timeAfter - timeBefore).toDouble) @@ Metric.gauge(name)
          } yield res
      }
  }

  override def getConnections(
      paginationInput: PaginationInput,
      thid: Option[String]
  )(implicit rc: RequestContext): IO[ErrorResponse, ConnectionsPage] = {

    val deleyEffect = ZIO.sleep(Duration.fromMillis(2000)).map(_ => 5)
    import zio.metrics.*

    val successZIO = ZIO.succeed(1)
    val errZIO = ZIO.fail("err")

    val succCounter = Metric.counter("success_counter").fromConst(1)
    val failCounter = Metric.counter("fail_counter").fromConst(1)

    val result = for {
      _ <- successZIO @@ succCounter.trackError
      connections <- thid match
        case None       => service.getConnectionRecords()
        case Some(thid) => service.getConnectionRecordByThreadId(thid).map(_.toSeq)
    } yield ConnectionsPage(contents = connections.map(Connection.fromDomain))

    result.mapError(toHttpError)
  }

  override def acceptConnectionInvitation(
      request: AcceptConnectionInvitationRequest
  )(implicit rc: RequestContext): IO[ErrorResponse, Connection] = {
    val result = for {
      record <- service.receiveConnectionInvitation(request.invitation)
      pairwiseDid <- managedDIDService.createAndStorePeerDID(appConfig.agent.didCommServiceEndpointUrl)
      connection <- service.acceptConnectionInvitation(record.id, pairwiseDid.did)
    } yield Connection.fromDomain(connection)

    result.mapError(toHttpError)
  }
}

object ConnectionControllerImpl {
  val layer: URLayer[ConnectionService & ManagedDIDService & AppConfig, ConnectionController] =
    ZLayer.fromFunction(ConnectionControllerImpl(_, _, _))
}
