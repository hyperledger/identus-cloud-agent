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
        |   schema_id,
        |   subject_id,
        |   role,
        |   validity_period,
        |   claims,
        |   state
        | ) values (
        |   ${record.id.toString},
        |   ${record.schemaId},
        |   ${record.subjectId},
        |   ${record.role.toString},
        |   ${record.validityPeriod},
        |   ${record.claims.asJson.toString},
        |   ${record.state.toString}
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

  override def getIssueCredentialRecords(): Task[Seq[IssueCredentialRecord]] = {
    val cxnIO = sql"""
        | SELECT
        |   id,
        |   schema_id,
        |   subject_id,
        |   role,
        |   validity_period,
        |   claims,
        |   state
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
        |   schema_id,
        |   subject_id,
        |   role,
        |   validity_period,
        |   claims,
        |   state
        | FROM public.issue_credential_records
        | WHERE id = ${id.toString}
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

}

object JdbcCredentialRepository {
  val layer: URLayer[Transactor[Task], CredentialRepository[Task]] =
    ZLayer.fromFunction(new JdbcCredentialRepository(_))
}
