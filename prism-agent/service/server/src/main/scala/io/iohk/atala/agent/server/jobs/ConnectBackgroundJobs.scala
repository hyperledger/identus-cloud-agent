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
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.mercury.AgentServiceAny
import org.didcommx.didcomm.DIDComm
import io.iohk.atala.mercury.PeerDID
import com.nimbusds.jose.jwk.OctetKeyPair
import io.iohk.atala.resolvers.UniversalDidResolver
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.agent.walletapi.model.error.DIDSecretStorageError

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
  ): URIO[ConnectionService & ManagedDIDService, Unit] = {
    import Role._
    import ProtocolState._
    val exchange = record match {
      case ConnectionRecord(id, _, _, _, _, Invitee, ConnectionRequestPending, _, Some(request), _) =>
        (for {
          didCommAgent <- buildDIDCommAgent(request.from)
          _ <- sendMessage(request.makeMessage).provide(didCommAgent)
          connectionService <- ZIO.service[ConnectionService]
          _ <- connectionService.markConnectionRequestSent(id)
        } yield ()): ZIO[
          ConnectionService & ManagedDIDService,
          ConnectionServiceError | MercuryException | DIDSecretStorageError,
          Unit
        ]

      case ConnectionRecord(id, _, _, _, _, Inviter, ConnectionResponsePending, _, _, Some(response)) =>
        (for {
          didCommAgent <- buildDIDCommAgent(response.from)
          _ <- sendMessage(response.makeMessage).provide(didCommAgent)
          connectionService <- ZIO.service[ConnectionService]
          _ <- connectionService.markConnectionResponseSent(id)
        } yield ()): ZIO[
          ConnectionService & ManagedDIDService,
          ConnectionServiceError | MercuryException | DIDSecretStorageError,
          Unit
        ]

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

  private[this] def buildDIDCommAgent(myDid: DidId) = {
    for {
      managedDidService <- ZIO.service[ManagedDIDService]
      peerDID <- managedDidService.getPeerDID(myDid)
      didCommAgent = ZLayer.succeed(
        AgentServiceAny(
          new DIDComm(UniversalDidResolver, peerDID.getSecretResolverInMemory),
          peerDID.did
        )
      )
    } yield didCommAgent
  }

}
