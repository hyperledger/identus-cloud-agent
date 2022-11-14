package io.iohk.atala.pollux.sql.repository

import cats.instances.seq
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
import io.iohk.atala.prism.crypto.MerkleInclusionProof
import io.iohk.atala.shared.utils.BytesOps
import zio.*
import zio.interop.catz.*

import java.util.UUID
import io.iohk.atala.pollux.core.model.IssueCredentialRecord.ProtocolState
import java.{util => ju}

// TODO: replace with actual implementation
class JdbcCredentialRepository(xa: Transactor[Task]) extends CredentialRepository[Task] {
  // serializes into hex string

  private def serializeInclusionProof(proof: MerkleInclusionProof): String = BytesOps.bytesToHex(proof.encode.getBytes)

  // deserializes from the hex string
  private def deserializeInclusionProof(proof: String): MerkleInclusionProof =
    MerkleInclusionProof.decode(
      String(
        BytesOps.hexToBytes(proof)
      )
    )

  // Uncomment to have Doobie LogHandler in scope and automatically output SQL statements in logs
  // given logHandler: LogHandler = LogHandler.jdkLogHandler

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

  given inclusionProofGet: Get[MerkleInclusionProof] = Get[String].map(deserializeInclusionProof)

  override def createIssueCredentialRecord(record: IssueCredentialRecord): Task[Int] = {
    val cxnIO = sql"""
        | INSERT INTO public.issue_credential_records(
        |   id,
        |   thid,
        |   schema_id,
        |   merkle_inclusion_proof
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
        |   ${record.merkleInclusionProof.map(serializeInclusionProof)},
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
        |   credential_id
        |   merkle_inclusion_proof
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

  override def getIssueCredentialRecordsByState(
      state: IssueCredentialRecord.ProtocolState
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
        | WHERE protocol_state = ${state.toString}
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
        |   credential_id
        |   schema_id,
        |   merkle_inclusion_proof
        |   thid,
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
        |   credential_id
        |   schema_id,
        |   merkle_inclusion_proof
        |   thid,
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
      recordId: UUID,
      from: IssueCredentialRecord.ProtocolState,
      to: IssueCredentialRecord.ProtocolState
  ): Task[Int] = {
    val cxnIO = sql"""
        | UPDATE public.issue_credential_records
        | SET
        |   protocol_state = $to
        | WHERE
        |   id = $recordId
        |   AND protocol_state = $from
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  override def updateCredentialRecordStateAndProofByCredentialIdBulk(
      idsStatesAndProofs: Seq[(UUID, IssueCredentialRecord.PublicationState, MerkleInclusionProof)]
  ): Task[Int] = {

    if (idsStatesAndProofs.isEmpty) ZIO.succeed(0)
    else
      val values = idsStatesAndProofs.map { idStateAndProof =>
        val (id, state, proof) = idStateAndProof
        s"(${id.toString}, '${state.toString}', '${serializeInclusionProof(proof)}')"
      }

      val cxnIO = sql"""
          | UPDATE public.issue_credential_records as icr
          | SET 
          |   publication_state = idsStatesAndProofs.publication_state,
          |   merkle_inclusion_proof = idsStatesAndProofs.serializedProof
          | FROM (values ${values.mkString(",")}) as idsStatesAndProofs(id, publication_state, serializedProof)
          | WHERE icr.id = idsStatesAndProofs.id
          |""".stripMargin.update

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
        |   publication_state = $to
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
        |   protocol_state = $protocolState
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
        |   protocol_state = $protocolState
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
