package io.iohk.atala.agent.server.jobs

import zio._
import io.iohk.atala.connect.core.service.ConnectionService
import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.ConnectionRecord._
import io.iohk.atala.mercury._
import io.iohk.atala.mercury.model._
import io.iohk.atala.mercury.model.error._
import io.iohk.atala.mercury.protocol.issuecredential._
import io.iohk.atala.resolvers.DIDResolver
import java.io.IOException
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import org.didcommx.didcomm.DIDComm
import io.iohk.atala.mercury.PeerDID
import com.nimbusds.jose.jwk.OctetKeyPair
import io.iohk.atala.resolvers.UniversalDidResolver
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.agent.walletapi.model.error.DIDSecretStorageError
import io.iohk.atala.agent.walletapi.model.error.DIDSecretStorageError.KeyNotFoundError
import io.iohk.atala.agent.server.config.AppConfig

object ConnectBackgroundJobs {

  val didCommExchanges = {
    for {
      connectionService <- ZIO.service[ConnectionService]
      config <- ZIO.service[AppConfig]
      records <- connectionService
        .getConnectionRecordsByStates(
          ConnectionRecord.ProtocolState.ConnectionRequestPending,
          ConnectionRecord.ProtocolState.ConnectionResponsePending
        )
        .mapError(err => Throwable(s"Error occurred while getting connection records: $err"))
      _ <- ZIO.foreachPar(records)(performExchange).withParallelism(config.connect.connectBgJobProcessingParallelism)
    } yield ()
  }

  private[this] def performExchange(
      record: ConnectionRecord
  ): URIO[DidOps & DIDResolver & HttpClient & ConnectionService & ManagedDIDService, Unit] = {
    import Role._
    import ProtocolState._
    val exchange = record match {
      case ConnectionRecord(id, _, _, _, _, Invitee, ConnectionRequestPending, _, Some(request), _) =>
        for {
          didCommAgent <- buildDIDCommAgent(request.from)
          _ <- MessagingService.send(request.makeMessage).provideSomeLayer(didCommAgent)
          connectionService <- ZIO.service[ConnectionService]
          _ <- connectionService.markConnectionRequestSent(id)
        } yield ()

      case ConnectionRecord(id, _, _, _, _, Inviter, ConnectionResponsePending, _, _, Some(response)) =>
        for {
          didCommAgent <- buildDIDCommAgent(response.from)
          _ <- MessagingService.send(response.makeMessage).provideSomeLayer(didCommAgent)
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
        case ex: DIDSecretStorageError =>
          ZIO.logErrorCause(s"DID secret storage error processing record: ${record.id} ", Cause.fail(ex))
      }
      .catchAllDefect { case throwable =>
        ZIO.logErrorCause(s"Connection protocol defect processing record: ${record.id}", Cause.fail(throwable))
      }
  }

  private[this] def buildDIDCommAgent(
      myDid: DidId
  ): ZIO[ManagedDIDService, KeyNotFoundError, ZLayer[Any, Nothing, DidAgent]] = {
    for {
      managedDidService <- ZIO.service[ManagedDIDService]
      peerDID <- managedDidService.getPeerDID(myDid)
      agent = AgentPeerService.makeLayer(peerDID)
    } yield agent
  }

}
