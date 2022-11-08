package io.iohk.atala.pollux.sql.repository

import doobie.*
import doobie.implicits.*
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.repository.CredentialRepository
import io.iohk.atala.pollux.sql.model.JWTCredentialRow
import zio.*
import zio.interop.catz.*
import io.circe.syntax._
import io.circe._, io.circe.parser._
import java.util.UUID
import io.iohk.atala.mercury.protocol.issuecredential.RequestCredential
import io.iohk.atala.mercury.protocol.issuecredential.IssueCredential
import io.iohk.atala.mercury.protocol.issuecredential.OfferCredential

// TODO: replace with actual implementation
class JdbcCredentialRepository(xa: Transactor[Task]) extends CredentialRepository[Task] {

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
        |   state,
        |   offer_credential_data
        | ) values (
        |   ${record.id.toString},
        |   ${record.thid.toString},
        |   ${record.schemaId},
        |   ${record.role.toString},
        |   ${record.subjectId},
        |   ${record.validityPeriod},
        |   ${record.claims.asJson.toString},
        |   ${record.state.toString},
        |   ${record.offerCredentialData}
        | )
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  given uuidGet: Get[UUID] = Get[String].map(UUID.fromString(_))
  given stateGet: Get[IssueCredentialRecord.State] = Get[String].map(IssueCredentialRecord.State.valueOf(_))
  given claimsGet: Get[Map[String, String]] =
    Get[String].map(decode[Map[String, String]](_).getOrElse(Map("parsingError" -> "parsingError")))
  given roleGet: Get[IssueCredentialRecord.Role] = Get[String].map(IssueCredentialRecord.Role.valueOf(_))

  given offerCredentialGet: Get[OfferCredential] = Get[String].map(decode[OfferCredential](_).getOrElse(???))
  given offerCredentialPut: Put[OfferCredential] = Put[String].contramap(_.asJson.toString)

  given requestCredentialGet: Get[RequestCredential] = Get[String].map(decode[RequestCredential](_).getOrElse(???))
  given requestCredentialPut: Put[RequestCredential] = Put[String].contramap(_.asJson.toString)

  given issueCredentialGet: Get[IssueCredential] = Get[String].map(decode[IssueCredential](_).getOrElse(???))
  given issueCredentialPut: Put[IssueCredential] = Put[String].contramap(_.asJson.toString)

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
        |   state,
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
        |   state,
        |   offer_credential_data,
        |   request_credential_data,
        |   issue_credential_data
        | FROM public.issue_credential_records
        | WHERE id = ${id.toString}
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
        |   state,
        |   offer_credential_data,
        |   request_credential_data,
        |   issue_credential_data
        | FROM public.issue_credential_records
        | WHERE thid = ${thid.toString}
        """.stripMargin
      .query[IssueCredentialRecord]
      .option

    cxnIO
      .transact(xa)
  }

  override def updateCredentialRecordState(
      id: UUID,
      from: IssueCredentialRecord.State,
      to: IssueCredentialRecord.State
  ): Task[Int] = {
    val cxnIO = sql"""
        | UPDATE public.issue_credential_records
        | SET
        |   state = ${to.toString}
        | WHERE
        |   id = ${id.toString}
        |   AND state = ${from.toString}
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  override def updateWithRequestCredential(request: RequestCredential): Task[Int] = {
    val cxnIO = sql"""
        | UPDATE public.issue_credential_records
        | SET
        |   request_credential_data = ${request},
        |   state = ${IssueCredentialRecord.State.RequestReceived.toString}
        | WHERE
        |   thid = ${request.thid.get}
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  override def updateWithIssueCredential(issue: IssueCredential): Task[Int] = {
    val cxnIO = sql"""
        | UPDATE public.issue_credential_records
        | SET
        |   issue_credential_data = ${issue},
        |   state = ${IssueCredentialRecord.State.CredentialReceived.toString}
        | WHERE
        |   thid = ${issue.thid.get}
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

}

object JdbcCredentialRepository {
  val layer: URLayer[Transactor[Task], CredentialRepository[Task]] =
    ZLayer.fromFunction(new JdbcCredentialRepository(_))
}
