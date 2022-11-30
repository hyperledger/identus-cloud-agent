package io.iohk.atala.pollux.core.service

import com.google.protobuf.ByteString
import io.circe.Json
import io.circe.syntax.*
import io.iohk.atala.pollux.core.model.EncodedJWTCredential
import io.iohk.atala.pollux.core.model.PresentationRecord
import io.iohk.atala.pollux.core.model.error.PresentationError
import io.iohk.atala.pollux.core.model.error.PresentationError._
import io.iohk.atala.pollux.core.repository.PresentationRepository
import io.iohk.atala.pollux.vc.jwt.*
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
      subjectDid: DidId,
      connectionId: Option[String],
      schemaId: Option[String]
  ): IO[PresentationError, PresentationRecord]

  def getPresentationRecords(): IO[PresentationError, Seq[PresentationRecord]]

  def getCredentialRecordsByState(
      state: PresentationRecord.ProtocolState
  ): IO[PresentationError, Seq[PresentationRecord]]

  def getPresentationRecord(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def receiveRequestPresentation(
      connectionId: Option[String],
      request: RequestPresentation
  ): IO[PresentationError, PresentationRecord]

  def acceptRequestPresentation(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def rejectRequestPresentation(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def receiveProposePresentation(request: ProposePresentation): IO[PresentationError, Option[PresentationRecord]]

  def acceptProposePresentation(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def receivePresentation(presentation: Presentation): IO[PresentationError, Option[PresentationRecord]]

  def acceptPresentation(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def rejectPresentation(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def markRequestPresentationSent(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def markRequestPresentationRejected(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def markProposePresentationSent(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def markPresentationGenerated(
      recordId: UUID,
      presentation: Presentation
  ): IO[PresentationError, Option[PresentationRecord]]

  def markPresentationSent(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def markPresentationVerified(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def markPresentationRejected(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

}

object PresentationServiceImpl {
  val layer: URLayer[PresentationRepository[Task] & DidComm, PresentationService] =
    ZLayer.fromFunction(PresentationServiceImpl(_, _))
}

private class PresentationServiceImpl(
    presentationRepository: PresentationRepository[Task],
    didComm: DidComm
) extends PresentationService {

  import PresentationRecord._

  override def extractIdFromCredential(credential: W3cCredentialPayload): Option[UUID] =
    credential.maybeId.map(_.split("/").last).map(UUID.fromString)

  override def getPresentationRecords(): IO[PresentationError, Seq[PresentationRecord]] = {
    for {
      records <- presentationRepository
        .getPresentationRecords()
        .mapError(RepositoryError.apply)
    } yield records
  }

  override def getPresentationRecord(recordId: UUID): IO[PresentationError, Option[PresentationRecord]] = {
    for {
      record <- presentationRepository
        .getPresentationRecord(recordId)
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def rejectRequestPresentation(recordId: UUID): IO[PresentationError, Option[PresentationRecord]] = {
    markRequestPresentationRejected(recordId)
  }
  def rejectPresentation(recordId: UUID): IO[PresentationError, Option[PresentationRecord]] = {
    markPresentationRejected(recordId)
  }

  override def createPresentationRecord(
      thid: UUID,
      subjectId: DidId,
      connectionId: Option[String],
      schemaId: Option[String]
  ): IO[PresentationError, PresentationRecord] = {
    for {
      request <- ZIO.succeed(createDidCommRequestPresentation(schemaId, thid, subjectId))
      record <- ZIO.succeed(
        PresentationRecord(
          id = UUID.randomUUID(),
          createdAt = Instant.now,
          updatedAt = None,
          thid = thid,
          connectionId = connectionId,
          schemaId = schemaId,
          role = PresentationRecord.Role.Verifier,
          subjectId = subjectId,
          protocolState = PresentationRecord.ProtocolState.RequestPending,
          requestPresentationData = Some(request),
          proposePresentationData = None,
          presentationData = None
        )
      )
      count <- presentationRepository
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
      records <- presentationRepository
        .getPresentationRecordsByState(state)
        .mapError(RepositoryError.apply)
    } yield records
  }

  override def receiveRequestPresentation(
      connectionId: Option[String],
      request: RequestPresentation
  ): IO[PresentationError, PresentationRecord] = {
    for {
      record <- ZIO.succeed(
        PresentationRecord(
          id = UUID.randomUUID(),
          createdAt = Instant.now,
          updatedAt = None,
          thid = UUID.fromString(request.thid.getOrElse(request.id)),
          connectionId = connectionId,
          schemaId = None,
          role = Role.Prover,
          subjectId = request.to,
          protocolState = PresentationRecord.ProtocolState.RequestReceived,
          requestPresentationData = Some(request),
          proposePresentationData = None,
          presentationData = None
        )
      )
      count <- presentationRepository
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
      maybeRecord <- presentationRepository
        .getPresentationRecord(recordId)
        .mapError(RepositoryError.apply)
      record <- ZIO
        .fromOption(maybeRecord)
        .mapError(_ => RecordIdNotFound(recordId))
      _ <- ZIO.log(record.toString())

      presentationRequest <- ZIO
        .fromOption(record.requestPresentationData)
        .mapError(_ => InvalidFlowStateError(s"No request found for this record: $recordId"))

      request = createDidCommPresentation(presentationRequest)

      count <- presentationRepository
        .updateWithPresentation(recordId, request, ProtocolState.PresentationPending)
        .mapError(RepositoryError.apply)
      _ <- count match
        case 1 => ZIO.succeed(())
        case n => ZIO.fail(RecordIdNotFound(recordId))
      record <- presentationRepository
        .getPresentationRecord(record.id)
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def acceptPresentation(recordId: UUID): IO[PresentationError, Option[PresentationRecord]] = {
    for {
      maybeRecord <- presentationRepository
        .getPresentationRecord(recordId)
        .mapError(RepositoryError.apply)
      record <- ZIO
        .fromOption(maybeRecord)
        .mapError(_ => RecordIdNotFound(recordId))
      _ <- ZIO.log(record.toString())

      presentationRequest <- ZIO
        .fromOption(record.presentationData)
        .mapError(_ => InvalidFlowStateError(s"No request found for this record: $recordId"))
      _ <- ZIO.log(s"************presentationRequest*************$presentationRequest")
      _ <- verifyPresentation(presentationRequest) // TODO
      recordUpdated <- markPresentationVerified(record.id)

    } yield recordUpdated
  }
  override def receivePresentation(
      presentation: Presentation
  ): IO[PresentationError, Option[PresentationRecord]] = {
    for {
      record <- getRecordFromThreadId(presentation.thid)
      _ <- presentationRepository
        .updateWithPresentation(record.id, presentation, ProtocolState.PresentationReceived)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
      record <- presentationRepository
        .getPresentationRecord(record.id)
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def acceptProposePresentation(recordId: UUID): IO[PresentationError, Option[PresentationRecord]] = {
    for {
      maybeRecord <- presentationRepository
        .getPresentationRecord(recordId)
        .mapError(RepositoryError.apply)
      record <- ZIO
        .fromOption(maybeRecord)
        .mapError(_ => RecordIdNotFound(recordId))
      request <- ZIO
        .fromOption(record.proposePresentationData)
        .mapError(_ => InvalidFlowStateError(s"No request found for this record: $recordId"))
      // TODO: Generate the JWT credential and use it to create the Presentation object
      requestPresentation = createDidCommRequestPresentationFromProposal(request)
      count <- presentationRepository
        .updateWithRequestPresentation(recordId, requestPresentation, ProtocolState.PresentationPending)
        .mapError(RepositoryError.apply)
      _ <- count match
        case 1 => ZIO.succeed(())
        case n => ZIO.fail(RecordIdNotFound(recordId))
      record <- presentationRepository
        .getPresentationRecord(record.id)
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def receiveProposePresentation(
      proposePresentation: ProposePresentation
  ): IO[PresentationError, Option[PresentationRecord]] = {
    for {
      record <- getRecordFromThreadId(proposePresentation.thid)
      _ <- presentationRepository
        .updateWithProposePresentation(record.id, proposePresentation, ProtocolState.ProposalReceived)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
      record <- presentationRepository
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
  override def markPresentationVerified(recordId: UUID): IO[PresentationError, Option[PresentationRecord]] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.PresentationReceived,
      PresentationRecord.ProtocolState.PresentationVerified
    )

  override def markPresentationGenerated(
      recordId: UUID,
      presentation: Presentation
  ): IO[PresentationError, Option[PresentationRecord]] = {
    for {
      count <- presentationRepository
        .updateWithPresentation(
          recordId,
          presentation,
          PresentationRecord.ProtocolState.PresentationGenerated
        )
        .mapError(RepositoryError.apply)
      _ <- count match
        case 1 => ZIO.succeed(())
        case n => ZIO.fail(RecordIdNotFound(recordId))
      record <- presentationRepository
        .getPresentationRecord(recordId)
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def markPresentationSent(recordId: UUID): IO[PresentationError, Option[PresentationRecord]] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.PresentationPending,
      PresentationRecord.ProtocolState.PresentationSent
    )

  override def markPresentationRejected(recordId: UUID): IO[PresentationError, Option[PresentationRecord]] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.PresentationReceived,
      PresentationRecord.ProtocolState.PresentationRejected
    )

  override def markRequestPresentationRejected(recordId: UUID): IO[PresentationError, Option[PresentationRecord]] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.RequestReceived,
      PresentationRecord.ProtocolState.RequestRejected
    )

  private[this] def getRecordFromThreadId(
      thid: Option[String]
  ): IO[PresentationError, PresentationRecord] = {
    for {
      thid <- ZIO
        .fromOption(thid)
        .mapError(_ => UnexpectedError("No `thid` found in Presentation request"))
        .map(UUID.fromString)
      maybeRecord <- presentationRepository
        .getPresentationRecordByThreadId(thid)
        .mapError(RepositoryError.apply)
      record <- ZIO
        .fromOption(maybeRecord)
        .mapError(_ => ThreadIdNotFound(thid))
    } yield record
  }

  private[this] def verifyPresentation(
      presentation: Presentation
  ) = {
    for {
      _ <- ZIO.log(s"************Verify Presentation Not Implemented*************")
    } yield ()
  }

  private[this] def createDidCommRequestPresentation(
      schemaId: Option[String], // TODO Presentation Formats
      thid: UUID,
      subjectDId: DidId
  ): RequestPresentation = {
    val body = RequestPresentation.Body(goal_code = Some("request"))

    RequestPresentation(
      body = body,
      attachments = Seq(AttachmentDescriptor.buildAttachment(payload = schemaId)),
      from = didComm.myDid,
      to = subjectDId,
      thid = Some(thid.toString())
    )
  }

  private[this] def createDidCommRequestPresentationFromProposal(
      proposePresentation: ProposePresentation
  ): RequestPresentation = {
    // TODO to review what is needed
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
      outcome <- presentationRepository
        .updatePresentationRecordProtocolState(id, from, to)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
      record <- presentationRepository
        .getPresentationRecord(id)
        .mapError(RepositoryError.apply)
    } yield record
  }

}
