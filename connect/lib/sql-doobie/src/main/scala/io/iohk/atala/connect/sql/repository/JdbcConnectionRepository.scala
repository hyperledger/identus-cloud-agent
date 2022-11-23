package io.iohk.atala.connect.sql.repository

import doobie.*
import doobie.implicits.*
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.iohk.atala.connect.core.model.*
import zio.*
import zio.interop.catz.*
import io.iohk.atala.mercury.protocol.connection.*
import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation
import java.util.UUID
import io.iohk.atala.connect.core.model.ConnectionRecord.Role
import io.iohk.atala.connect.core.model.ConnectionRecord.ProtocolState
import io.iohk.atala.connect.core.repository.ConnectionRepository
import java.time.Instant

class JdbcConnectionRepository(xa: Transactor[Task]) extends ConnectionRepository[Task] {

  // given logHandler: LogHandler = LogHandler.jdkLogHandler

  given uuidGet: Get[UUID] = Get[String].map(UUID.fromString)
  given uuidPut: Put[UUID] = Put[String].contramap(_.toString())

  given instantGet: Get[Instant] = Get[Long].map(Instant.ofEpochSecond)
  given instantPut: Put[Instant] = Put[Long].contramap(_.getEpochSecond())

  given protocolStateGet: Get[ProtocolState] = Get[String].map(ProtocolState.valueOf)
  given protocolStatePut: Put[ProtocolState] = Put[String].contramap(_.toString)

  given roleGet: Get[Role] = Get[String].map(Role.valueOf)
  given rolePut: Put[Role] = Put[String].contramap(_.toString)

  given invitationGet: Get[Invitation] = Get[String].map(decode[Invitation](_).getOrElse(???))
  given invitationPut: Put[Invitation] = Put[String].contramap(_.asJson.toString)

  given connectionRequestGet: Get[ConnectionRequest] = Get[String].map(decode[ConnectionRequest](_).getOrElse(???))
  given connectionRequestPut: Put[ConnectionRequest] = Put[String].contramap(_.asJson.toString)

  given connectionResponseGet: Get[ConnectionResponse] = Get[String].map(decode[ConnectionResponse](_).getOrElse(???))
  given connectionResponsePut: Put[ConnectionResponse] = Put[String].contramap(_.asJson.toString)

  override def createConnectionRecord(record: ConnectionRecord): Task[Int] = {
    val cxnIO = sql"""
        | INSERT INTO public.connection_records(
        |   id,
        |   created_at,
        |   updated_at,
        |   thid,
        |   label,
        |   role,
        |   protocol_state,
        |   invitation
        | ) values (
        |   ${record.id},
        |   ${record.createdAt},
        |   ${record.updatedAt},
        |   ${record.thid},
        |   ${record.label},
        |   ${record.role},
        |   ${record.protocolState},
        |   ${record.invitation}
        | )
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  override def getConnectionRecords(): Task[Seq[ConnectionRecord]] = {
    val cxnIO = sql"""
        | SELECT
        |   id,
        |   created_at,
        |   updated_at,
        |   thid,
        |   label,
        |   role,
        |   protocol_state,
        |   invitation,
        |   connection_request,
        |   connection_response
        | FROM public.connection_records
        """.stripMargin
      .query[ConnectionRecord]
      .to[Seq]

    cxnIO
      .transact(xa)
  }

  override def getConnectionRecord(recordId: UUID): Task[Option[ConnectionRecord]] = {
    val cxnIO = sql"""
        | SELECT
        |   id,
        |   created_at,
        |   updated_at,
        |   thid,
        |   label,
        |   role,
        |   protocol_state,
        |   invitation,
        |   connection_request,
        |   connection_response
        | FROM public.connection_records
        | WHERE id = $recordId
        """.stripMargin
      .query[ConnectionRecord]
      .option

    cxnIO
      .transact(xa)
  }

  override def deleteConnectionRecord(recordId: UUID): Task[Int] = {
    val cxnIO = sql"""
      | DELETE
      | FROM public.connection_records
      | WHERE id = $recordId
      """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  override def getConnectionRecordByThreadId(thid: UUID): Task[Option[ConnectionRecord]] = {
    val cxnIO = sql"""
        | SELECT
        |   id,
        |   created_at,
        |   updated_at,
        |   thid,
        |   label,
        |   role,
        |   protocol_state,
        |   invitation,
        |   connection_request,
        |   connection_response
        | FROM public.connection_records
        | WHERE thid = $thid
        """.stripMargin
      .query[ConnectionRecord]
      .option

    cxnIO
      .transact(xa)
  }

  override def updateConnectionProtocolState(
      id: UUID,
      from: ConnectionRecord.ProtocolState,
      to: ConnectionRecord.ProtocolState
  ): Task[Int] = {
    val cxnIO = sql"""
        | UPDATE public.connection_records
        | SET
        |   protocol_state = $to
        | WHERE
        |   id = $id
        |   AND protocol_state = $from
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  override def updateWithConnectionRequest(
      recordId: UUID,
      request: ConnectionRequest,
      state: ProtocolState
  ): Task[Int] = {
    val cxnIO = sql"""
        | UPDATE public.connection_records
        | SET
        |   connection_request = $request,
        |   protocol_state = $state
        | WHERE
        |   id = $recordId
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  override def updateWithConnectionResponse(
      recordId: UUID,
      response: ConnectionResponse,
      state: ProtocolState
  ): Task[Int] = {
    val cxnIO = sql"""
        | UPDATE public.connection_records
        | SET
        |   connection_response = $response,
        |   protocol_state = $state
        | WHERE
        |   id = $recordId
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

}

object JdbcConnectionRepository {
  val layer: URLayer[Transactor[Task], ConnectionRepository[Task]] =
    ZLayer.fromFunction(new JdbcConnectionRepository(_))
}
