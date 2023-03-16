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
import io.iohk.atala.pollux.core.model.presentation._
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
import io.iohk.atala.pollux.core.model._
import io.iohk.atala.pollux.core.repository.CredentialRepository
import java.{util => ju}
import cats.syntax.all._
import cats._, cats.data._, cats.implicits._

trait PresentationService {

  def extractIdFromCredential(credential: W3cCredentialPayload): Option[UUID]

  def createPresentationRecord(
      thid: DidCommID,
      subjectDid: DidId,
      connectionId: Option[String],
      proofTypes: Seq[ProofType],
      options: Option[io.iohk.atala.pollux.core.model.presentation.Options]
  ): IO[PresentationError, PresentationRecord]

  def getPresentationRecords(): IO[PresentationError, Seq[PresentationRecord]]

  def createPresentationPayloadFromRecord(
      record: DidCommID,
      issuer: Issuer,
      issuanceDate: Instant
  ): IO[PresentationError, PresentationPayload]

  def getPresentationRecordsByStates(
      ignoreWithZeroRetries: Boolean = true,
      state: PresentationRecord.ProtocolState*
  ): IO[PresentationError, Seq[PresentationRecord]]

  def getPresentationRecord(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]]

  def receiveRequestPresentation(
      connectionId: Option[String],
      request: RequestPresentation
  ): IO[PresentationError, PresentationRecord]

  def acceptRequestPresentation(
      recordId: DidCommID,
      crecentialsToUse: Seq[String]
  ): IO[PresentationError, Option[PresentationRecord]]

  def rejectRequestPresentation(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]]

  def receiveProposePresentation(request: ProposePresentation): IO[PresentationError, Option[PresentationRecord]]

  def acceptProposePresentation(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]]

  def receivePresentation(presentation: Presentation): IO[PresentationError, Option[PresentationRecord]]

  def acceptPresentation(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]]

  def rejectPresentation(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]]

  def markRequestPresentationSent(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]]

  def markRequestPresentationRejected(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]]

  def markProposePresentationSent(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]]

  def markPresentationSent(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]]

  def markPresentationGenerated(
      recordId: DidCommID,
      presentation: Presentation
  ): IO[PresentationError, Option[PresentationRecord]]

  def markPresentationVerified(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]]

  def markPresentationRejected(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]]

  def markPresentationAccepted(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]]

  def markPresentationVerificationFailed(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]]

  def markFailure(recordId: DidCommID, failReason: Option[String]): IO[RepositoryError, Unit]

}

object PresentationServiceImpl {
  val layer: URLayer[PresentationRepository[Task] & CredentialRepository[Task] & DidAgent, PresentationService] =
    ZLayer.fromFunction(PresentationServiceImpl(_, _, _))
}

private class PresentationServiceImpl(
    presentationRepository: PresentationRepository[Task],
    credentialRepository: CredentialRepository[Task],
    didAgent: DidAgent,
    maxRetries: Int = 5, // TODO move to config
) extends PresentationService {

  import PresentationRecord._

  override def markPresentationGenerated(
      recordId: DidCommID,
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
      recordId: DidCommID,
      prover: Issuer,
      issuanceDate: Instant
  ): IO[PresentationError, PresentationPayload] = {

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
      requestPresentation <- ZIO
        .fromOption(record.requestPresentationData)
        .mapError(_ => InvalidFlowStateError(s"RequestPresentation not found: $recordId"))
      issuedValidCredentials <- credentialRepository
        .getValidIssuedCredentials(credentialsToUse.map(DidCommID(_)))
        .mapError(RepositoryError.apply)

      issuedRawCredentials = issuedValidCredentials.flatMap(_.issuedCredentialRaw.map(IssuedCredentialRaw(_)))

      issuedCredentials <- ZIO.fromEither(
        Either.cond(
          issuedRawCredentials.nonEmpty,
          issuedRawCredentials,
          PresentationError.IssuedCredentialNotFoundError(
            new Throwable("No matching issued credentials found in prover db")
          )
        )
      )

      presentationPayload <- createPresentationPayloadFromCredential(issuedCredentials, requestPresentation, prover)
    } yield presentationPayload
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

  override def getPresentationRecord(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]] = {
    for {
      record <- presentationRepository
        .getPresentationRecord(recordId)
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def rejectRequestPresentation(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]] = {
    markRequestPresentationRejected(recordId)
  }
  def rejectPresentation(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]] = {
    markPresentationRejected(recordId)
  }

  override def createPresentationRecord(
      thid: DidCommID,
      subjectId: DidId,
      connectionId: Option[String],
      proofTypes: Seq[ProofType],
      options: Option[io.iohk.atala.pollux.core.model.presentation.Options]
  ): IO[PresentationError, PresentationRecord] = {
    for {
      request <- ZIO.succeed(createDidCommRequestPresentation(proofTypes, thid, subjectId, options))
      record <- ZIO.succeed(
        PresentationRecord(
          id = DidCommID(),
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
          credentialsToUse = None,
          metaRetries = maxRetries,
          metaNextRetry = Some(Instant.now()),
          metaLastFailure = None,
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
      ignoreWithZeroRetries: Boolean = true,
      states: PresentationRecord.ProtocolState*
  ): IO[PresentationError, Seq[PresentationRecord]] = {
    for {
      records <- presentationRepository
        .getPresentationRecordsByStates(ignoreWithZeroRetries, states: _*)
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
          id = DidCommID(),
          createdAt = Instant.now,
          updatedAt = None,
          thid = DidCommID(request.thid.getOrElse(request.id)),
          connectionId = connectionId,
          schemaId = None,
          role = Role.Prover,
          subjectId = request.to,
          protocolState = PresentationRecord.ProtocolState.RequestReceived,
          requestPresentationData = Some(request),
          proposePresentationData = None,
          presentationData = None,
          credentialsToUse = None,
          metaRetries = maxRetries,
          metaNextRetry = Some(Instant.now()),
          metaLastFailure = None,
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
      requestPresentation: RequestPresentation,
      prover: Issuer
  ): IO[PresentationError, PresentationPayload] = {

    val verifiableCredentials =
      issuedCredentials.map { issuedCredential =>
        decode[io.iohk.atala.mercury.model.Base64](issuedCredential.signedCredential).right
          .flatMap(x => Right(new String(java.util.Base64.getDecoder().decode(x.base64))))
          .right
          .flatMap(x => Right(JwtVerifiableCredentialPayload(JWT(x))))
          .left
          .map(err => PresentationDecodingError(new Throwable(s"JsonData decoding error: $err")))
      }.sequence

    val maybePresentationOptions
        : Either[PresentationError, Option[io.iohk.atala.pollux.core.model.presentation.Options]] =
      requestPresentation.attachments.headOption
        .map(attachment =>
          decode[io.iohk.atala.mercury.model.JsonData](attachment.data.asJson.noSpaces)
            .flatMap(data =>
              io.iohk.atala.pollux.core.model.presentation.PresentationAttachment.given_Decoder_PresentationAttachment
                .decodeJson(data.json.asJson)
                .map(_.options)
                .leftMap(err =>
                  PresentationDecodingError(new Throwable(s"PresentationAttachment decoding error: $err"))
                )
            )
            .leftMap(err => PresentationDecodingError(new Throwable(s"JsonData decoding error: $err")))
        )
        .getOrElse(Right(None))

    for {
      maybeOptions <- ZIO.fromEither(maybePresentationOptions)
      vcs <- ZIO.fromEither(verifiableCredentials)
      presentationPayload <-
        ZIO.succeed(
          maybeOptions
            .map { options =>
              W3cPresentationPayload(
                `@context` = Vector("https://www.w3.org/2018/presentations/v1"),
                maybeId = None,
                `type` = Vector("VerifiablePresentation"),
                verifiableCredential = vcs.toVector,
                holder = prover.did.value,
                verifier = Vector(options.domain),
                maybeIssuanceDate = None,
                maybeExpirationDate = None
              ).toJwtPresentationPayload.copy(maybeNonce = Some(options.challenge))
            }
            .getOrElse {
              W3cPresentationPayload(
                `@context` = Vector("https://www.w3.org/2018/presentations/v1"),
                maybeId = None,
                `type` = Vector("VerifiablePresentation"),
                verifiableCredential = vcs.toVector,
                holder = prover.did.value,
                verifier = Vector("https://example.verifier"), // TODO Fix this
                maybeIssuanceDate = None,
                maybeExpirationDate = None
              ).toJwtPresentationPayload
            }
        )
    } yield presentationPayload

  }

  def acceptRequestPresentation(
      recordId: DidCommID,
      credentialsToUse: Seq[String]
  ): IO[PresentationError, Option[PresentationRecord]] = {

    for {
      record <- getRecordWithState(recordId, ProtocolState.RequestReceived)
      issuedValidCredentials <- credentialRepository
        .getValidIssuedCredentials(credentialsToUse.map(DidCommID(_)))
        .mapError(RepositoryError.apply)
      _ <- ZIO.cond(
        (issuedValidCredentials.map(_.subjectId).toSet.size == 1),
        (),
        PresentationError.HolderBindingError(
          s"Creating a Verifiable Presentation for credential with different subject DID is not supported, found : ${issuedValidCredentials
              .map(_.subjectId)}"
        )
      )
      issuedRawCredentials = issuedValidCredentials.flatMap(_.issuedCredentialRaw.map(IssuedCredentialRaw(_)))
      issuedCredentials <- ZIO.fromEither(
        Either.cond(
          issuedRawCredentials.nonEmpty,
          issuedRawCredentials,
          PresentationError.IssuedCredentialNotFoundError(
            new Throwable(s"No matching issued credentials found in prover db from the given: $credentialsToUse")
          )
        )
      )
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

  override def acceptPresentation(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]] = {
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

  override def acceptProposePresentation(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]] = {
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
      recordId: DidCommID,
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

  override def markRequestPresentationSent(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.RequestPending,
      PresentationRecord.ProtocolState.RequestSent
    )

  override def markProposePresentationSent(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.ProposalPending,
      PresentationRecord.ProtocolState.ProposalSent
    )
  override def markPresentationVerified(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.PresentationReceived,
      PresentationRecord.ProtocolState.PresentationVerified
    )

  override def markPresentationAccepted(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.PresentationVerified,
      PresentationRecord.ProtocolState.PresentationAccepted
    )

  override def markPresentationSent(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.PresentationGenerated,
      PresentationRecord.ProtocolState.PresentationSent
    )

  override def markPresentationRejected(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.PresentationReceived,
      PresentationRecord.ProtocolState.PresentationRejected
    )

  override def markRequestPresentationRejected(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.RequestReceived,
      PresentationRecord.ProtocolState.RequestRejected
    )

  override def markPresentationVerificationFailed(
      recordId: DidCommID
  ): IO[PresentationError, Option[PresentationRecord]] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.PresentationReceived,
      PresentationRecord.ProtocolState.PresentationVerificationFailed
    )

  def markFailure(
      recordId: DidCommID,
      failReason: Option[String]
  ): ZIO[Any, RepositoryError, Unit] = {
    for {
      outcome <- presentationRepository
        .updateAfterFail(recordId, failReason)
        .tapError(ex => ZIO.logError(s"Failure in $recordId not registered:" + ex.getMessage()))
        .mapError(RepositoryError.apply)
    } yield ()
  }

  private[this] def getRecordFromThreadId(
      thid: Option[String]
  ): IO[PresentationError, PresentationRecord] = {
    for {
      thidID <- ZIO
        .fromOption(thid)
        .map(DidCommID(_))
        .mapError(_ => UnexpectedError("No `thid` found in Presentation request"))
      maybeRecord <- presentationRepository
        .getPresentationRecordByThreadId(thidID)
        .mapError(RepositoryError.apply)
      record <- ZIO
        .fromOption(maybeRecord)
        .mapError(_ => ThreadIdNotFound(thidID))
    } yield record
  }

  private[this] def createDidCommRequestPresentation(
      proofTypes: Seq[ProofType],
      thid: DidCommID,
      subjectDId: DidId,
      maybeOptions: Option[io.iohk.atala.pollux.core.model.presentation.Options]
  ): RequestPresentation = {
    RequestPresentation(
      body = RequestPresentation.Body(
        goal_code = Some("request"),
        proof_types = proofTypes
      ),
      attachments = maybeOptions
        .map(options =>
          Seq(
            AttachmentDescriptor.buildJsonAttachment(payload =
              io.iohk.atala.pollux.core.model.presentation.PresentationAttachment.build(Some(options))
            )
          )
        )
        .getOrElse(Seq.empty),
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
      id: DidCommID,
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
