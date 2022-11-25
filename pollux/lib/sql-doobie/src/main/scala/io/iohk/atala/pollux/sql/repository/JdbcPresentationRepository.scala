package io.iohk.atala.pollux.sql.repository

import cats.instances.seq
import doobie.*
import doobie.implicits.*
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.iohk.atala.mercury.protocol.presentproof._
import io.iohk.atala.pollux.core.model.PresentationRecord.ProtocolState
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.repository.PresentationRepository
import io.iohk.atala.pollux.sql.model.JWTCredentialRow
import io.iohk.atala.prism.crypto.MerkleInclusionProof
import io.iohk.atala.shared.utils.BytesOps
import zio.*
import zio.interop.catz.*

import java.time.Instant
import java.util.UUID

// TODO: replace with actual implementation
class JdbcPresentationRepository(xa: Transactor[Task]) extends PresentationRepository[Task] {
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

  import PresentationRecord._
  given uuidGet: Get[UUID] = Get[String].map(UUID.fromString)
  given uuidPut: Put[UUID] = Put[String].contramap(_.toString())

  given instantGet: Get[Instant] = Get[Long].map(Instant.ofEpochSecond)
  given instantPut: Put[Instant] = Put[Long].contramap(_.getEpochSecond())

  given protocolStateGet: Get[ProtocolState] = Get[String].map(ProtocolState.valueOf)
  given protocolStatePut: Put[ProtocolState] = Put[String].contramap(_.toString)

  given roleGet: Get[Role] = Get[String].map(Role.valueOf)
  given rolePut: Put[Role] = Put[String].contramap(_.toString)

  given presentationGet: Get[Presentation] = Get[String].map(decode[Presentation](_).getOrElse(???))
  given presentationPut: Put[Presentation] = Put[String].contramap(_.asJson.toString)

  given requestPresentationGet: Get[RequestPresentation] =
    Get[String].map(decode[RequestPresentation](_).getOrElse(???))
  given requestPresentationPut: Put[RequestPresentation] = Put[String].contramap(_.asJson.toString)

  given proposePresentationGet: Get[ProposePresentation] =
    Get[String].map(decode[ProposePresentation](_).getOrElse(???))
  given proposePresentationPut: Put[ProposePresentation] = Put[String].contramap(_.asJson.toString)

  given inclusionProofGet: Get[MerkleInclusionProof] = Get[String].map(deserializeInclusionProof)

  override def createPresentationRecord(record: PresentationRecord): Task[Int] = {
    val cxnIO = sql"""
        | INSERT INTO public.presentation_records(
        |   id,
        |   created_at,
        |   updated_at,
        |   thid,
        |   schema_id,
        |   role,
        |   subject_id,
        |   protocol_state,
        |   request_presentation_data
        | ) values (
        |   ${record.id},
        |   ${record.createdAt},
        |   ${record.updatedAt},
        |   ${record.thid},
        |   ${record.schemaId},
        |   ${record.role},
        |   ${record.subjectId},
        |   ${record.protocolState},
        |   ${record.requestPresentationData}
        | )
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  override def getPresentationRecords(): Task[Seq[PresentationRecord]] = {
    val cxnIO = sql"""
        | SELECT
        |   id,
        |   created_at,
        |   updated_at,
        |   thid,
        |   schema_id,
        |   role,
        |   subject_id,
        |   protocol_state,
        |   request_presentation_data,
        |   propose_presentation_data,
        |   presentation_data
        | FROM public.presentation_records
        """.stripMargin
      .query[PresentationRecord]
      .to[Seq]

    cxnIO
      .transact(xa)
  }

  override def getPresentationRecordsByState(
      state: PresentationRecord.ProtocolState
  ): Task[Seq[PresentationRecord]] = {
    val cxnIO = sql"""
        | SELECT
        |   id,
        |   created_at,
        |   updated_at,
        |   thid,
        |   schema_id,
        |   role,
        |   subject_id,
        |   protocol_state,
        |   request_presentation_data,
        |   propose_presentation_data,
        |   presentation_data
        | FROM public.presentation_records
        | WHERE protocol_state = ${state.toString}
        """.stripMargin
      .query[PresentationRecord]
      .to[Seq]

    cxnIO
      .transact(xa)
  }

  override def getPresentationRecord(recordId: UUID): Task[Option[PresentationRecord]] = {
    val cxnIO = sql"""
        | SELECT
        |   id,
        |   created_at,
        |   updated_at,
        |   thid,
        |   schema_id,
        |   role,
        |   subject_id,
        |   protocol_state,
        |   request_presentation_data,
        |   propose_presentation_data,
        |   presentation_data
        | FROM public.presentation_records
        | WHERE id = $recordId
        """.stripMargin
      .query[PresentationRecord]
      .option

    cxnIO
      .transact(xa)
  }

  override def getPresentationRecordByThreadId(thid: UUID): Task[Option[PresentationRecord]] = {
    val cxnIO = sql"""
        | SELECT
        |   id,
        |   created_at,
        |   updated_at,
        |   thid,
        |   schema_id,
        |   role,
        |   subject_id,
        |   protocol_state,
        |   request_presentation_data,
        |   propose_presentation_data,
        |   presentation_data
        | FROM public.presentation_records
        | WHERE thid = $thid
        """.stripMargin
      .query[PresentationRecord]
      .option

    cxnIO
      .transact(xa)
  }

  override def updatePresentationRecordProtocolState(
      recordId: UUID,
      from: PresentationRecord.ProtocolState,
      to: PresentationRecord.ProtocolState
  ): Task[Int] = {
    val cxnIO = sql"""
        | UPDATE public.presentation_records
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

  override def updateWithRequestPresentation(
      recordId: UUID,
      request: RequestPresentation,
      protocolState: ProtocolState
  ): Task[Int] = {
    val cxnIO = sql"""
        | UPDATE public.presentation_records
        | SET
        |   request_presentation_data = $request,
        |   protocol_state = $protocolState,
        |   updated_at = ${Instant.now}
        | WHERE
        |   id = $recordId
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  override def updateWithProposePresentation(
      recordId: UUID,
      propose: ProposePresentation,
      protocolState: ProtocolState
  ): Task[Int] = {
    val cxnIO = sql"""
        | UPDATE public.presentation_records
        | SET
        |   propose_presentation_data = $propose,
        |   protocol_state = $protocolState,
        |   updated_at = ${Instant.now}
        | WHERE
        |   id = $recordId
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  override def updateWithPresentation(
      recordId: UUID,
      presentation: Presentation,
      protocolState: ProtocolState
  ): Task[Int] = {
    val cxnIO = sql"""
        | UPDATE public.presentation_records
        | SET
        |   presentation_data = $presentation,
        |   protocol_state = $protocolState,
        |   updated_at = ${Instant.now}
        | WHERE
        |   id = $recordId
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

}

object JdbcPresentationRepository {
  val layer: URLayer[Transactor[Task], PresentationRepository[Task]] =
    ZLayer.fromFunction(new JdbcPresentationRepository(_))
}
