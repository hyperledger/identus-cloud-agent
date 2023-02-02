package io.iohk.atala.pollux.core.service

import com.google.protobuf.ByteString
import io.circe.Json
import io.circe.parser._
import io.circe.syntax.*
import io.circe._
import io.iohk.atala.pollux.core.model.EncodedJWTCredential
import io.iohk.atala.pollux.core.model.PresentationRecord
import io.iohk.atala.pollux.core.model.error.PresentationError
import io.iohk.atala.pollux.core.model.error.PresentationError._
import io.iohk.atala.pollux.core.model.IssuedCredentialRaw
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
import io.iohk.atala.mercury.DidAgent
import io.iohk.atala.mercury.model._
import io.iohk.atala.mercury.protocol.presentproof._
import java.time.Instant
import io.iohk.atala.mercury.protocol.presentproof.RequestPresentation
import java.security.PublicKey
import io.iohk.atala.mercury.protocol.issuecredential.IssueCredential
import io.iohk.atala.pollux.core.model.IssueCredentialRecord
import io.iohk.atala.pollux.core.repository.CredentialRepository
import org.didcommx.didcomm.message.Attachment.Data.Base64 //FIXME create interface so we do not have to use it directly
import java.{util => ju}

trait PresentationService {

  def extractIdFromCredential(credential: W3cCredentialPayload): Option[UUID]

  def createPresentationRecord(
      thid: UUID,
      subjectDid: DidId,
      connectionId: Option[String],
      proofTypes: Seq[ProofType]
  ): IO[PresentationError, PresentationRecord]

  def getPresentationRecords(): IO[PresentationError, Seq[PresentationRecord]]

  def createPresentationPayloadFromRecord(
      record: UUID,
      issuer: Issuer,
      issuanceDate: Instant
  ): IO[PresentationError, W3cPresentationPayload]

  def getPresentationRecordsByStates(
      state: PresentationRecord.ProtocolState*
  ): IO[PresentationError, Seq[PresentationRecord]]

  def getPresentationRecord(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def receiveRequestPresentation(
      connectionId: Option[String],
      request: RequestPresentation
  ): IO[PresentationError, PresentationRecord]

  def acceptRequestPresentation(
      recordId: UUID,
      crecentialsToUse: Seq[String]
  ): IO[PresentationError, Option[PresentationRecord]]

  def rejectRequestPresentation(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def receiveProposePresentation(request: ProposePresentation): IO[PresentationError, Option[PresentationRecord]]

  def acceptProposePresentation(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def receivePresentation(presentation: Presentation): IO[PresentationError, Option[PresentationRecord]]

  def acceptPresentation(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def rejectPresentation(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def markRequestPresentationSent(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def markRequestPresentationRejected(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def markProposePresentationSent(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def markPresentationSent(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def markPresentationGenerated(
      recordId: UUID,
      presentation: Presentation
  ): IO[PresentationError, Option[PresentationRecord]]

  def markPresentationVerified(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def markPresentationRejected(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

  def markPresentationAccepted(recordId: UUID): IO[PresentationError, Option[PresentationRecord]]

}

object PresentationServiceImpl {
  val layer: URLayer[PresentationRepository[Task] & CredentialRepository[Task] & DidAgent, PresentationService] =
    ZLayer.fromFunction(PresentationServiceImpl(_, _, _))
}

private class PresentationServiceImpl(
    presentationRepository: PresentationRepository[Task],
    credentialRepository: CredentialRepository[Task],
    didAgent: DidAgent
) extends PresentationService {

  import PresentationRecord._

  override def markPresentationGenerated(
      recordId: UUID,
      presentation: Presentation
  ): IO[PresentationError, Option[PresentationRecord]] = {
    for {
      record <- getRecordWithState(recordId, ProtocolState.PresentationPending)
      count <- presentationRepository
        .updateWithPresentation(recordId, presentation, ProtocolState.PresentationGenerated)
        .mapError(RepositoryError.apply)
      _ <- count match
        case 1 => ZIO.succeed(())
        case n => ZIO.fail(RecordIdNotFound(recordId))
      record <- presentationRepository
        .getPresentationRecord(recordId)
        .mapError(RepositoryError.apply)

    } yield record
  }

  override def createPresentationPayloadFromRecord(
      recordId: UUID,
      prover: Issuer,
      issuanceDate: Instant
  ): IO[PresentationError, W3cPresentationPayload] = {

    for {
      maybeRecord <- presentationRepository
        .getPresentationRecord(recordId)
        .mapError(RepositoryError.apply)
      record <- ZIO
        .fromOption(maybeRecord)
        .mapError(_ => RecordIdNotFound(recordId))
      _ <- ZIO.log(record.toString())
      credentialsToUse <- ZIO
        .fromOption(record.credentialsToUse)
        .mapError(_ => InvalidFlowStateError(s"No request found for this record: $recordId"))

      issuedValidCredentials <- credentialRepository
        .getValidIssuedCredentials(credentialsToUse.map(UUID.fromString))
        .mapError(RepositoryError.apply)

      issuedRawCredentials = issuedValidCredentials.map(_.issuedCredentialRaw.map(IssuedCredentialRaw(_))).flatten
      issuedCredentials <- ZIO.fromEither(
        Either.cond(
          issuedRawCredentials.nonEmpty,
          issuedRawCredentials,
          PresentationError.IssuedCredentialNotFoundError(
            new Throwable("No matching issued credentials foound in prover db")
          )
        )
      )

      w3cPresentationPayload <- createPresentationPayloadFromCredential(issuedCredentials, prover)
    } yield w3cPresentationPayload
  }

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
      proofTypes: Seq[ProofType]
  ): IO[PresentationError, PresentationRecord] = {
    for {
      request <- ZIO.succeed(createDidCommRequestPresentation(proofTypes, thid, subjectId))
      record <- ZIO.succeed(
        PresentationRecord(
          id = UUID.randomUUID(),
          createdAt = Instant.now,
          updatedAt = None,
          thid = thid,
          connectionId = connectionId,
          schemaId = None, // TODO REMOVE from DB
          role = PresentationRecord.Role.Verifier,
          subjectId = subjectId,
          protocolState = PresentationRecord.ProtocolState.RequestPending,
          requestPresentationData = Some(request),
          proposePresentationData = None,
          presentationData = None,
          credentialsToUse = None
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

  override def getPresentationRecordsByStates(
      states: PresentationRecord.ProtocolState*
  ): IO[PresentationError, Seq[PresentationRecord]] = {
    for {
      records <- presentationRepository
        .getPresentationRecordsByStates(states: _*)
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
          presentationData = None,
          credentialsToUse = None
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

  private def createPresentationPayloadFromCredential(
      issuedCredentials: Seq[IssuedCredentialRaw],
      prover: Issuer
  ): IO[PresentationError, W3cPresentationPayload] = {

    val verifiableCredentials = issuedCredentials.map { issuedCredential =>
      decode[io.iohk.atala.mercury.model.Base64](issuedCredential.signedCredential)
        .map(x => new String(java.util.Base64.getDecoder().decode(x.base64)))
        .map(_.drop(1).dropRight(1))
        .map(x => JwtVerifiableCredentialPayload(JWT(x)))
        .getOrElse(???)
    }.toVector

    val w3cPresentationPayload =
      W3cPresentationPayload(
        `@context` = Vector("https://www.w3.org/2018/presentations/v1"),
        maybeId = None,
        `type` = Vector("VerifiablePresentation"),
        verifiableCredential = verifiableCredentials,
        holder = prover.did.value,
        verifier = Vector("https://example.edu/issuers/565049"), // TODO Fix this
        maybeIssuanceDate = None,
        maybeExpirationDate = None
      )
    ZIO.succeed(w3cPresentationPayload)
  }

  override def acceptRequestPresentation(
      recordId: UUID,
      credentialsToUse: Seq[String]
  ): IO[PresentationError, Option[PresentationRecord]] = {

    for {
      record <- getRecordWithState(recordId, ProtocolState.RequestReceived)
      count <- presentationRepository
        .updatePresentationWithCredentialsToUse(recordId, Option(credentialsToUse), ProtocolState.PresentationPending)
        .mapError(RepositoryError.apply)
      _ <- count match
        case 1 => ZIO.succeed(())
        case n => ZIO.fail(RecordIdNotFound(recordId))
      record <- presentationRepository
        .getPresentationRecord(recordId)
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

      recordUpdated <- markPresentationAccepted(record.id)

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

  private[this] def getRecordWithState(
      recordId: UUID,
      state: ProtocolState
  ): IO[PresentationError, PresentationRecord] = {
    for {
      maybeRecord <- presentationRepository
        .getPresentationRecord(recordId)
        .mapError(RepositoryError.apply)
      record <- ZIO
        .fromOption(maybeRecord)
        .mapError(_ => RecordIdNotFound(recordId))
      _ <- record.protocolState match {
        case s if s == state => ZIO.unit
        case state           => ZIO.fail(InvalidFlowStateError(s"Invalid protocol state for operation: $state"))
      }
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

  override def markPresentationAccepted(recordId: UUID): IO[PresentationError, Option[PresentationRecord]] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.PresentationVerified,
      PresentationRecord.ProtocolState.PresentationAccepted
    )

  override def markPresentationSent(recordId: UUID): IO[PresentationError, Option[PresentationRecord]] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.PresentationGenerated,
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

  private[this] def createDidCommRequestPresentation(
      proofTypes: Seq[ProofType],
      thid: UUID,
      subjectDId: DidId
  ): RequestPresentation = {
    RequestPresentation(
      body = RequestPresentation.Body(
        goal_code = Some("request"),
        proof_types = proofTypes
      ),
      attachments = Seq.empty,
      from = didAgent.id,
      to = subjectDId,
      thid = Some(thid.toString)
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
      from = didAgent.id,
      to = proposePresentation.from,
      thid = proposePresentation.thid
    )
  }

  private[this] def createDidCommProposePresentation(request: RequestPresentation): ProposePresentation = {
    ProposePresentation(
      body = ProposePresentation.Body(
        goal_code = request.body.goal_code,
        comment = request.body.comment
      ),
      attachments = request.attachments,
      thid = request.thid.orElse(Some(request.id)),
      from = request.to,
      to = request.from
    )
  }

  import io.iohk.atala.pollux.vc.jwt.CredentialPayload.Implicits._

  private[this] def createDidCommPresentation(
      request: RequestPresentation,
      jwtPresentation: JWT
  ): Presentation = {

    Presentation(
      body = Presentation.Body(
        goal_code = request.body.goal_code,
        comment = request.body.comment
      ),
      attachments = Seq(
        AttachmentDescriptor
          .buildJsonAttachment(payload = jwtPresentation.value, mediaType = Some("prism/jwt"))
      ),
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
