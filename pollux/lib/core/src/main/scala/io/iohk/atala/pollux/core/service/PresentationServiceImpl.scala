package io.iohk.atala.pollux.core.service

import cats.*
import cats.implicits.*
import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
import io.iohk.atala.mercury.model.*
import io.iohk.atala.mercury.protocol.issuecredential.{IssueCredential, IssueCredentialIssuedFormat}
import io.iohk.atala.mercury.protocol.presentproof.*
import io.iohk.atala.pollux.anoncreds.*
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.error.PresentationError
import io.iohk.atala.pollux.core.model.error.PresentationError.*
import io.iohk.atala.pollux.core.model.presentation.*
import io.iohk.atala.pollux.core.model.schema.CredentialSchema.parseCredentialSchema
import io.iohk.atala.pollux.core.model.schema.`type`.anoncred.AnoncredSchemaSerDesV1
import io.iohk.atala.pollux.core.repository.{CredentialRepository, PresentationRepository}
import io.iohk.atala.pollux.core.service.serdes.AnoncredPresentationRequestV1
import io.iohk.atala.pollux.vc.jwt.*
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.utils.aspects.CustomMetricsAspect
import zio.*

import java.net.URI
import java.rmi.UnexpectedException
import java.time.Instant
import java.util as ju
import java.util.{UUID, Base64 as JBase64}

private class PresentationServiceImpl(
    credentialDefinitionService: CredentialDefinitionService,
    uriDereferencer: URIDereferencer,
    linkSecretService: LinkSecretService,
    presentationRepository: PresentationRepository,
    credentialRepository: CredentialRepository,
    maxRetries: Int = 5, // TODO move to config
) extends PresentationService {

  import PresentationRecord.*

  override def markPresentationGenerated(
      recordId: DidCommID,
      presentation: Presentation
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] = {
    for {
      record <- getRecordWithState(recordId, ProtocolState.PresentationPending)
      count <- presentationRepository
        .updateWithPresentation(recordId, presentation, ProtocolState.PresentationGenerated)
        .mapError(RepositoryError.apply) @@ CustomMetricsAspect.endRecordingTime(
        s"${record.id}_present_proof_flow_prover_presentation_pending_to_generated_ms_gauge",
        "present_proof_flow_prover_presentation_pending_to_generated_ms_gauge"
      ) @@ CustomMetricsAspect.startRecordingTime(
        s"${record.id}_present_proof_flow_prover_presentation_generated_to_sent_ms_gauge"
      )
      _ <- count match
        case 1 => ZIO.succeed(())
        case n => ZIO.fail(RecordIdNotFound(recordId))
      record <- presentationRepository
        .getPresentationRecord(recordId)
        .mapError(RepositoryError.apply)
        .flatMap {
          case None        => ZIO.fail(RecordIdNotFound(record.id))
          case Some(value) => ZIO.succeed(value)
        }
    } yield record
  }

  override def createJwtPresentationPayloadFromRecord(
      recordId: DidCommID,
      prover: Issuer,
      issuanceDate: Instant
  ): ZIO[WalletAccessContext, PresentationError, PresentationPayload] = {

    for {
      maybeRecord <- presentationRepository
        .getPresentationRecord(recordId)
        .mapError(RepositoryError.apply)
      record <- ZIO
        .fromOption(maybeRecord)
        .mapError(_ => RecordIdNotFound(recordId))
      credentialsToUse <- ZIO
        .fromOption(record.credentialsToUse)
        .mapError(_ => InvalidFlowStateError(s"No request found for this record: $recordId"))
      requestPresentation <- ZIO
        .fromOption(record.requestPresentationData)
        .mapError(_ => InvalidFlowStateError(s"RequestPresentation not found: $recordId"))
      issuedValidCredentials <- credentialRepository
        .getValidIssuedCredentials(credentialsToUse.map(DidCommID(_)))
        .mapError(RepositoryError.apply)
      signedCredentials = issuedValidCredentials.flatMap(_.issuedCredentialRaw)
      issuedCredentials <- ZIO.fromEither(
        Either.cond(
          signedCredentials.nonEmpty,
          signedCredentials,
          PresentationError.IssuedCredentialNotFoundError(
            new Throwable("No matching issued credentials found in prover db")
          )
        )
      )

      presentationPayload <- createJwtPresentationPayloadFromCredential(
        issuedCredentials,
        requestPresentation,
        prover
      )
    } yield presentationPayload
  }

  override def createAnoncredPresentationPayloadFromRecord(
      recordId: DidCommID,
      prover: Issuer,
      issuanceDate: Instant
  ): ZIO[WalletAccessContext, PresentationError, AnoncredPresentation] = {

    for {
      maybeRecord <- presentationRepository
        .getPresentationRecord(recordId)
        .mapError(RepositoryError.apply)
      record <- ZIO
        .fromOption(maybeRecord)
        .mapError(_ => RecordIdNotFound(recordId))
      credentialsToUse <- ZIO
        .fromOption(record.credentialsToUse)
        .mapError(_ => InvalidFlowStateError(s"No request found for this record: $recordId"))
      requestPresentation <- ZIO
        .fromOption(record.requestPresentationData)
        .mapError(_ => InvalidFlowStateError(s"RequestPresentation not found: $recordId"))
      issuedValidCredentials <- credentialRepository
        .getValidAnoncredIssuedCredentials(credentialsToUse.map(DidCommID(_)))
        .mapError(RepositoryError.apply)
      signedCredentials = issuedValidCredentials.flatMap(_.issuedCredential)
      issuedCredentials <- ZIO.fromEither(
        Either.cond(
          signedCredentials.nonEmpty,
          signedCredentials,
          PresentationError.IssuedCredentialNotFoundError(
            new Throwable("No matching issued credentials found in prover db")
          )
        )
      )
      presentationPayload <- createAnoncredPresentationPayloadFromCredential(
        issuedCredentials,
        issuedValidCredentials.flatMap(_.schemaId),
        issuedValidCredentials.flatMap(_.credentialDefinitionId),
        requestPresentation,
      )
    } yield presentationPayload
  }

  override def extractIdFromCredential(credential: W3cCredentialPayload): Option[UUID] =
    credential.maybeId.map(_.split("/").last).map(UUID.fromString)

  override def getPresentationRecords(
      ignoreWithZeroRetries: Boolean
  ): ZIO[WalletAccessContext, PresentationError, Seq[PresentationRecord]] = {
    for {
      records <- presentationRepository
        .getPresentationRecords(ignoreWithZeroRetries)
        .mapError(RepositoryError.apply)
    } yield records
  }

  override def getPresentationRecord(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, Option[PresentationRecord]] = {
    for {
      record <- presentationRepository
        .getPresentationRecord(recordId)
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def getPresentationRecordByThreadId(
      thid: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, Option[PresentationRecord]] =
    for {
      record <- presentationRepository
        .getPresentationRecordByThreadId(thid)
        .mapError(RepositoryError.apply)
    } yield record

  override def rejectRequestPresentation(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] = {
    markRequestPresentationRejected(recordId)
  }

  def rejectPresentation(recordId: DidCommID): ZIO[WalletAccessContext, PresentationError, PresentationRecord] = {
    markPresentationRejected(recordId)
  }

  override def createJwtPresentationRecord(
      pairwiseVerifierDID: DidId,
      pairwiseProverDID: DidId,
      thid: DidCommID,
      connectionId: Option[String],
      proofTypes: Seq[ProofType],
      maybeOptions: Option[io.iohk.atala.pollux.core.model.presentation.Options]
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] = {
    for {
      request <- ZIO.succeed(
        createDidCommRequestPresentation(
          proofTypes,
          thid,
          pairwiseVerifierDID,
          pairwiseProverDID,
          maybeOptions.map(options => Seq(toJWTAttachment(options))).getOrElse(Seq.empty)
        )
      )
      record <- ZIO.succeed(
        PresentationRecord(
          id = DidCommID(),
          createdAt = Instant.now,
          updatedAt = None,
          thid = thid,
          connectionId = connectionId,
          schemaId = None, // TODO REMOVE from DB
          role = PresentationRecord.Role.Verifier,
          subjectId = pairwiseProverDID,
          protocolState = PresentationRecord.ProtocolState.RequestPending,
          credentialFormat = CredentialFormat.JWT,
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
        .mapError(RepositoryError.apply) @@ CustomMetricsAspect.startRecordingTime(
        s"${record.id}_present_proof_flow_verifier_req_pending_to_sent_ms_gauge"
      )
    } yield record
  }

  override def createAnoncredPresentationRecord(
      pairwiseVerifierDID: DidId,
      pairwiseProverDID: DidId,
      thid: DidCommID,
      connectionId: Option[String],
      presentationRequest: AnoncredPresentationRequestV1
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] = {
    for {
      request <- ZIO.succeed(
        createDidCommRequestPresentation(
          Seq.empty,
          thid,
          pairwiseVerifierDID,
          pairwiseProverDID,
          Seq(toAnoncredAttachment(presentationRequest))
        )
      )
      record <- ZIO.succeed(
        PresentationRecord(
          id = DidCommID(),
          createdAt = Instant.now,
          updatedAt = None,
          thid = thid,
          connectionId = connectionId,
          schemaId = None, // TODO REMOVE from DB
          role = PresentationRecord.Role.Verifier,
          subjectId = pairwiseProverDID,
          protocolState = PresentationRecord.ProtocolState.RequestPending,
          credentialFormat = CredentialFormat.AnonCreds,
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
        .mapError(RepositoryError.apply) @@ CustomMetricsAspect.startRecordingTime(
        s"${record.id}_present_proof_flow_verifier_req_pending_to_sent_ms_gauge"
      )
    } yield record
  }

  override def getPresentationRecordsByStates(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: PresentationRecord.ProtocolState*
  ): ZIO[WalletAccessContext, PresentationError, Seq[PresentationRecord]] = {
    for {
      records <- presentationRepository
        .getPresentationRecordsByStates(ignoreWithZeroRetries, limit, states: _*)
        .mapError(RepositoryError.apply)
    } yield records
  }

  override def getPresentationRecordsByStatesForAllWallets(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: PresentationRecord.ProtocolState*
  ): IO[PresentationError, Seq[PresentationRecord]] = {
    for {
      records <- presentationRepository
        .getPresentationRecordsByStatesForAllWallets(ignoreWithZeroRetries, limit, states: _*)
        .mapError(RepositoryError.apply)
    } yield records
  }

  override def receiveRequestPresentation(
      connectionId: Option[String],
      request: RequestPresentation
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] = {
    for {
      format <- request.attachments match {
        case Seq() => ZIO.fail(PresentationError.MissingCredential)
        case Seq(head) =>
          val jsonF = PresentCredentialRequestFormat.JWT.name // stable identifier
          val anoncredF = PresentCredentialRequestFormat.Anoncred.name // stable identifier
          head.format match
            case None          => ZIO.fail(PresentationError.MissingCredentialFormat)
            case Some(`jsonF`) => ZIO.succeed(CredentialFormat.JWT)
            case Some(`anoncredF`) =>
              head.data match
                case Base64(data) =>
                  val decodedData = new String(JBase64.getUrlDecoder.decode(data))
                  AnoncredPresentationRequestV1.schemaSerDes
                    .validate(decodedData)
                    .map(_ => CredentialFormat.AnonCreds)
                    .mapError(error => InvalidAnoncredPresentationRequest(error.error))
                case _ => ZIO.fail(InvalidAnoncredPresentationRequest("Expecting Base64-encoded data"))
            case Some(unsupportedFormat) => ZIO.fail(PresentationError.UnsupportedCredentialFormat(unsupportedFormat))
        case _ => ZIO.fail(PresentationError.UnexpectedError("Presentation with multi attachments"))
      }
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
          credentialFormat = format,
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

  /** All credentials MUST be of the same format */
  private def createJwtPresentationPayloadFromCredential(
      issuedCredentials: Seq[String],
      requestPresentation: RequestPresentation,
      prover: Issuer
  ): IO[PresentationError, PresentationPayload] = {

    val verifiableCredentials: Either[
      PresentationError.PresentationDecodingError,
      Seq[JwtVerifiableCredentialPayload]
    ] =
      issuedCredentials.map { signedCredential =>
        decode[io.iohk.atala.mercury.model.Base64](signedCredential)
          .flatMap(x => Right(new String(java.util.Base64.getDecoder.decode(x.base64))))
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

  private def createAnoncredPresentationPayloadFromCredential(
      issuedCredentials: Seq[IssueCredential],
      schemaIds: Seq[String],
      credentialDefinitionIds: Seq[UUID],
      requestPresentation: RequestPresentation,
  ): ZIO[WalletAccessContext, PresentationError, AnoncredPresentation] = {
    for {
      schemaMap <-
        ZIO
          .collectAll(schemaIds.map { schemaId =>
            resolveSchema(schemaId)
          })
          .map(_.toMap)
      credentialDefinitionMap <-
        ZIO
          .collectAll(credentialDefinitionIds.map { credentialDefinitionId =>
            for {
              credentialDefinition <- credentialDefinitionService
                .getByGUID(credentialDefinitionId)
                .mapError(e => UnexpectedError(e.toString))
            } yield (credentialDefinition.longId, CredentialDefinition(credentialDefinition.definition.toString))
          })
          .map(_.toMap)

      verifiableCredentials <-
        ZIO.collectAll(
          issuedCredentials
            .flatMap(_.attachments)
            .filter(attachment => attachment.format.contains(IssueCredentialIssuedFormat.Anoncred.name))
            .map(_.data)
            .map {
              case Base64(data) => Right(new String(JBase64.getUrlDecoder.decode(data)))
              case _            => Left(InvalidAnoncredPresentationRequest("Expecting Base64-encoded data"))
            }
            .map(ZIO.fromEither(_))
        )
      presentationRequestAttachment <- ZIO.fromEither(
        requestPresentation.attachments.headOption.toRight(InvalidAnoncredPresentationRequest("Missing Presentation"))
      )
      presentationRequestData <-
        presentationRequestAttachment.data match
          case Base64(data) => ZIO.succeed(new String(JBase64.getUrlDecoder.decode(data)))
          case _            => ZIO.fail(InvalidAnoncredPresentationRequest("Expecting Base64-encoded data"))
      deserializedPresentationRequestData <-
        AnoncredPresentationRequestV1.schemaSerDes
          .deserialize(presentationRequestData)
          .mapError(error => InvalidAnoncredPresentationRequest(error.error))
      linkSecret <-
        linkSecretService
          .fetchOrCreate()
          .map(_.secret)
          .mapError(t => AnoncredPresentationCreationError(t.cause))
      presentation <-
        ZIO
          .fromEither(
            AnoncredLib.createPresentation(
              PresentationRequest(presentationRequestData),
              verifiableCredentials.map(verifiableCredential =>
                CredentialRequests(
                  Credential(verifiableCredential),
                  deserializedPresentationRequestData.requested_attributes.keys.toSeq, // TO FIX
                  deserializedPresentationRequestData.requested_predicates.keys.toSeq // TO FIX
                )
              ),
              Map.empty, // TO FIX
              linkSecret,
              schemaMap,
              credentialDefinitionMap
            )
          )
          .mapError((t: Throwable) => AnoncredPresentationCreationError(t))
    } yield presentation
  }

  private def resolveSchema(schemaId: String): IO[UnexpectedError, (String, SchemaDef)] = {
    for {
      uri <- ZIO.attempt(new URI(schemaId)).mapError(e => UnexpectedError(e.getMessage))
      content <- uriDereferencer.dereference(uri).mapError(e => UnexpectedError(e.error))
      vcSchema <- parseCredentialSchema(content).mapError(e => UnexpectedError(e.message))
      anoncredSchema <- AnoncredSchemaSerDesV1.schemaSerDes
        .deserialize(vcSchema.schema)
        .mapError(e => UnexpectedError(e.error))
      anoncredLibSchema =
        SchemaDef(
          schemaId,
          anoncredSchema.version,
          anoncredSchema.attrNames,
          anoncredSchema.issuerId
        )
    } yield (schemaId, anoncredLibSchema)
  }

  def acceptRequestPresentation(
      recordId: DidCommID,
      credentialsToUse: Seq[String]
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] = {

    for {
      record <- getRecordWithState(recordId, ProtocolState.RequestReceived)
      issuedCredentials <- credentialRepository
        .getValidIssuedCredentials(credentialsToUse.map(DidCommID(_)))
        .mapError(RepositoryError.apply)
      _ <- ZIO.cond(
        (issuedCredentials.map(_.subjectId).toSet.size == 1),
        (),
        PresentationError.HolderBindingError(
          s"Creating a Verifiable Presentation for credential with different subject DID is not supported, found : ${issuedCredentials
              .map(_.subjectId)}"
        )
      )
      validatedCredentials <- ZIO.fromEither(
        Either.cond(
          issuedCredentials.forall(issuedValidCredential =>
            issuedValidCredential.credentialFormat == record.credentialFormat
          ),
          issuedCredentials,
          PresentationError.NotMatchingPresentationCredentialFormat(
            new IllegalArgumentException(
              s"No matching issued credentials format: expectedFormat=${record.credentialFormat}"
            )
          )
        )
      )
      signedCredentials = validatedCredentials.flatMap(_.issuedCredentialRaw)
      _ <- ZIO.fromEither(
        Either.cond(
          signedCredentials.nonEmpty,
          signedCredentials,
          PresentationError.IssuedCredentialNotFoundError(
            new Throwable(s"No matching issued credentials found in prover db from the given: $credentialsToUse")
          )
        )
      )
      count <- presentationRepository
        .updatePresentationWithCredentialsToUse(recordId, Option(credentialsToUse), ProtocolState.PresentationPending)
        .mapError(RepositoryError.apply) @@ CustomMetricsAspect.startRecordingTime(
        s"${record.id}_present_proof_flow_prover_presentation_pending_to_generated_ms_gauge"
      )
      _ <- count match
        case 1 => ZIO.succeed(())
        case n => ZIO.fail(RecordIdNotFound(recordId))
      record <- presentationRepository
        .getPresentationRecord(recordId)
        .mapError(RepositoryError.apply)
        .flatMap {
          case None        => ZIO.fail(RecordIdNotFound(record.id))
          case Some(value) => ZIO.succeed(value)
        }
    } yield record
  }

  override def acceptPresentation(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] = {
    for {
      maybeRecord <- presentationRepository
        .getPresentationRecord(recordId)
        .mapError(RepositoryError.apply)
      record <- ZIO
        .fromOption(maybeRecord)
        .mapError(_ => RecordIdNotFound(recordId))
      _ <- ZIO
        .fromOption(record.presentationData)
        .mapError(_ => InvalidFlowStateError(s"No request found for this record: $recordId"))
      recordUpdated <- markPresentationAccepted(record.id)
    } yield recordUpdated
  }

  override def receivePresentation(
      presentation: Presentation
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] = {
    for {
      record <- getRecordFromThreadId(presentation.thid)
      _ <- presentationRepository
        .updateWithPresentation(record.id, presentation, ProtocolState.PresentationReceived)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply) @@ CustomMetricsAspect.startRecordingTime(
        s"${record.id}_present_proof_flow_verifier_presentation_received_to_verification_success_or_failure_ms_gauge"
      )
      record <- presentationRepository
        .getPresentationRecord(record.id)
        .mapError(RepositoryError.apply)
        .flatMap {
          case None        => ZIO.fail(RecordIdNotFound(record.id))
          case Some(value) => ZIO.succeed(value)
        }
    } yield record
  }

  override def acceptProposePresentation(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] = {
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
        .flatMap {
          case None        => ZIO.fail(RecordIdNotFound(record.id))
          case Some(value) => ZIO.succeed(value)
        }
    } yield record
  }

  override def receiveProposePresentation(
      proposePresentation: ProposePresentation
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] = {
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
        .flatMap {
          case None        => ZIO.fail(RecordIdNotFound(record.id))
          case Some(value) => ZIO.succeed(value)
        }
    } yield record
  }

  private[this] def getRecordWithState(
      recordId: DidCommID,
      state: ProtocolState
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] = {
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

  override def markRequestPresentationSent(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.RequestPending,
      PresentationRecord.ProtocolState.RequestSent
    )

  override def markProposePresentationSent(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.ProposalPending,
      PresentationRecord.ProtocolState.ProposalSent
    )
  override def markPresentationVerified(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.PresentationReceived,
      PresentationRecord.ProtocolState.PresentationVerified
    )

  override def markPresentationAccepted(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.PresentationVerified,
      PresentationRecord.ProtocolState.PresentationAccepted
    )

  override def markPresentationSent(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.PresentationGenerated,
      PresentationRecord.ProtocolState.PresentationSent
    )

  override def markPresentationRejected(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.PresentationVerified,
      PresentationRecord.ProtocolState.PresentationRejected
    )

  override def markRequestPresentationRejected(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.RequestReceived,
      PresentationRecord.ProtocolState.RequestRejected
    )

  override def markPresentationVerificationFailed(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    updatePresentationRecordProtocolState(
      recordId,
      PresentationRecord.ProtocolState.PresentationReceived,
      PresentationRecord.ProtocolState.PresentationVerificationFailed
    )

  def reportProcessingFailure(
      recordId: DidCommID,
      failReason: Option[String]
  ): ZIO[WalletAccessContext, PresentationError, Unit] =
    presentationRepository
      .updateAfterFail(recordId, failReason)
      .mapError(RepositoryError.apply)
      .flatMap {
        case 1 => ZIO.unit
        case n => ZIO.fail(UnexpectedError(s"Invalid number of records updated: $n"))
      }

  private[this] def getRecordFromThreadId(
      thid: Option[String]
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] = {
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

  private[this] def toJWTAttachment(options: Options): AttachmentDescriptor = {
    AttachmentDescriptor.buildJsonAttachment(
      payload = PresentationAttachment.build(Some(options)),
      format = Some(PresentCredentialRequestFormat.JWT.name)
    )
  }

  private[this] def toAnoncredAttachment(
      presentationRequest: AnoncredPresentationRequestV1
  ): AttachmentDescriptor = {
    AttachmentDescriptor.buildBase64Attachment(
      mediaType = Some("application/json"),
      format = Some(PresentCredentialRequestFormat.Anoncred.name),
      payload = AnoncredPresentationRequestV1.schemaSerDes.serialize(presentationRequest).getBytes()
    )
  }

  private[this] def createDidCommRequestPresentation(
      proofTypes: Seq[ProofType],
      thid: DidCommID,
      pairwiseVerifierDID: DidId,
      pairwiseProverDID: DidId,
      attachments: Seq[AttachmentDescriptor]
  ): RequestPresentation = {
    RequestPresentation(
      body = RequestPresentation.Body(
        goal_code = Some("Request Proof Presentation"),
        proof_types = proofTypes
      ),
      attachments = attachments,
      from = pairwiseVerifierDID,
      to = pairwiseProverDID,
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
      from = proposePresentation.to,
      to = proposePresentation.from,
      thid = proposePresentation.thid
    )
  }

  private[this] def updatePresentationRecordProtocolState(
      id: DidCommID,
      from: PresentationRecord.ProtocolState,
      to: PresentationRecord.ProtocolState
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] = {
    for {
      _ <- presentationRepository
        .updatePresentationRecordProtocolState(id, from, to)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
      record <- presentationRepository
        .getPresentationRecord(id)
        .mapError(RepositoryError.apply)
        .flatMap {
          case None        => ZIO.fail(RecordIdNotFound(id))
          case Some(value) => ZIO.succeed(value)
        }
    } yield record
  }

}

object PresentationServiceImpl {
  val layer: URLayer[
    CredentialDefinitionService & URIDereferencer & LinkSecretService & PresentationRepository & CredentialRepository,
    PresentationService
  ] =
    ZLayer.fromFunction(PresentationServiceImpl(_, _, _, _, _))
}
