package io.iohk.atala.pollux.sql.repository

import doobie.*
import doobie.implicits.*
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.iohk.atala.mercury.protocol.issuecredential.IssueCredential
import io.iohk.atala.mercury.protocol.issuecredential.OfferCredential
import io.iohk.atala.mercury.protocol.issuecredential.RequestCredential
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.repository.CredentialRepository
import io.iohk.atala.pollux.sql.model.JWTCredentialRow
import zio.*
import zio.interop.catz.*

import java.util.UUID
import java.time.Instant

// TODO: replace with actual implementation
class JdbcCredentialRepository(xa: Transactor[Task]) extends CredentialRepository[Task] {

  // Uncomment to have Doobie LogHandler in scope and automatically output SQL statements in logs
  // given logHandler: LogHandler = LogHandler.jdkLogHandler

  import IssueCredentialRecord._
  given uuidGet: Get[UUID] = Get[String].map(UUID.fromString)
  given uuidPut: Put[UUID] = Put[String].contramap(_.toString())

  given instantGet: Get[Instant] = Get[Long].map(Instant.ofEpochSecond)
  given instantPut: Put[Instant] = Put[Long].contramap(_.getEpochSecond())

  given protocolStateGet: Get[ProtocolState] = Get[String].map(ProtocolState.valueOf)
  given protocolStatePut: Put[ProtocolState] = Put[String].contramap(_.toString)

  given publicationStateGet: Get[PublicationState] = Get[String].map(PublicationState.valueOf)
  given publicationStatePut: Put[PublicationState] = Put[String].contramap(_.toString)

  given roleGet: Get[Role] = Get[String].map(Role.valueOf)
  given rolePut: Put[Role] = Put[String].contramap(_.toString)

  given offerCredentialGet: Get[OfferCredential] = Get[String].map(decode[OfferCredential](_).getOrElse(???))
  given offerCredentialPut: Put[OfferCredential] = Put[String].contramap(_.asJson.toString)

  given requestCredentialGet: Get[RequestCredential] = Get[String].map(decode[RequestCredential](_).getOrElse(???))
  given requestCredentialPut: Put[RequestCredential] = Put[String].contramap(_.asJson.toString)

  given issueCredentialGet: Get[IssueCredential] = Get[String].map(decode[IssueCredential](_).getOrElse(???))
  given issueCredentialPut: Put[IssueCredential] = Put[String].contramap(_.asJson.toString)

  override def createIssueCredentialRecord(record: IssueCredentialRecord): Task[Int] = {
    val cxnIO = sql"""
        | INSERT INTO public.issue_credential_records(
        |   id,
        |   created_at,
        |   updated_at,
        |   thid,
        |   schema_id,
        |   role,
        |   subject_id,
        |   validity_period,
        |   automatic_issuance,
        |   await_confirmation,
        |   protocol_state,
        |   publication_state,
        |   offer_credential_data
        | ) values (
        |   ${record.id},
        |   ${record.createdAt},
        |   ${record.updatedAt},
        |   ${record.thid},
        |   ${record.schemaId},
        |   ${record.role},
        |   ${record.subjectId},
        |   ${record.validityPeriod},
        |   ${record.automaticIssuance},
        |   ${record.awaitConfirmation},
        |   ${record.protocolState},
        |   ${record.publicationState},
        |   ${record.offerCredentialData}
        | )
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  override def getIssueCredentialRecords(): Task[Seq[IssueCredentialRecord]] = {
    val cxnIO = sql"""
        | SELECT
        |   id,
        |   created_at,
        |   updated_at,
        |   thid,
        |   schema_id,
        |   role,
        |   subject_id,
        |   validity_period,
        |   automatic_issuance,
        |   await_confirmation,
        |   protocol_state,
        |   publication_state,
        |   offer_credential_data,
        |   request_credential_data,
        |   issue_credential_data
        | FROM public.issue_credential_records
        """.stripMargin
      .query[IssueCredentialRecord]
      .to[Seq]

    cxnIO
      .transact(xa)
  }

  override def getIssueCredentialRecord(recordId: UUID): Task[Option[IssueCredentialRecord]] = {
    val cxnIO = sql"""
        | SELECT
        |   id,
        |   created_at,
        |   updated_at,
        |   thid,
        |   schema_id,
        |   role,
        |   subject_id,
        |   validity_period,
        |   automatic_issuance,
        |   await_confirmation,
        |   protocol_state,
        |   publication_state,
        |   offer_credential_data,
        |   request_credential_data,
        |   issue_credential_data
        | FROM public.issue_credential_records
        | WHERE id = $recordId
        """.stripMargin
      .query[IssueCredentialRecord]
      .option

    cxnIO
      .transact(xa)
  }

  override def getIssueCredentialRecordByThreadId(thid: UUID): Task[Option[IssueCredentialRecord]] = {
    val cxnIO = sql"""
        | SELECT
        |   id,
        |   created_at,
        |   updated_at,
        |   thid,
        |   schema_id,
        |   role,
        |   subject_id,
        |   validity_period,
        |   automatic_issuance,
        |   await_confirmation,
        |   protocol_state,
        |   publication_state,
        |   offer_credential_data,
        |   request_credential_data,
        |   issue_credential_data
        | FROM public.issue_credential_records
        | WHERE thid = $thid
        """.stripMargin
      .query[IssueCredentialRecord]
      .option

    cxnIO
      .transact(xa)
  }

  override def updateCredentialRecordProtocolState(
      recordId: UUID,
      from: IssueCredentialRecord.ProtocolState,
      to: IssueCredentialRecord.ProtocolState
  ): Task[Int] = {
    val cxnIO = sql"""
        | UPDATE public.issue_credential_records
        | SET
        |   protocol_state = $to,
        |   updated_at = ${Instant.now}
        | WHERE
        |   id = $recordId
        |   AND protocol_state = $from
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  override def updateCredentialRecordPublicationState(
      recordId: UUID,
      from: Option[IssueCredentialRecord.PublicationState],
      to: Option[IssueCredentialRecord.PublicationState]
  ): Task[Int] = {
    val pubStateFragment = from
      .map(state => fr"publication_state = $state")
      .getOrElse(fr"publication_state IS NULL")

    val cxnIO = sql"""
        | UPDATE public.issue_credential_records
        | SET
        |   publication_state = $to,
        |   updated_at = ${Instant.now}
        | WHERE
        |   id = $recordId
        |   AND $pubStateFragment
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  override def updateWithRequestCredential(recordId: UUID, request: RequestCredential, protocolState: ProtocolState): Task[Int] = {
    val cxnIO = sql"""
        | UPDATE public.issue_credential_records
        | SET
        |   request_credential_data = $request,
        |   protocol_state = $protocolState,
        |   updated_at = ${Instant.now}
        | WHERE
        |   id = $recordId
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  override def updateWithIssueCredential(recordId: UUID, issue: IssueCredential, protocolState: ProtocolState): Task[Int] = {
    val cxnIO = sql"""
        | UPDATE public.issue_credential_records
        | SET
        |   issue_credential_data = $issue,
        |   protocol_state = $protocolState,
        |   updated_at = ${Instant.now}
        | WHERE
        |   id = $recordId
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

}

object JdbcCredentialRepository {
  val layer: URLayer[Transactor[Task], CredentialRepository[Task]] =
    ZLayer.fromFunction(new JdbcCredentialRepository(_))
}
