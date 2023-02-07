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
  ): URIO[DidOps & DIDResolver & HttpClient & ConnectionService & ManagedDIDService, Unit] = {
    import Role._
    import ProtocolState._
    val exchange = record match {
      case ConnectionRecord(
            id,
            _,
            _,
            _,
            _,
            Invitee,
            ConnectionRequestPending,
            _,
            Some(request),
            _,
            metaRetries,
            _
          ) if metaRetries > 0 =>
        val aux = for {

          didCommAgent <- buildDIDCommAgent(request.from)
          _ <- MessagingService.send(request.makeMessage).provideSomeLayer(didCommAgent)
          connectionService <- ZIO.service[ConnectionService]
          _ <- connectionService.markConnectionRequestSent(id)
        } yield ()

        // aux // TODO decrete metaRetries if it has a error
        aux

      case ConnectionRecord(
            id,
            _,
            _,
            _,
            _,
            Inviter,
            ConnectionResponsePending,
            _,
            _,
            Some(response),
            metaRetries,
            _
          ) if metaRetries > 0 =>
        val aux = for {
          didCommAgent <- buildDIDCommAgent(response.from)
          _ <- MessagingService.send(response.makeMessage).provideSomeLayer(didCommAgent)
          connectionService <- ZIO.service[ConnectionService]
          _ <- connectionService.markConnectionResponseSent(id)
        } yield ()

        aux.tapError(ex =>
          for {
            connectionService <- ZIO.service[ConnectionService]
            _ <- connectionService
              .reportProcessingFailure(id, None) // TODO ex get message
          } yield ()
        )
      case e
          if (e.protocolState == ConnectionRequestPending || e.protocolState == ConnectionResponsePending) && e.metaRetries == 0 =>
        ZIO.logWarning( // TODO use logDebug
          s"ConnectionRecord '${e.id}' has '${e.metaRetries}' retries and will NOT be processed"
        )
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
