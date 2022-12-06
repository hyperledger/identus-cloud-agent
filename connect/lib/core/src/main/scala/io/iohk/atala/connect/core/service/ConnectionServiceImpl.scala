package io.iohk.atala.connect.core.service

import io.iohk.atala.connect.core.repository.ConnectionRepository
import io.iohk.atala.mercury.DidComm
import zio._
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.connect.core.model.error.ConnectionServiceError._
import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.ConnectionRecord._
import io.iohk.atala.mercury.protocol.connection.ConnectionRequest
import java.util.UUID
import io.iohk.atala.mercury._
import io.iohk.atala.mercury.model.DidId
import java.time.Instant
import java.rmi.UnexpectedException
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation
import io.iohk.atala.mercury.protocol.connection.ConnectionResponse
import io.iohk.atala.shared.utils.Base64Utils

private class ConnectionServiceImpl(
    connectionRepository: ConnectionRepository[Task]
) extends ConnectionService {

  override def createConnectionInvitation(
      label: Option[String],
      pairwiseDID: DidId
  ): IO[ConnectionServiceError, ConnectionRecord] =
    for {
      recordId <- ZIO.succeed(UUID.randomUUID)
      invitation <- ZIO.succeed(createDidCommInvitation(recordId, pairwiseDID))
      record <- ZIO.succeed(
        ConnectionRecord(
          id = recordId,
          createdAt = Instant.now,
          updatedAt = None,
          thid = Some(recordId),
          label = label,
          role = ConnectionRecord.Role.Inviter,
          protocolState = ConnectionRecord.ProtocolState.InvitationGenerated,
          invitation = invitation,
          connectionRequest = None,
          connectionResponse = None
        )
      )
      count <- connectionRepository
        .createConnectionRecord(record)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
    } yield record

  override def getConnectionRecords(): IO[ConnectionServiceError, Seq[ConnectionRecord]] = {
    for {
      records <- connectionRepository
        .getConnectionRecords()
        .mapError(RepositoryError.apply)
    } yield records
  }

  override def getConnectionRecord(recordId: UUID): IO[ConnectionServiceError, Option[ConnectionRecord]] = {
    for {
      record <- connectionRepository
        .getConnectionRecord(recordId)
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def deleteConnectionRecord(recordId: UUID): IO[ConnectionServiceError, Int] = ???

  override def receiveConnectionInvitation(invitation: String): IO[ConnectionServiceError, ConnectionRecord] =
    for {
      invitation <- ZIO
        .fromEither(io.circe.parser.decode[Invitation](Base64Utils.decodeUrlToString(invitation)))
        .mapError(err => InvitationParsingError(err))
      _ <- connectionRepository
        .getConnectionRecordByThreadId(UUID.fromString(invitation.id))
        .mapError(RepositoryError.apply)
        .flatMap {
          case None    => ZIO.unit
          case Some(_) => ZIO.fail(InvitationAlreadyReceived(invitation.id))
        }
      record <- ZIO.succeed(
        ConnectionRecord(
          id = UUID.randomUUID(),
          createdAt = Instant.now,
          updatedAt = None,
          thid = Some(
            UUID.fromString(invitation.id)
          ), // TODO: According to the standard, we should rather use 'pthid' and not 'thid'
          label = None,
          role = ConnectionRecord.Role.Invitee,
          protocolState = ConnectionRecord.ProtocolState.InvitationReceived,
          invitation = invitation,
          connectionRequest = None,
          connectionResponse = None
        )
      )
      count <- connectionRepository
        .createConnectionRecord(record)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
    } yield record

  override def acceptConnectionInvitation(
      recordId: UUID,
      pairwiseDid: DidId
  ): IO[ConnectionServiceError, Option[ConnectionRecord]] =
    for {
      record <- getRecordWithState(recordId, ProtocolState.InvitationReceived)
      request = createDidCommConnectionRequest(record, pairwiseDid)
      count <- connectionRepository
        .updateWithConnectionRequest(recordId, request, ProtocolState.ConnectionRequestPending)
        .mapError(RepositoryError.apply)
      _ <- count match
        case 1 => ZIO.succeed(())
        case n => ZIO.fail(RecordIdNotFound(recordId))
      record <- connectionRepository
        .getConnectionRecord(record.id)
        .mapError(RepositoryError.apply)
    } yield record

  override def markConnectionRequestSent(recordId: UUID): IO[ConnectionServiceError, Option[ConnectionRecord]] =
    updateConnectionProtocolState(
      recordId,
      ProtocolState.ConnectionRequestPending,
      ProtocolState.ConnectionRequestSent
    )

  override def receiveConnectionRequest(
      request: ConnectionRequest
  ): IO[ConnectionServiceError, Option[ConnectionRecord]] =
    for {
      record <- getRecordFromThreadIdAndState(request.thid, ProtocolState.InvitationGenerated)
      _ <- connectionRepository
        .updateWithConnectionRequest(record.id, request, ProtocolState.ConnectionRequestReceived)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
      record <- connectionRepository
        .getConnectionRecord(record.id)
        .mapError(RepositoryError.apply)
    } yield record

  override def acceptConnectionRequest(recordId: UUID): IO[ConnectionServiceError, Option[ConnectionRecord]] =
    for {
      record <- getRecordWithState(recordId, ProtocolState.ConnectionRequestReceived)
      response = createDidCommConnectionResponse(record)
      count <- connectionRepository
        .updateWithConnectionResponse(recordId, response, ProtocolState.ConnectionResponsePending)
        .mapError(RepositoryError.apply)
      _ <- count match
        case 1 => ZIO.succeed(())
        case n => ZIO.fail(RecordIdNotFound(recordId))
      record <- connectionRepository
        .getConnectionRecord(record.id)
        .mapError(RepositoryError.apply)
    } yield record

  override def markConnectionResponseSent(recordId: UUID): IO[ConnectionServiceError, Option[ConnectionRecord]] =
    updateConnectionProtocolState(
      recordId,
      ProtocolState.ConnectionResponsePending,
      ProtocolState.ConnectionResponseSent
    )

  override def receiveConnectionResponse(
      response: ConnectionResponse
  ): IO[ConnectionServiceError, Option[ConnectionRecord]] =
    for {
      record <- getRecordFromThreadIdAndState(response.thid, ProtocolState.ConnectionRequestSent)
      _ <- connectionRepository
        .updateWithConnectionResponse(record.id, response, ProtocolState.ConnectionResponseReceived)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
      record <- connectionRepository
        .getConnectionRecord(record.id)
        .mapError(RepositoryError.apply)
    } yield record

  private[this] def getRecordWithState(
      recordId: UUID,
      state: ProtocolState
  ): IO[ConnectionServiceError, ConnectionRecord] = {
    for {
      maybeRecord <- connectionRepository
        .getConnectionRecord(recordId)
        .mapError(RepositoryError.apply)
      record <- ZIO
        .fromOption(maybeRecord)
        .mapError(_ => RecordIdNotFound(recordId))
      _ <- record.protocolState match {
        case s if s == state => ZIO.unit
        case state           => ZIO.fail(InvalidFlowStateError(s"Invalid protocol state for operation: $state"))
      }
    } yield record
  }

  private[this] def createDidCommInvitation(thid: UUID, from: DidId): Invitation = {
    Invitation(
      id = thid.toString,
      from = from,
      body = Invitation.Body(goal_code = "connect", goal = "Establish a trust connection between two peers", Nil)
    )
  }

  private[this] def createDidCommConnectionRequest(record: ConnectionRecord, pairwiseDid: DidId): ConnectionRequest = {
    ConnectionRequest(
      from = pairwiseDid,
      to = record.invitation.from,
      thid = record.thid.map(_.toString),
      body = ConnectionRequest.Body(goal_code = Some("Connect"))
    )
  }

  private[this] def createDidCommConnectionResponse(record: ConnectionRecord): ConnectionResponse =
    ConnectionResponse.makeResponseFromRequest(record.connectionRequest.get.makeMessage) // TODO: get

  private[this] def updateConnectionProtocolState(
      recordId: UUID,
      from: ProtocolState,
      to: ProtocolState
  ): IO[ConnectionServiceError, Option[ConnectionRecord]] = {
    for {
      _ <- connectionRepository
        .updateConnectionProtocolState(recordId, from, to)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
      record <- connectionRepository
        .getConnectionRecord(recordId)
        .mapError(RepositoryError.apply)
    } yield record
  }

  private[this] def getRecordFromThreadIdAndState(
      thid: Option[String],
      state: ProtocolState
  ): IO[ConnectionServiceError, ConnectionRecord] = {
    for {
      thid <- ZIO
        .fromOption(thid)
        .mapError(_ => UnexpectedError("No `thid` found in credential request"))
        .map(UUID.fromString)
      maybeRecord <- connectionRepository
        .getConnectionRecordByThreadId(thid)
        .mapError(RepositoryError.apply)
      record <- ZIO
        .fromOption(maybeRecord)
        .mapError(_ => ThreadIdNotFound(thid))
      _ <- record.protocolState match {
        case s if s == state => ZIO.unit
        case state           => ZIO.fail(InvalidFlowStateError(s"Invalid protocol state for operation: $state"))
      }
    } yield record
  }

}

object ConnectionServiceImpl {
  val layer: URLayer[ConnectionRepository[Task], ConnectionService] =
    ZLayer.fromFunction(ConnectionServiceImpl(_))
}
