package io.iohk.atala.agent.server.jobs

import zio._
import io.iohk.atala.connect.core.service.ConnectionService
import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.ConnectionRecord._
import io.iohk.atala.mercury.DidComm
import io.iohk.atala.mercury.MediaTypes
import io.iohk.atala.mercury.MessagingService
import io.iohk.atala.mercury.HttpClient
import io.iohk.atala.mercury.model._
import io.iohk.atala.mercury.model.error._
import io.iohk.atala.mercury.protocol.issuecredential._
import io.iohk.atala.resolvers.DIDResolver
import java.io.IOException
import io.iohk.atala.connect.core.model.error.ConnectionServiceError

object ConnectBackgroundJobs {

  val didCommExchanges = {
    for {
      connectionService <- ZIO.service[ConnectionService]
      records <- connectionService
        .getConnectionRecords()
        .mapError(err => Throwable(s"Error occured while getting connection records: $err"))
      _ <- ZIO.foreach(records)(performExchange)
    } yield ()
  }

  private[this] def performExchange(
      record: ConnectionRecord
  ): URIO[DidComm & DIDResolver & HttpClient & ConnectionService, Unit] = {
    import Role._
    import ProtocolState._
    val exchange = record match {
      case ConnectionRecord(id, _, _, _, _, Invitee, ConnectionRequestPending, _, Some(request), _) =>
        for {
          didComm <- ZIO.service[DidComm]
          _ <- MessagingService.send(request.makeMessage)
          connectionService <- ZIO.service[ConnectionService]
          _ <- connectionService.markConnectionRequestSent(id)
        } yield ()

      case ConnectionRecord(id, _, _, _, _, Inviter, ConnectionResponsePending, _, _, Some(response)) =>
        for {
          didComm <- ZIO.service[DidComm]
          _ <- MessagingService.send(response.makeMessage)
          connectionService <- ZIO.service[ConnectionService]
          _ <- connectionService.markConnectionResponseSent(id)
        } yield ()

      case _ => ZIO.unit
    }

    exchange
      .catchAll {
        case ex: MercuryException =>
          ZIO.logErrorCause(s"DIDComm communication error processing record: ${record.id}", Cause.fail(ex))
        case ex: ConnectionServiceError =>
          ZIO.logErrorCause(s"Connection service error processing record: ${record.id} ", Cause.fail(ex))
      }
      .catchAllDefect { case throwable =>
        ZIO.logErrorCause(s"Conection protocol defect processing record: ${record.id}", Cause.fail(throwable))
      }
  }

}
