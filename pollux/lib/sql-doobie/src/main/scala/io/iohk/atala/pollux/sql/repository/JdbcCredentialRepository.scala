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

// TODO: replace with actual implementation
class JdbcCredentialRepository(xa: Transactor[Task]) extends CredentialRepository[Task] {

  given logHandler: LogHandler = LogHandler.jdkLogHandler

  import IssueCredentialRecord._
  given uuidGet: Get[UUID] = Get[String].map(UUID.fromString)
  given uuidPut: Put[UUID] = Put[String].contramap(_.toString())

  given protocolStateGet: Get[ProtocolState] = Get[String].map(ProtocolState.valueOf)
  given protocolStatePut: Put[ProtocolState] = Put[String].contramap(_.toString)

  given publicationStateGet: Get[PublicationState] = Get[String].map(PublicationState.valueOf)
  given publicationStatePut: Put[PublicationState] = Put[String].contramap(_.toString)

  given claimsGet: Get[Map[String, String]] = Get[String].map(
    decode[Map[String, String]](_)
      .getOrElse(Map("parsingError" -> "parsingError"))
  )
  given claimsPut: Put[Map[String, String]] = Put[String].contramap(_.asJson.toString)

  given roleGet: Get[Role] = Get[String].map(Role.valueOf)
  given rolePut: Put[Role] = Put[String].contramap(_.toString)

  given offerCredentialGet: Get[OfferCredential] = Get[String].map(decode[OfferCredential](_).getOrElse(???))
  given offerCredentialPut: Put[OfferCredential] = Put[String].contramap(_.asJson.toString)

  given requestCredentialGet: Get[RequestCredential] = Get[String].map(decode[RequestCredential](_).getOrElse(???))
  given requestCredentialPut: Put[RequestCredential] = Put[String].contramap(_.asJson.toString)

  given issueCredentialGet: Get[IssueCredential] = Get[String].map(decode[IssueCredential](_).getOrElse(???))
  given issueCredentialPut: Put[IssueCredential] = Put[String].contramap(_.asJson.toString)

  override def createCredentials(batchId: String, credentials: Seq[EncodedJWTCredential]): Task[Unit] = {
    ZIO.succeed(())
  }
  override def getCredentials(batchId: String): Task[Seq[EncodedJWTCredential]] = {
    val cxnIO = sql"""
        | SELECT
        |   c.batch_id
        |   c.credential_id
        |   c.value
        | FROM public.jwt_credentials AS c
        | WHERE c.batch_id = $batchId
        """.stripMargin
      .query[JWTCredentialRow]
      .to[Seq]

    cxnIO
      .transact(xa)
      .map(_.map(c => EncodedJWTCredential(c.batchId, c.credentialId, c.content)))
  }

  override def createIssueCredentialRecord(record: IssueCredentialRecord): Task[Int] = {
    val cxnIO = sql"""
        | INSERT INTO public.issue_credential_records(
        |   id,
        |   thid,
        |   schema_id,
        |   role,
        |   subject_id,
        |   validity_period,
        |   claims,
        |   protocol_state,
        |   publication_state,
        |   offer_credential_data
        | ) values (
        |   ${record.id},
        |   ${record.thid},
        |   ${record.schemaId},
        |   ${record.role},
        |   ${record.subjectId},
        |   ${record.validityPeriod},
        |   ${record.claims},
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
        |   thid,
        |   schema_id,
        |   role,
        |   subject_id,
        |   validity_period,
        |   claims,
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

  override def getIssueCredentialRecord(id: UUID): Task[Option[IssueCredentialRecord]] = {
    val cxnIO = sql"""
        | SELECT
        |   id,
        |   thid,
        |   schema_id,
        |   role,
        |   subject_id,
        |   validity_period,
        |   claims,
        |   protocol_state,
        |   publication_state,
        |   offer_credential_data,
        |   request_credential_data,
        |   issue_credential_data
        | FROM public.issue_credential_records
        | WHERE id = $id
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
        |   thid,
        |   schema_id,
        |   role,
        |   subject_id,
        |   validity_period,
        |   claims,
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
      id: UUID,
      from: IssueCredentialRecord.ProtocolState,
      to: IssueCredentialRecord.ProtocolState
  ): Task[Int] = {
    val cxnIO = sql"""
        | UPDATE public.issue_credential_records
        | SET
        |   protocol_state = $to
        | WHERE
        |   id = $id
        |   AND protocol_state = $from
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  override def updateCredentialRecordPublicationState(
      id: UUID,
      from: Option[IssueCredentialRecord.PublicationState],
      to: Option[IssueCredentialRecord.PublicationState]
  ): Task[Int] = {
    val pubStateFragment = from
      .map(state => fr"publication_state = $state")
      .getOrElse(fr"publication_state IS NULL")

    val cxnIO = sql"""
        | UPDATE public.issue_credential_records
        | SET
        |   publication_state = $to
        | WHERE
        |   id = $id
        |   AND $pubStateFragment
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  override def updateWithRequestCredential(request: RequestCredential): Task[Int] = {
    request.thid match
      case None =>
        ZIO.succeed(0)
      case Some(value) =>
        val cxnIO = sql"""
            | UPDATE public.issue_credential_records
            | SET
            |   request_credential_data = $request,
            |   protocol_state = ${IssueCredentialRecord.ProtocolState.RequestReceived}
            | WHERE
            |   thid = $value
            """.stripMargin.update

        cxnIO.run
          .transact(xa)
  }

  override def updateWithIssueCredential(issue: IssueCredential): Task[Int] = {
    issue.thid match
      case None =>
        ZIO.succeed(0)
      case Some(value) =>
        val cxnIO = sql"""
            | UPDATE public.issue_credential_records
            | SET
            |   issue_credential_data = $issue,
            |   protocol_state = ${IssueCredentialRecord.ProtocolState.CredentialReceived}
            | WHERE
            |   thid = $value
            """.stripMargin.update

        cxnIO.run
          .transact(xa)
  }

}

object JdbcCredentialRepository {
  val layer: URLayer[Transactor[Task], CredentialRepository[Task]] =
    ZLayer.fromFunction(new JdbcCredentialRepository(_))
}
