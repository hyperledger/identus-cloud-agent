package io.iohk.atala.agent.server.jobs

import zio._
import io.iohk.atala.connect.core.service.ConnectionService
import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.mercury.DidComm
import io.iohk.atala.connect.core.model.ConnectionRecord._
import io.iohk.atala.agent.server.jobs.MercuryUtils.sendMessage
import io.iohk.atala.mercury.DidComm
import io.iohk.atala.mercury.MediaTypes
import io.iohk.atala.mercury.model._
import io.iohk.atala.mercury.model.error._
import io.iohk.atala.mercury.protocol.issuecredential._
import java.io.IOException

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
  ): ZIO[DidComm & ConnectionService, Throwable, Unit] = {
    val exchange = record match {
      case ConnectionRecord(
            id,
            _,
            _,
            _,
            _,
            Role.Invitee,
            ProtocolState.ConnectionRequestPending,
            _,
            Some(request),
            _
          ) =>
        for {
          didComm <- ZIO.service[DidComm]
          _ <- sendMessage(request.makeMessage)
          connectionService <- ZIO.service[ConnectionService]
          _ <- connectionService.markConnectionRequestSent(id)
        } yield ()

      case ConnectionRecord(
            id,
            _,
            _,
            _,
            _,
            Role.Inviter,
            ProtocolState.ConnectionResponsePending,
            _,
            _,
            Some(response)
          ) =>
        for {
          didComm <- ZIO.service[DidComm]
          _ <- sendMessage(response.makeMessage)
          connectionService <- ZIO.service[ConnectionService]
          _ <- connectionService.markConnectionResponseSent(id)
        } yield ()

      case _ => ZIO.unit
    }

    exchange.catchAll {
      case ex: TransportError => // : io.iohk.atala.mercury.model.error.MercuryError | java.io.IOException =>
        ex.printStackTrace()
        ZIO.logError(ex.getMessage()) *>
          ZIO.fail(mercuryErrorAsThrowable(ex))
      case ex: IOException => ZIO.fail(ex)
    }
  }

}
