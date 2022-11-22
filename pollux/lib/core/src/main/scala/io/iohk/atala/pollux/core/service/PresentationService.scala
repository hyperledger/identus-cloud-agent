package io.iohk.atala.pollux.core.service

import com.google.protobuf.ByteString
import io.circe.Json
import io.circe.syntax.*
import io.iohk.atala.iris.proto.dlt.IrisOperation
import io.iohk.atala.iris.proto.service.IrisOperationId
import io.iohk.atala.iris.proto.service.IrisServiceGrpc.IrisServiceStub
import io.iohk.atala.pollux.core.model.EncodedJWTCredential
import io.iohk.atala.pollux.core.model.PresentationRecord
import io.iohk.atala.pollux.core.model.error.PresentationError
import io.iohk.atala.pollux.core.model.error.PresentationError._
import io.iohk.atala.pollux.core.repository.PresentationRepository
import io.iohk.atala.pollux.vc.jwt.*
import io.iohk.atala.prism.crypto.MerkleInclusionProof
import io.iohk.atala.prism.crypto.MerkleTreeKt
import io.iohk.atala.prism.crypto.Sha256
import zio.*
import io.iohk.atala.mercury.model.AttachmentDescriptor
import java.rmi.UnexpectedException
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.util.UUID

import io.iohk.atala.mercury.protocol.presentproof._
import io.iohk.atala.mercury.model.AttachmentDescriptor
import io.iohk.atala.mercury.DidComm
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.model.Message
import java.time.Instant
import io.iohk.atala.mercury.protocol.presentproof.RequestPresentation

trait PresentationService {

  def extractIdFromCredential(credential: W3cCredentialPayload): Option[UUID] 

  def createPresentationRecord(
      thid: UUID,
      subjectId: String,
      schemaId: Option[String],
      presentation:String, //TODO
      validityPeriod: Option[Double] = None,
      automaticIssuance: Option[Boolean],
      awaitConfirmation: Option[Boolean]
  ): IO[PresentationError, PresentationRecord]

  def getPresentationRecords(): IO[PresentationError, Seq[PresentationRecord]]

  def getCredentialRecordsByState(
      state: PresentationRecord.ProtocolState
  ): IO[PresentationError, Seq[PresentationRecord]]

  def getPresentationRecord(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def receiveRequestPresentation(request: RequestPresentation): IO[PresentationError, PresentationRecord]

  def acceptRequestPresentation(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def receiveProposePresentation(request: ProposePresentation): IO[PresentationError, Option[PresentationRecord]]

  def acceptProposePresentation(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def receivePresentation(presentation: Presentation): IO[PresentationError, Option[PresentationRecord]]


  def markRequestPresentationSent(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def markProposePresentationSent(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def markPresentationGenerated(
      recordId: UUID,
      presentation: Presentation
  ): IO[PresentationError, Option[PresentationRecord]]

  def markPresentationSent(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

}

object PresentationServiceImpl {
  val layer: URLayer[IrisServiceStub & PresentationRepository[Task] & DidComm, PresentationService] =
    ZLayer.fromFunction(PresentationServiceImpl(_, _, _))
}

private class PresentationServiceImpl(
    irisClient: IrisServiceStub,
    PresentationRepository: PresentationRepository[Task],
    didComm: DidComm
) extends PresentationService {

  import PresentationRecord._

  override def extractIdFromCredential(credential: W3cCredentialPayload): Option[UUID] =
    credential.maybeId.map(_.split("/").last).map(UUID.fromString)

  override def getPresentationRecords(): IO[PresentationError, Seq[PresentationRecord]] = {
    for {
      records <- PresentationRepository
        .getPresentationRecords()
        .mapError(RepositoryError.apply)
    } yield records
  }

  override def getPresentationRecord(recordId: UUID): IO[PresentationError, Option[PresentationRecord]] = {
    for {
      record <- PresentationRepository
        .getPresentationRecord(recordId)
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def createPresentationRecord(
      thid: UUID,
      subjectId: String,
      schemaId: Option[String],
      presentation:String, //TODO
      validityPeriod: Option[Double],
      automaticIssuance: Option[Boolean],
      awaitConfirmation: Option[Boolean]
  ): IO[PresentationError, PresentationRecord] = {
    for {
      request <- ZIO.succeed(createDidCommRequestPresentation(presentation, thid, subjectId))
      record <- ZIO.succeed(
        PresentationRecord(
          id = UUID.randomUUID(),
          createdAt = Instant.now,
          updatedAt = None,
          thid = thid,
          schemaId = schemaId,
          role = PresentationRecord.Role.Verifier,
          subjectId = subjectId,
          protocolState = PresentationRecord.ProtocolState.RequestPending,
          requestPresentationData = Some(request),
          proposePresentationData = None,
          presentationData = None
        )
      )
      count <- PresentationRepository
        .createPresentationRecord(record)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def getCredentialRecordsByState(
      state: PresentationRecord.ProtocolState
  ): IO[PresentationError, Seq[PresentationRecord]] = {
    for {
      records <- PresentationRepository
        .getPresentationRecordsByState(state)
        .mapError(RepositoryError.apply)
    } yield records
  }

  override def receiveRequestPresentation(
      request: RequestPresentation
  ): IO[PresentationError, PresentationRecord] = {
    for {
      record <- ZIO.succeed(
        PresentationRecord(
          id = UUID.randomUUID(),
          createdAt = Instant.now,
          updatedAt = None,
          thid = UUID.fromString(request.thid.getOrElse(request.id)),
          schemaId = None,
          role = Role.Prover,
          subjectId = request.to.value,
          protocolState = PresentationRecord.ProtocolState.RequestReceived,
          requestPresentationData = Some(request),
          proposePresentationData = None,
          presentationData = None
        )
      )
      count <- PresentationRepository
        .createPresentationRecord(record)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def acceptRequestPresentation(recordId: UUID): IO[PresentationError, Option[PresentationRecord]] = {
    for {
      maybeRecord <- PresentationRepository
        .getPresentationRecord(recordId)
        .mapError(RepositoryError.apply)
      record <- ZIO
        .fromOption(maybeRecord)
        .mapError(_ => RecordIdNotFound(recordId))
      presentationRequest <- ZIO
        .fromOption(record.requestPresentationData)
        .mapError(_ => InvalidFlowStateError(s"No request found for this record: $recordId"))
      request = createDidCommPresentation(presentationRequest)
      count <- PresentationRepository
        .updateWithPresentation(recordId, request, ProtocolState.PresentationPending)
        .mapError(RepositoryError.apply)
      _ <- count match
        case 1 => ZIO.succeed(())
        case n => ZIO.fail(RecordIdNotFound(recordId))
      record <- PresentationRepository
        .getPresentationRecord(record.id)
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def receivePresentation(
      presentation: Presentation
  ): IO[PresentationError, Option[PresentationRecord]] = {
    for {
      record <- getRecordFromThreadId(presentation.thid)
      _ <- PresentationRepository
        .updateWithPresentation(record.id, presentation, ProtocolState.PresentationReceived)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
      record <- PresentationRepository
        .getPresentationRecord(record.id)
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def acceptProposePresentation(recordId: UUID): IO[PresentationError, Option[PresentationRecord]] = {
    for {
      maybeRecord <- PresentationRepository
        .getPresentationRecord(recordId)
        .mapError(RepositoryError.apply)
      record <- ZIO
        .fromOption(maybeRecord)
        .mapError(_ => RecordIdNotFound(recordId))
      request <- ZIO
        .fromOption(record.proposePresentationData)
        .mapError(_ => InvalidFlowStateError(s"No request found for this record: $recordId"))
      // TODO: Generate the JWT credential and use it to create the Presentation object
      requestPresentation = createDidCommRequestPresentation(request)
      count <- PresentationRepository
        .updateWithRequestPresentation(recordId, requestPresentation, ProtocolState.PresentationPending)
        .mapError(RepositoryError.apply)
      _ <- count match
        case 1 => ZIO.succeed(())
        case n => ZIO.fail(RecordIdNotFound(recordId))
      record <- PresentationRepository
        .getPresentationRecord(record.id)
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def receiveProposePresentation(
      proposePresentation: ProposePresentation
  ): IO[PresentationError, Option[PresentationRecord]] = {
    for {
      record <- getRecordFromThreadId(proposePresentation.thid)
      _ <- PresentationRepository
        .updateWithProposePresentation(record.id, proposePresentation, ProtocolState.ProposalReceived)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
      record <- PresentationRepository
        .getPresentationRecord(record.id)
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def markRequestPresentationSent(recordId: UUID): IO[PresentationError, Option[PresentationRecord]] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.RequestPending,
      PresentationRecord.ProtocolState.RequestSent
    )

  override def markProposePresentationSent(recordId: UUID): IO[PresentationError, Option[PresentationRecord]] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.ProposalPending,
      PresentationRecord.ProtocolState.ProposalSent
    )

  override def markPresentationGenerated(
      recordId: UUID,
      presentation: Presentation
  ): IO[PresentationError, Option[PresentationRecord]] = {
    for {
      count <- PresentationRepository
        .updateWithPresentation(
          recordId,
          presentation,
          PresentationRecord.ProtocolState.PresentationGenerated
        )
        .mapError(RepositoryError.apply)
      _ <- count match
        case 1 => ZIO.succeed(())
        case n => ZIO.fail(RecordIdNotFound(recordId))
      record <- PresentationRepository
        .getPresentationRecord(recordId)
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def markPresentationSent(recordId: UUID): IO[PresentationError, Option[PresentationRecord]] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.PresentationGenerated,
      PresentationRecord.ProtocolState.PresentationSent
    )


  private[this] def getRecordFromThreadId(
      thid: Option[String]
  ): IO[PresentationError, PresentationRecord] = {
    for {
      thid <- ZIO
        .fromOption(thid)
        .mapError(_ => UnexpectedError("No `thid` found in credential request"))
        .map(UUID.fromString)
      maybeRecord <- PresentationRepository
        .getPresentationRecordByThreadId(thid)
        .mapError(RepositoryError.apply)
      record <- ZIO
        .fromOption(maybeRecord)
        .mapError(_ => ThreadIdNotFound(thid))
    } yield record
  }

  private[this] def createDidCommRequestPresentation(
      presentation: String, //FIX ME
      thid: UUID,
      subjectId: String
  ): RequestPresentation = {
    val body = RequestPresentation.Body(goal_code = Some("request"))

    RequestPresentation(
      body = body,
      attachments = Seq(AttachmentDescriptor.buildAttachment(payload = presentation)),
      from = didComm.myDid,
      to = DidId(subjectId),
      thid = Some(thid.toString())
    )
  }

    private[this] def createDidCommRequestPresentation(proposePresentation: ProposePresentation): RequestPresentation = {
      //TODO to review what is needed
    val body = RequestPresentation.Body(goal_code = proposePresentation.body.goal_code)

    RequestPresentation(
      body = body,
      attachments = proposePresentation.attachments,
      from = didComm.myDid,
      to = proposePresentation.from,
      thid = proposePresentation.thid
    )
  }

  private[this] def createDidCommProposePresentation(request: RequestPresentation): ProposePresentation = {
    ProposePresentation(
      body = ProposePresentation.Body(
        goal_code = request.body.goal_code,
        comment = request.body.comment,
        formats = request.body.formats
      ),
      attachments = request.attachments,
      thid = request.thid.orElse(Some(request.id)),
      from = request.to,
      to = request.from
    )
  }

  private[this] def createDidCommPresentation(request: RequestPresentation): Presentation = {
    Presentation(
      body = Presentation.Body(
        goal_code = request.body.goal_code,
        comment = request.body.comment,
        formats = request.body.formats
      ),
      attachments = request.attachments,
      thid = request.thid.orElse(Some(request.id)),
      from = request.to,
      to = request.from
    )
  }

  private[this] def updatePresentationRecordProtocolState(
      id: UUID,
      from: PresentationRecord.ProtocolState,
      to: PresentationRecord.ProtocolState
  ): IO[PresentationError, Option[PresentationRecord]] = {
    for {
      outcome <- PresentationRepository
        .updatePresentationRecordProtocolState(id, from, to)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
      record <- PresentationRepository
        .getPresentationRecord(id)
        .mapError(RepositoryError.apply)
    } yield record
  }



  private def sendCredential(
      jwtCredential: JwtCredentialPayload,
      holderDid: DID,
      inclusionProof: MerkleInclusionProof
  ): Nothing = ???



}
