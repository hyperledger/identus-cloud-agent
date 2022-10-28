package io.iohk.atala.pollux.sql.repository

import doobie.*
import doobie.implicits.*
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.repository.CredentialRepository
import io.iohk.atala.pollux.sql.model.JWTCredentialRow
import io.iohk.atala.prism.crypto.MerkleInclusionProof
import io.iohk.atala.shared.utils.BytesOps
import zio.*
import zio.interop.catz.*

import java.util.UUID

// TODO: replace with actual implementation
class JdbcCredentialRepository(xa: Transactor[Task]) extends CredentialRepository[Task] {

  given uuidGet: Get[UUID] = Get[String].map(UUID.fromString(_))
  given stateGet: Get[IssueCredentialRecord.State] = Get[String].map(IssueCredentialRecord.State.valueOf(_))
  given claimsGet: Get[Map[String, String]] =
    Get[String].map(decode[Map[String, String]](_).getOrElse(Map("parsingError" -> "parsingError")))
  given inclusionProofGet: Get[MerkleInclusionProof] = Get[String].map(deserializeInclusionProof)

  // serializes into hex string
  private def serializeInclusionProof(proof: MerkleInclusionProof): String = BytesOps.bytesToHex(proof.encode.getBytes)

  // deserializes from the hex string
  private def deserializeInclusionProof(proof: String): MerkleInclusionProof =
    MerkleInclusionProof.decode(
      String(
        BytesOps.hexToBytes(proof)
      )
    )

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
        |   merkle_inclusion_proof
        |   subject_id,
        |   validity_period,
        |   claims,
        |   state
        | ) values (
        |   ${record.id.toString},
        |   ${record.schemaId},
        |   ${record.merkleInclusionProof.map(serializeInclusionProof)},
        |   ${record.subjectId},
        |   ${record.validityPeriod},
        |   ${record.claims.asJson.toString},
        |   ${record.state.toString}
        | )
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  override def getIssueCredentialRecords(): Task[Seq[IssueCredentialRecord]] = {
    val cxnIO = sql"""
        | SELECT
        |   id,
        |   credential_id
        |   schema_id,
        |   merkle_inclusion_proof
        |   subject_id,
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

  override def getIssueCredentialRecordsByState(
      state: IssueCredentialRecord.State
  ): Task[Seq[IssueCredentialRecord]] = {
    val cxnIO = sql"""
        | SELECT
        |   id,
        |   credential_id
        |   schema_id,
        |   merkle_inclusion_proof
        |   subject_id,
        |   validity_period,
        |   claims,
        |   state
        | FROM public.issue_credential_records
        | WHERE state = ${state.toString}
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
        |   credential_id
        |   schema_id,
        |   merkle_inclusion_proof
        |   subject_id,
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
