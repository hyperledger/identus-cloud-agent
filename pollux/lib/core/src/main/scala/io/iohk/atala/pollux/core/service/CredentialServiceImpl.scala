package io.iohk.atala.pollux.core.service

import com.google.protobuf.ByteString
import io.circe.Json
import io.circe.syntax.*
import io.iohk.atala.agent.walletapi.storage.DIDSecretStorage
import io.iohk.atala.castor.core.model.did.{CanonicalPrismDID, PrismDID, VerificationRelationship}
import io.iohk.atala.iris.proto.dlt.IrisOperation
import io.iohk.atala.iris.proto.service.IrisOperationId
import io.iohk.atala.iris.proto.service.IrisServiceGrpc.IrisServiceStub
import io.iohk.atala.iris.proto.vc_operations.IssueCredentialsBatch
import io.iohk.atala.mercury.model.*
import io.iohk.atala.mercury.protocol.issuecredential.*
import io.iohk.atala.pollux.*
import io.iohk.atala.pollux.anoncreds.{AnoncredLib, CreateCredentialDefinition, CredentialOffer, LinkSecretWithId}
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.CredentialFormat.AnonCreds
import io.iohk.atala.pollux.core.model.IssueCredentialRecord.ProtocolState.OfferReceived
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import io.iohk.atala.pollux.core.model.error.CredentialServiceError.*
import io.iohk.atala.pollux.core.model.presentation.*
import io.iohk.atala.pollux.core.model.schema.CredentialSchema
import io.iohk.atala.pollux.core.repository.CredentialRepository
import io.iohk.atala.pollux.core.service.serdes.PrivateCredentialDefinitionSchemaSerDesV1
import io.iohk.atala.pollux.vc.jwt.*
import io.iohk.atala.prism.crypto.{MerkleInclusionProof, MerkleTreeKt, Sha256}
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.utils.aspects.CustomMetricsAspect
import zio.*
import zio.prelude.ZValidation

import java.net.URI
import java.rmi.UnexpectedException
import java.time.{Instant, ZoneId}
import java.util.UUID
import scala.language.implicitConversions

object CredentialServiceImpl {
  val layer: URLayer[
    IrisServiceStub & CredentialRepository & DidResolver & URIDereferencer & DIDSecretStorage &
      CredentialDefinitionService,
    CredentialService
  ] =
    ZLayer.fromFunction(CredentialServiceImpl(_, _, _, _, _, _))

//  private val VC_JSON_SCHEMA_URI = "https://w3c-ccg.github.io/vc-json-schemas/schema/2.0/schema.json"
  private val VC_JSON_SCHEMA_TYPE = "CredentialSchema2022"
}

private class CredentialServiceImpl(
    irisClient: IrisServiceStub,
    credentialRepository: CredentialRepository,
    didResolver: DidResolver,
    uriDereferencer: URIDereferencer,
    didSecretStorage: DIDSecretStorage,
    credentialDefinitionService: CredentialDefinitionService,
    maxRetries: Int = 5 // TODO move to config
) extends CredentialService {

  import CredentialServiceImpl.*
  import IssueCredentialRecord.*

  override def extractIdFromCredential(credential: W3cCredentialPayload): Option[DidCommID] =
    credential.maybeId.map(_.split("/").last).map(DidCommID(_))

  override def getIssueCredentialRecords(
      offset: Option[Int],
      limit: Option[Int]
  ): ZIO[WalletAccessContext, CredentialServiceError, (Seq[IssueCredentialRecord], Int)] = {
    for {
      records <- credentialRepository
        .getIssueCredentialRecords(offset = offset, limit = limit)
        .mapError(RepositoryError.apply)
    } yield records
  }

  override def getIssueCredentialRecordByThreadId(
      thid: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, Option[IssueCredentialRecord]] =
    for {
      record <- credentialRepository
        .getIssueCredentialRecordByThreadId(thid)
        .mapError(RepositoryError.apply)
    } yield record

  override def getIssueCredentialRecord(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, Option[IssueCredentialRecord]] = {
    for {
      record <- credentialRepository
        .getIssueCredentialRecord(recordId)
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def createIssueCredentialRecord(
      pairwiseIssuerDID: DidId,
      pairwiseHolderDID: DidId,
      thid: DidCommID,
      schemaId: Option[String],
      credentialDefinitionId: Option[UUID],
      credentialFormat: CredentialFormat,
      claims: Json,
      validityPeriod: Option[Double],
      automaticIssuance: Option[Boolean],
      awaitConfirmation: Option[Boolean],
      issuingDID: Option[CanonicalPrismDID],
      restServiceUrl: String
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] = {
    for {
      // TODO For AnonCreds, validate claims is a flat structure of type [String, String] or [String, Int]
      _ <- schemaId match
        case Some(schemaId) =>
          credentialFormat match
            case CredentialFormat.JWT =>
              CredentialSchema
                .validateClaims(schemaId, claims.noSpaces, uriDereferencer)
                .mapError(e => CredentialSchemaError(e))
            case CredentialFormat.AnonCreds =>
              ZIO.unit
        case None =>
          ZIO.unit

      attributes <- CredentialService.convertJsonClaimsToAttributes(claims)

      offer <- (credentialFormat, schemaId, credentialDefinitionId, issuingDID) match
        case (CredentialFormat.JWT, Some(schemaId), None, Some(_)) =>
          createJWTDidCommOfferCredential(
            pairwiseIssuerDID = pairwiseIssuerDID,
            pairwiseHolderDID = pairwiseHolderDID,
            schemaId = schemaId,
            claims = attributes,
            thid = thid,
            UUID.randomUUID().toString,
            "domain"
          )
        // TODO For AnonCreds, schemaId should be None. But still required for now because used to get CD secret.
        case (CredentialFormat.AnonCreds, Some(schemaId), Some(credentialDefinitionId), None) =>
          createAnonCredsDidCommOfferCredential(
            pairwiseIssuerDID = pairwiseIssuerDID,
            pairwiseHolderDID = pairwiseHolderDID,
            schemaId = schemaId,
            credentialDefinitionId = credentialDefinitionId,
            claims = attributes,
            thid = thid,
            restServiceUrl
          )
        case _ =>
          ZIO.fail(
            CredentialServiceError.UnexpectedError(
              s"Invalid 'schemaId/credentialDefinitionId/issuingDID' combination for $credentialFormat format"
            )
          )
      record <- ZIO.succeed(
        IssueCredentialRecord(
          id = DidCommID(),
          createdAt = Instant.now,
          updatedAt = None,
          thid = thid,
          schemaId = schemaId,
          credentialDefinitionId = credentialDefinitionId,
          credentialFormat = credentialFormat,
          role = IssueCredentialRecord.Role.Issuer,
          subjectId = None,
          validityPeriod = validityPeriod,
          automaticIssuance = automaticIssuance,
          awaitConfirmation = awaitConfirmation,
          protocolState = IssueCredentialRecord.ProtocolState.OfferPending,
          publicationState = None,
          offerCredentialData = Some(offer),
          requestCredentialData = None,
          issueCredentialData = None,
          issuedCredentialRaw = None,
          issuingDID = issuingDID,
          metaRetries = maxRetries,
          metaNextRetry = Some(Instant.now()),
          metaLastFailure = None,
        )
      )
      count <- credentialRepository
        .createIssueCredentialRecord(record)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply) @@ CustomMetricsAspect
        .startRecordingTime(s"${record.id}_issuer_offer_pending_to_sent_ms_gauge")
    } yield record
  }

  override def getIssueCredentialRecordsByStates(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: IssueCredentialRecord.ProtocolState*
  ): ZIO[WalletAccessContext, CredentialServiceError, Seq[IssueCredentialRecord]] = {
    for {
      records <- credentialRepository
        .getIssueCredentialRecordsByStates(ignoreWithZeroRetries, limit, states: _*)
        .mapError(RepositoryError.apply)
    } yield records
  }

  override def receiveCredentialOffer(
      offer: OfferCredential
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] = {
    for {
      attachment <- ZIO
        .fromOption(offer.attachments.headOption)
        .mapError(_ => CredentialServiceError.UnexpectedError("Missing attachment in credential offer"))

      format <- ZIO.fromOption(attachment.format).mapError(_ => MissingCredentialFormat)

      credentialFormat <- format match
        case value if value == IssueCredentialOfferFormat.JWT.name      => ZIO.succeed(CredentialFormat.JWT)
        case value if value == IssueCredentialOfferFormat.Anoncred.name => ZIO.succeed(CredentialFormat.AnonCreds)
        case value                                                      => ZIO.fail(UnsupportedCredentialFormat(value))

      _ <- validateCredentialOfferAttachment(credentialFormat, attachment)

      record <- ZIO.succeed(
        IssueCredentialRecord(
          id = DidCommID(),
          createdAt = Instant.now,
          updatedAt = None,
          thid = DidCommID(offer.thid.getOrElse(offer.id)),
          schemaId = None,
          credentialDefinitionId = None,
          credentialFormat = credentialFormat,
          role = Role.Holder,
          subjectId = None,
          validityPeriod = None,
          automaticIssuance = None,
          awaitConfirmation = None,
          protocolState = IssueCredentialRecord.ProtocolState.OfferReceived,
          publicationState = None,
          offerCredentialData = Some(offer),
          requestCredentialData = None,
          issueCredentialData = None,
          issuedCredentialRaw = None,
          issuingDID = None,
          metaRetries = maxRetries,
          metaNextRetry = Some(Instant.now()),
          metaLastFailure = None,
        )
      )
      count <- credentialRepository
        .createIssueCredentialRecord(record)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
    } yield record
  }

  private[this] def validateCredentialOfferAttachment(
      credentialFormat: CredentialFormat,
      attachment: AttachmentDescriptor
  ) = for {
    _ <- credentialFormat match
      case CredentialFormat.JWT =>
        attachment.data match
          case JsonData(json) =>
            ZIO
              .attempt(json.asJson.hcursor.downField("json").as[CredentialOfferAttachment])
              .mapError(e =>
                CredentialServiceError
                  .UnexpectedError(s"Unexpected error parsing credential offer attachment: ${e.toString}")
              )
          case _ =>
            ZIO.fail(
              CredentialServiceError
                .UnexpectedError(s"A JSON attachment is expected in the credential offer")
            )
      case CredentialFormat.AnonCreds =>
        attachment.data match
          case Base64(value) =>
            for {
              credentialOffer <- ZIO
                .attempt(CredentialOffer(value))
                .mapError(e =>
                  CredentialServiceError.UnexpectedError(
                    s"Unexpected error parsing credential offer attachment: ${e.toString}"
                  )
                )
              _ <- ZIO.logInfo(s"Credential Offer parsed => $credentialOffer")
            } yield ()
          case _ =>
            ZIO.fail(
              CredentialServiceError
                .UnexpectedError(s"A Base64 attachment is expected in the credential offer")
            )
  } yield ()

  override def acceptCredentialOffer(
      recordId: DidCommID,
      maybeSubjectId: Option[String]
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] = {
    for {
      record <- getRecordWithState(recordId, ProtocolState.OfferReceived)
      count <- (record.credentialFormat, maybeSubjectId) match
        case (CredentialFormat.JWT, Some(subjectId)) =>
          for {
            _ <- ZIO
              .fromEither(PrismDID.fromString(subjectId))
              .mapError(_ => CredentialServiceError.UnsupportedDidFormat(subjectId))
            count <- credentialRepository
              .updateWithSubjectId(recordId, subjectId, ProtocolState.RequestPending)
              .mapError(RepositoryError.apply) @@ CustomMetricsAspect.startRecordingTime(
              s"${record.id}_issuance_flow_holder_req_pending_to_generated"
            )
          } yield count
        case (CredentialFormat.AnonCreds, None) =>
          credentialRepository
            .updateCredentialRecordProtocolState(recordId, ProtocolState.OfferReceived, ProtocolState.RequestPending)
            .mapError(RepositoryError.apply) @@ CustomMetricsAspect.startRecordingTime(
            s"${record.id}_issuance_flow_holder_req_pending_to_generated"
          )
        case (format, _) =>
          ZIO.fail(
            CredentialServiceError.UnexpectedError(
              s"Invalid subjectId input for $format offer acceptance: $maybeSubjectId"
            )
          )
      _ <- count match
        case 1 => ZIO.succeed(())
        case n => ZIO.fail(RecordIdNotFound(recordId))
      record <- credentialRepository
        .getIssueCredentialRecord(record.id)
        .mapError(RepositoryError.apply)
        .flatMap {
          case None        => ZIO.fail(RecordIdNotFound(recordId))
          case Some(value) => ZIO.succeed(value)
        }
    } yield record
  }

  override def createPresentationPayload(
      recordId: DidCommID,
      subject: Issuer
  ): ZIO[WalletAccessContext, CredentialServiceError, PresentationPayload] = {
    for {
      record <- getRecordWithState(recordId, ProtocolState.RequestPending)
      maybeOptions <- getOptionsFromOfferCredentialData(record)
    } yield {
      W3cPresentationPayload(
        `@context` = Vector("https://www.w3.org/2018/presentations/v1"),
        maybeId = None,
        `type` = Vector("VerifiablePresentation"),
        verifiableCredential = IndexedSeq.empty,
        holder = subject.did.value,
        verifier = IndexedSeq.empty ++ maybeOptions.map(_.domain),
        maybeIssuanceDate = None,
        maybeExpirationDate = None
      ).toJwtPresentationPayload.copy(maybeNonce = maybeOptions.map(_.challenge))
    }
  }

  override def generateJWTCredentialRequest(
      recordId: DidCommID,
      signedPresentation: JWT
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] = {
    for {
      record <- getRecordWithState(recordId, ProtocolState.RequestPending)
      formatAndOffer <- ZIO
        .fromOption(record.offerCredentialFormatAndData)
        .mapError(_ => InvalidFlowStateError(s"No offer found for this record: $recordId"))
      request = createDidCommRequestCredential(formatAndOffer._1, formatAndOffer._2, signedPresentation)
      count <- credentialRepository
        .updateWithRequestCredential(recordId, request, ProtocolState.RequestGenerated)
        .mapError(RepositoryError.apply) @@ CustomMetricsAspect.endRecordingTime(
        s"${record.id}_issuance_flow_holder_req_pending_to_generated",
        "issuance_flow_holder_req_pending_to_generated_ms_gauge"
      ) @@ CustomMetricsAspect.startRecordingTime(s"${record.id}_issuance_flow_holder_req_generated_to_sent")
      _ <- count match
        case 1 => ZIO.succeed(())
        case n => ZIO.fail(RecordIdNotFound(recordId))
      record <- credentialRepository
        .getIssueCredentialRecord(record.id)
        .mapError(RepositoryError.apply)
        .flatMap {
          case None        => ZIO.fail(RecordIdNotFound(recordId))
          case Some(value) => ZIO.succeed(value)
        }
    } yield record
  }

  override def generateAnonCredsCredentialRequest(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] = {
    for {
      record <- getRecordWithState(recordId, ProtocolState.RequestPending)
      offerCredential <- ZIO
        .fromOption(record.offerCredentialData)
        .mapError(_ => InvalidFlowStateError(s"No offer found for this record: ${record.id}"))
      body = RequestCredential.Body(goal_code = Some("Request Credential"))
      attachments <- createAnonCredsRequestCredential(offerCredential).map { createCredentialRequest =>
        Seq(
          AttachmentDescriptor.buildBase64Attachment(
            mediaType = Some("application/json"),
            format = Some(IssueCredentialRequestFormat.Anoncred.name),
            payload = createCredentialRequest.request.data.getBytes()
          )
        )
      }

      // TODO How to serialize this createCredentialRequest to JSON for DB storage??
      // the request part is sent to issuer
      // the metadata part is used by holder when later processing received credential
      // Add a new 'request_credential_metadata' field in DB!
      request = RequestCredential(
        body = body,
        attachments = attachments,
        from = offerCredential.to,
        to = offerCredential.from,
        thid = offerCredential.thid
      )
      count <- credentialRepository
        .updateWithRequestCredential(recordId, request, ProtocolState.RequestGenerated)
        .mapError(RepositoryError.apply) @@ CustomMetricsAspect.endRecordingTime(
        s"${record.id}_issuance_flow_holder_req_pending_to_generated",
        "issuance_flow_holder_req_pending_to_generated_ms_gauge"
      ) @@ CustomMetricsAspect.startRecordingTime(s"${record.id}_issuance_flow_holder_req_generated_to_sent")
      _ <- count match
        case 1 => ZIO.succeed(())
        case n => ZIO.fail(RecordIdNotFound(recordId))
      record <- credentialRepository
        .getIssueCredentialRecord(record.id)
        .mapError(RepositoryError.apply)
        .flatMap {
          case None        => ZIO.fail(RecordIdNotFound(recordId))
          case Some(value) => ZIO.succeed(value)
        }
    } yield record
  }

  private[this] def createAnonCredsRequestCredential(offerCredential: OfferCredential) = {
    for {
      attachmentData <- ZIO
        .fromOption(
          offerCredential.attachments
            .find(_.format.contains(IssueCredentialOfferFormat.Anoncred.name))
            .map(_.data)
            .flatMap {
              case Base64(value) => Some(new String(java.util.Base64.getDecoder.decode(value)))
              case _             => None
            }
        )
        .mapError(_ => InvalidFlowStateError(s"No AnonCreds offer attachment found"))
      credentialOffer = anoncreds.CredentialOffer(attachmentData)
      _ <- ZIO.logInfo(s"Cred def ID => ${credentialOffer.getCredDefId}")
      credDefContent <- uriDereferencer
        .dereference(new URI(credentialOffer.getCredDefId))
        .mapError(err => UnexpectedError(err.toString))
      _ <- ZIO.logInfo(s"Cred Def Content => $credDefContent")
      credentialDefinition = anoncreds.CredentialDefinition(credDefContent)
      linkSecret = LinkSecretWithId(UUID.randomUUID().toString)
      createCredentialRequest = AnoncredLib.createCredentialRequest(linkSecret, credentialDefinition, credentialOffer)
    } yield createCredentialRequest
  }

  override def receiveCredentialRequest(
      request: RequestCredential
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] = {
    for {
      record <- getRecordFromThreadIdWithState(
        request.thid.map(DidCommID(_)),
        ignoreWithZeroRetries = true,
        ProtocolState.OfferPending,
        ProtocolState.OfferSent
      )
      _ <- credentialRepository
        .updateWithRequestCredential(record.id, request, ProtocolState.RequestReceived)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
      record <- credentialRepository
        .getIssueCredentialRecord(record.id)
        .mapError(RepositoryError.apply)
        .someOrFail(RecordIdNotFound(record.id))
    } yield record
  }

  override def acceptCredentialRequest(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] = {
    for {
      record <- getRecordWithState(recordId, ProtocolState.RequestReceived)
      request <- ZIO
        .fromOption(record.requestCredentialData)
        .mapError(_ => InvalidFlowStateError(s"No request found for this record: $recordId"))
      issue = createDidCommIssueCredential(request)
      count <- credentialRepository
        .updateWithIssueCredential(recordId, issue, ProtocolState.CredentialPending)
        .mapError(RepositoryError.apply) @@ CustomMetricsAspect.startRecordingTime(
        s"${record.id}_issuance_flow_issuer_credential_pending_to_generated"
      )
      _ <- count match
        case 1 => ZIO.succeed(())
        case n => ZIO.fail(RecordIdNotFound(recordId))
      record <- credentialRepository
        .getIssueCredentialRecord(record.id)
        .mapError(RepositoryError.apply)
        .someOrFail(RecordIdNotFound(record.id))
    } yield record
  }

  override def receiveCredentialIssue(
      issueCredential: IssueCredential
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] = {
    // TODO We can get rid of this 'raw' representation stored in DB, because it is not used.
    val rawIssuedCredential = issueCredential.attachments.map(_.data.asJson.noSpaces).headOption.getOrElse("???")
    for {
      // TODO Move this type of generic/reusable code to a helper trait
      attachmentFormatAndData <- ZIO.succeed {
        import IssueCredentialIssuedFormat.{Anoncred, JWT}
        issueCredential.attachments
          .collectFirst {
            case AttachmentDescriptor(_, _, Base64(v), Some(JWT.name), _, _, _, _)      => (JWT, v)
            case AttachmentDescriptor(_, _, Base64(v), Some(Anoncred.name), _, _, _, _) => (Anoncred, v)
          }
          .map { case (f, v) => (f, java.util.Base64.getUrlDecoder.decode(v)) }
      }
      _ = attachmentFormatAndData match
        case Some(IssueCredentialIssuedFormat.JWT, _)      => ZIO.succeed(())
        case Some(IssueCredentialIssuedFormat.Anoncred, v) => processAnonCredsCredential(v)
        case _ => UnexpectedError("No AnonCreds or JWT credential attachment found")

      record <- getRecordFromThreadIdWithState(
        issueCredential.thid.map(DidCommID(_)),
        ignoreWithZeroRetries = true,
        ProtocolState.RequestPending,
        ProtocolState.RequestSent
      )
      _ <- credentialRepository
        .updateWithIssuedRawCredential(
          record.id,
          issueCredential,
          rawIssuedCredential,
          ProtocolState.CredentialReceived
        )
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
      record <- credentialRepository
        .getIssueCredentialRecord(record.id)
        .mapError(RepositoryError.apply)
        .someOrFail(RecordIdNotFound(record.id))
    } yield record
  }

  private[this] def processAnonCredsCredential(credential: Array[Byte]) = {
    // TODO Implement credential processing/validation by holder
//    for {
//      _ <- ZIO.attempt(
//        AnoncredLib.processCredential(
//          anoncreds.Credential(new String(credential)),
//          ???,
//          ???,
//          ???
//        )
//      )
//    } yield ()
    ZIO.succeed(())
  }

  override def markOfferSent(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] =
    updateCredentialRecordProtocolState(
      recordId,
      IssueCredentialRecord.ProtocolState.OfferPending,
      IssueCredentialRecord.ProtocolState.OfferSent
    )

  override def markRequestSent(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] =
    updateCredentialRecordProtocolState(
      recordId,
      IssueCredentialRecord.ProtocolState.RequestGenerated,
      IssueCredentialRecord.ProtocolState.RequestSent
    ) @@ CustomMetricsAspect.endRecordingTime(
      s"${recordId}_issuance_flow_holder_req_generated_to_sent",
      "issuance_flow_holder_req_generated_to_sent_ms_gauge"
    )

  override def markCredentialGenerated(
      recordId: DidCommID,
      issueCredential: IssueCredential
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] = {
    for {
      record <- getRecordWithState(recordId, ProtocolState.CredentialPending)
      count <- credentialRepository
        .updateWithIssueCredential(
          recordId,
          issueCredential,
          IssueCredentialRecord.ProtocolState.CredentialGenerated
        )
        .mapError(RepositoryError.apply) @@ CustomMetricsAspect.endRecordingTime(
        s"${record.id}_issuance_flow_issuer_credential_pending_to_generated",
        "issuance_flow_issuer_credential_pending_to_generated_ms_gauge"
      ) @@ CustomMetricsAspect.startRecordingTime(s"${record.id}_issuance_flow_issuer_credential_generated_to_sent")
      _ <- count match
        case 1 => ZIO.succeed(())
        case n => ZIO.fail(RecordIdNotFound(recordId))
      record <- credentialRepository
        .getIssueCredentialRecord(recordId)
        .mapError(RepositoryError.apply)
        .flatMap {
          case None        => ZIO.fail(RecordIdNotFound(recordId))
          case Some(value) => ZIO.succeed(value)
        }

    } yield record
  }

  override def markCredentialSent(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] =
    updateCredentialRecordProtocolState(
      recordId,
      IssueCredentialRecord.ProtocolState.CredentialGenerated,
      IssueCredentialRecord.ProtocolState.CredentialSent
    ) @@ CustomMetricsAspect.endRecordingTime(
      s"${recordId}_issuance_flow_issuer_credential_generated_to_sent",
      "issuance_flow_issuer_credential_generated_to_sent_ms_gauge"
    )

  override def markCredentialPublicationPending(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] =
    updateCredentialRecordPublicationState(
      recordId,
      None,
      Some(IssueCredentialRecord.PublicationState.PublicationPending)
    )

  override def markCredentialPublicationQueued(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] =
    updateCredentialRecordPublicationState(
      recordId,
      Some(IssueCredentialRecord.PublicationState.PublicationPending),
      Some(IssueCredentialRecord.PublicationState.PublicationQueued)
    )

  override def markCredentialPublished(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] =
    updateCredentialRecordPublicationState(
      recordId,
      Some(IssueCredentialRecord.PublicationState.PublicationQueued),
      Some(IssueCredentialRecord.PublicationState.Published)
    )

  override def reportProcessingFailure(
      recordId: DidCommID,
      failReason: Option[String]
  ): ZIO[WalletAccessContext, CredentialServiceError, Unit] =
    credentialRepository
      .updateAfterFail(recordId, failReason)
      .mapError(RepositoryError.apply)
      .flatMap {
        case 1 => ZIO.unit
        case n => ZIO.fail(UnexpectedError(s"Invalid number of records updated: $n"))
      }

  private[this] def getRecordWithState(
      recordId: DidCommID,
      state: ProtocolState
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] = {
    for {
      maybeRecord <- credentialRepository
        .getIssueCredentialRecord(recordId)
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

  private[this] def getRecordFromThreadIdWithState(
      thid: Option[DidCommID],
      ignoreWithZeroRetries: Boolean,
      states: ProtocolState*
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] = {
    for {
      thid <- ZIO
        .fromOption(thid)
        .mapError(_ => UnexpectedError("No `thid` found in credential request"))
      maybeRecord <- credentialRepository
        .getIssueCredentialRecordByThreadId(thid, ignoreWithZeroRetries)
        .mapError(RepositoryError.apply)
      record <- ZIO
        .fromOption(maybeRecord)
        .mapError(_ => ThreadIdNotFound(thid))
      _ <- record.protocolState match {
        case s if states.contains(s) => ZIO.unit
        case state                   => ZIO.fail(InvalidFlowStateError(s"Invalid protocol state for operation: $state"))
      }
    } yield record
  }

  private[this] def createJWTDidCommOfferCredential(
      pairwiseIssuerDID: DidId,
      pairwiseHolderDID: DidId,
      schemaId: String,
      claims: Seq[Attribute],
      thid: DidCommID,
      challenge: String,
      domain: String
  ) = {
    for {
      credentialPreview <- ZIO.succeed(CredentialPreview(schema_id = Some(schemaId), attributes = claims))
      body = OfferCredential.Body(
        goal_code = Some("Offer Credential"),
        credential_preview = credentialPreview,
      )
      attachments <- ZIO.succeed(
        Seq(
          AttachmentDescriptor.buildJsonAttachment(
            mediaType = Some("application/json"),
            format = Some(IssueCredentialOfferFormat.JWT.name),
            payload = PresentationAttachment(
              Some(Options(challenge, domain)),
              PresentationDefinition(format = Some(ClaimFormat(jwt = Some(Jwt(alg = Seq("ES256K"), proof_type = Nil)))))
            )
          )
        )
      )
    } yield OfferCredential(
      body = body,
      attachments = attachments,
      from = pairwiseIssuerDID,
      to = pairwiseHolderDID,
      thid = Some(thid.value)
    )
  }

  private[this] def createAnonCredsDidCommOfferCredential(
      pairwiseIssuerDID: DidId,
      pairwiseHolderDID: DidId,
      schemaId: String,
      credentialDefinitionId: UUID,
      claims: Seq[Attribute],
      thid: DidCommID,
      restServiceUrl: String
  ) = {
    for {
      credentialPreview <- ZIO.succeed(CredentialPreview(schema_id = Some(schemaId), attributes = claims))
      body = OfferCredential.Body(
        goal_code = Some("Offer Credential"),
        credential_preview = credentialPreview,
      )
      attachments <- createAnonCredsCredentialOffer(credentialDefinitionId, restServiceUrl).map { offer =>
        Seq(
          AttachmentDescriptor.buildBase64Attachment(
            mediaType = Some("application/json"),
            format = Some(IssueCredentialOfferFormat.Anoncred.name),
            payload = offer.data.getBytes()
          )
        )
      }
    } yield OfferCredential(
      body = body,
      attachments = attachments,
      from = pairwiseIssuerDID,
      to = pairwiseHolderDID,
      thid = Some(thid.value)
    )
  }

  private[this] def createAnonCredsCredentialOffer(credentialDefinitionId: UUID, restServiceUrl: String) = for {
    credentialDefinition <- credentialDefinitionService
      .getByGUID(credentialDefinitionId)
      .mapError(e => CredentialServiceError.UnexpectedError(e.toString))
    cd = anoncreds.CredentialDefinition(credentialDefinition.definition.toString)
    kcp = anoncreds.CredentialKeyCorrectnessProof(credentialDefinition.keyCorrectnessProof.toString)
    maybeDidSecret <- didSecretStorage
      .getKey(
        DidId(credentialDefinition.author),
        s"anoncred-credential-definition-private-key/${credentialDefinition.guid}",
        PrivateCredentialDefinitionSchemaSerDesV1.version
      )
      .orDie
    didSecret <- ZIO
      .fromOption(maybeDidSecret)
      .mapError(_ => CredentialServiceError.CredentialDefinitionPrivatePartNotFound(credentialDefinition.guid))
    cdp = anoncreds.CredentialDefinitionPrivate(didSecret.json.toString)
    createCredentialDefinition = CreateCredentialDefinition(cd, cdp, kcp)
    offer = AnoncredLib.createOffer(
      createCredentialDefinition, {
        val urlSuffix = s"credential-definition-registry/definitions/${credentialDefinition.guid.toString}/definition"
        val urlPrefix = if (restServiceUrl.endsWith("/")) restServiceUrl else restServiceUrl + "/"
        s"$urlPrefix$urlSuffix"
      }
    )
  } yield offer

  private[this] def createDidCommRequestCredential(
      format: IssueCredentialOfferFormat,
      offer: OfferCredential,
      signedPresentation: JWT
  ): RequestCredential = {
    RequestCredential(
      body = RequestCredential.Body(
        goal_code = offer.body.goal_code,
        comment = offer.body.comment,
      ),
      attachments = Seq(
        AttachmentDescriptor
          .buildBase64Attachment(
            mediaType = Some("application/json"),
            format = Some(format.name),
            // FIXME copy payload will probably not work for anoncreds!
            payload = signedPresentation.value.getBytes(),
          )
      ),
      thid = offer.thid.orElse(Some(offer.id)),
      from = offer.to,
      to = offer.from
    )
  }

  private[this] def createDidCommIssueCredential(request: RequestCredential): IssueCredential = {
    IssueCredential(
      body = IssueCredential.Body(
        goal_code = request.body.goal_code,
        comment = request.body.comment,
        replacement_id = None,
        more_available = None,
      ),
      attachments = Seq(), // FIXME !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
      thid = request.thid.orElse(Some(request.id)),
      from = request.to,
      to = request.from
    )
  }

  /** this is an auxiliary function.
    *
    * @note
    *   Between updating and getting the CredentialRecord back the CredentialRecord can be updated by other operations
    *   in the middle.
    *
    * TODO: this should be improved to behave exactly like atomic operation.
    */
  private[this] def updateCredentialRecordProtocolState(
      id: DidCommID,
      from: IssueCredentialRecord.ProtocolState,
      to: IssueCredentialRecord.ProtocolState
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] = {
    for {
      record <- credentialRepository
        .updateCredentialRecordProtocolState(id, from, to)
        .mapError(RepositoryError.apply)
        .flatMap {
          case 0 =>
            credentialRepository
              .getIssueCredentialRecord(id)
              .mapError(RepositoryError.apply)
              .flatMap {
                case None => ZIO.fail(RecordIdNotFound(id))
                case Some(record) if record.protocolState == to => // Not update by is alredy on the desirable state
                  ZIO.succeed(record)
                case Some(record) =>
                  ZIO.fail(
                    OperationNotExecuted(
                      id,
                      s"CredentialRecord was not updated because have the ProtocolState ${record.protocolState}"
                    )
                  )
              }
          case 1 =>
            credentialRepository
              .getIssueCredentialRecord(id)
              .mapError(RepositoryError.apply)
              .flatMap {
                case None => ZIO.fail(RecordIdNotFound(id))
                case Some(record) =>
                  ZIO
                    .logError(
                      s"The CredentialRecord ($id) is expected to be on the ProtocolState '$to' after updating it"
                    )
                    .when(record.protocolState != to)
                    .as(record)
              }
          case n => ZIO.fail(UnexpectedError(s"Invalid row count result: $n"))
        }
    } yield record
  }

  /** this is an auxiliary function.
    *
    * @note
    *   Between updating and getting the CredentialRecord back the CredentialRecord can be updated by other operations
    *   in the middle.
    *
    * TODO: this should be improved to behave exactly like atomic operation.
    */
  private[this] def updateCredentialRecordPublicationState(
      id: DidCommID,
      from: Option[IssueCredentialRecord.PublicationState],
      to: Option[IssueCredentialRecord.PublicationState]
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] = {
    for {
      record <- credentialRepository
        .updateCredentialRecordPublicationState(id, from, to)
        .mapError(RepositoryError.apply)
        .flatMap {
          case 0 =>
            credentialRepository
              .getIssueCredentialRecord(id)
              .mapError(RepositoryError.apply)
              .flatMap {
                case None => ZIO.fail(RecordIdNotFound(id))
                case Some(record) if record.publicationState == to => // Not update by is alredy on the desirable state
                  ZIO.succeed(record)
                case Some(record) =>
                  ZIO.fail(
                    OperationNotExecuted(
                      id,
                      s"CredentialRecord was not updated because have the PublicationState ${record.publicationState}"
                    )
                  )
              }
          case 1 =>
            credentialRepository
              .getIssueCredentialRecord(id)
              .mapError(RepositoryError.apply)
              .flatMap {
                case None => ZIO.fail(RecordIdNotFound(id))
                case Some(record) =>
                  {
                    if (record.publicationState == to) (ZIO.unit)
                    else
                      ZIO.logError(
                        s"The CredentialRecord ($id) is expected to be on the PublicationState '$to' after updating it"
                      ) // The expectation is for the record to still be on the state we (just) updated to
                  } *> ZIO.succeed(record)
              }
          case n => ZIO.fail(UnexpectedError(s"Invalid row count result: $n"))
        }
    } yield record
  }

  override def createCredentialPayloadFromRecord(
      record: IssueCredentialRecord,
      issuer: Issuer,
      issuanceDate: Instant
  ): ZIO[WalletAccessContext, CredentialServiceError, W3cCredentialPayload] = {
    val credential = for {
      offerCredentialData <- ZIO
        .fromOption(record.offerCredentialData)
        .mapError(_ =>
          CredentialServiceError.CreateCredentialPayloadFromRecordError(
            new Throwable("Could not extract claims from \"requestCredential\" DIDComm message")
          )
        )
      preview = offerCredentialData.body.credential_preview
      claims <- CredentialService.convertAttributesToJsonClaims(preview.attributes)
      maybeOfferOptions <- getOptionsFromOfferCredentialData(record)
      requestJwt <- getJwtFromRequestCredentialData(record)

      // domain/challenge validation + JWT verification
      jwtPresentation <- validateRequestCredentialDataProof(maybeOfferOptions, requestJwt).tapBoth(
        error =>
          ZIO.logErrorCause("JWT Presentation Validation Failed!!", Cause.fail(error)) *> credentialRepository
            .updateCredentialRecordProtocolState(
              record.id,
              ProtocolState.CredentialPending,
              ProtocolState.ProblemReportPending
            )
            .mapError(t => RepositoryError(t)),
        payload => ZIO.logInfo("JWT Presentation Validation Successful!")
      )
      // TODO: get schema when schema registry is available if schema ID is provided
      credential = W3cCredentialPayload(
        `@context` = Set(
          "https://www.w3.org/2018/credentials/v1"
        ), // TODO: his information should come from Schema registry by record.schemaId
        maybeId = None,
        `type` =
          Set("VerifiableCredential"), // TODO: This information should come from Schema registry by record.schemaId
        issuer = issuer.did,
        issuanceDate = issuanceDate,
        maybeExpirationDate = record.validityPeriod.map(sec => issuanceDate.plusSeconds(sec.toLong)),
        maybeCredentialSchema =
          record.schemaId.map(id => io.iohk.atala.pollux.vc.jwt.CredentialSchema(id, VC_JSON_SCHEMA_TYPE)),
        credentialSubject = claims.add("id", jwtPresentation.iss.asJson).asJson,
        maybeCredentialStatus = None,
        maybeRefreshService = None,
        maybeEvidence = None,
        maybeTermsOfUse = None
      )
    } yield credential

    credential

  }

  override def generateAnonCredsCredential(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] = {
    for {
      record <- getRecordWithState(recordId, ProtocolState.CredentialPending)
      requestCredential <- ZIO
        .fromOption(record.requestCredentialData)
        .mapError(_ => InvalidFlowStateError(s"No request found for this record: ${record.id}"))
      body = IssueCredential.Body(goal_code = Some("Issue Credential"))
      attachments <- createAnonCredsCredential(record).map { credential =>
        Seq(
          AttachmentDescriptor.buildBase64Attachment(
            mediaType = Some("application/json"),
            format = Some(IssueCredentialIssuedFormat.Anoncred.name),
            payload = credential.data.getBytes()
          )
        )
      }
      issue = IssueCredential(
        body = body,
        attachments = attachments,
        from = requestCredential.to,
        to = requestCredential.from,
        thid = requestCredential.thid
      )
      count <- credentialRepository
        .updateWithIssueCredential(recordId, issue, ProtocolState.CredentialGenerated)
        .mapError(RepositoryError.apply) @@ CustomMetricsAspect.endRecordingTime(
        s"${record.id}_issuance_flow_issuer_credential_pending_to_generated",
        "issuance_flow_issuer_credential_pending_to_generated_ms_gauge"
      ) @@ CustomMetricsAspect.startRecordingTime(s"${record.id}_issuance_flow_issuer_credential_generated_to_sent")
      _ <- count match
        case 1 => ZIO.succeed(())
        case n => ZIO.fail(RecordIdNotFound(recordId))
      record <- credentialRepository
        .getIssueCredentialRecord(record.id)
        .mapError(RepositoryError.apply)
        .flatMap {
          case None        => ZIO.fail(RecordIdNotFound(recordId))
          case Some(value) => ZIO.succeed(value)
        }
    } yield record
  }

  private[this] def createAnonCredsCredential(record: IssueCredentialRecord) = {
    for {
      credentialDefinitionId <- ZIO
        .fromOption(record.credentialDefinitionId)
        .mapError(_ => CredentialServiceError.UnexpectedError(s"No cred def Id found un record: ${record.id}"))
      credentialDefinition <- credentialDefinitionService
        .getByGUID(credentialDefinitionId)
        .mapError(e => CredentialServiceError.UnexpectedError(e.toString))
      cd = anoncreds.CredentialDefinition(credentialDefinition.definition.toString)
      offerCredential <- ZIO
        .fromOption(record.offerCredentialData)
        .mapError(_ => InvalidFlowStateError(s"No offer found for this record: ${record.id}"))
      offerCredentialAttachmentData <- ZIO
        .fromOption(
          offerCredential.attachments
            .find(_.format.contains(IssueCredentialOfferFormat.Anoncred.name))
            .map(_.data)
            .flatMap {
              case Base64(value) => Some(new String(java.util.Base64.getUrlDecoder.decode(value)))
              case _             => None
            }
        )
        .mapError(_ => InvalidFlowStateError(s"No AnonCreds offer attachment found"))
      credentialOffer = anoncreds.CredentialOffer(offerCredentialAttachmentData)
      requestCredential <- ZIO
        .fromOption(record.requestCredentialData)
        .mapError(_ => InvalidFlowStateError(s"No request found for this record: ${record.id}"))
      requestCredentialAttachmentData <- ZIO
        .fromOption(
          requestCredential.attachments
            .find(_.format.contains(IssueCredentialRequestFormat.Anoncred.name))
            .map(_.data)
            .flatMap {
              case Base64(value) => Some(new String(java.util.Base64.getUrlDecoder.decode(value)))
              case _             => None
            }
        )
        .mapError(_ => InvalidFlowStateError(s"No AnonCreds request attachment found"))
      credentialRequest = anoncreds.CredentialRequest(requestCredentialAttachmentData)
      attrValues = offerCredential.body.credential_preview.attributes.map { attr =>
        (attr.name, attr.value)
      }
      did <- ZIO
        .fromOption(record.issuingDID)
        .mapError(_ => CredentialServiceError.UnexpectedError("No issuingDID found"))
      maybeDidSecret <- didSecretStorage
        .getKey(
          DidId(did.toString),
          s"anoncred-credential-definition-private-key/${credentialDefinition.guid}",
          PrivateCredentialDefinitionSchemaSerDesV1.version
        )
        .mapError(error => CredentialServiceError.UnexpectedError(error.getMessage))
      cdPrivate <- ZIO
        .fromOption(maybeDidSecret)
        .map(secret => anoncreds.CredentialDefinitionPrivate(secret.json.toString))
        .mapError(_ => CredentialServiceError.UnexpectedError("Credential Definition Private part not found in secret"))
      credential = AnoncredLib.createCredential(
        cd,
        cdPrivate,
        credentialOffer,
        credentialRequest,
        attrValues
      )
    } yield credential
  }

  private[this] def getOptionsFromOfferCredentialData(record: IssueCredentialRecord) = {
    for {
      offer <- ZIO
        .fromOption(record.offerCredentialData)
        .mapError(_ => CredentialServiceError.UnexpectedError(s"Offer data not found in record: ${record.id}"))
      attachmentDescriptor <- ZIO
        .fromOption(offer.attachments.headOption)
        .mapError(_ => UnexpectedError(s"Attachments not found in record: ${record.id}"))
      json <- attachmentDescriptor.data match
        case JsonData(json) => ZIO.succeed(json.asJson)
        case _              => ZIO.fail(UnexpectedError(s"Attachment doesn't contain JsonData: ${record.id}"))
      maybeOptions <- ZIO
        .fromEither(json.as[PresentationAttachment].map(_.options))
        .mapError(df => UnexpectedError(df.getMessage))
    } yield maybeOptions
  }

  private[this] def getJwtFromRequestCredentialData(record: IssueCredentialRecord) = {
    for {
      request <- ZIO
        .fromOption(record.requestCredentialData)
        .mapError(_ => CredentialServiceError.UnexpectedError(s"Request data not found in record: ${record.id}"))
      attachmentDescriptor <- ZIO
        .fromOption(request.attachments.headOption)
        .mapError(_ => UnexpectedError(s"Attachments not found in record: ${record.id}"))
      jwt <- attachmentDescriptor.data match
        case Base64(b64) =>
          ZIO.succeed {
            val base64Decoded = new String(java.util.Base64.getDecoder().decode(b64))
            JWT(base64Decoded)
          }
        case _ => ZIO.fail(UnexpectedError(s"Attachment doesn't contain Base64Data: ${record.id}"))
    } yield jwt
  }

  private[this] def validateRequestCredentialDataProof(maybeOptions: Option[Options], jwt: JWT) = {
    for {
      _ <- maybeOptions match
        case None => ZIO.unit
        case Some(options) =>
          JwtPresentation.validatePresentation(jwt, options.domain, options.challenge) match
            case ZValidation.Success(log, value) => ZIO.unit
            case ZValidation.Failure(log, error) =>
              ZIO.fail(CredentialRequestValidationError("JWT presentation domain/validation validation failed"))

      clock = java.time.Clock.system(ZoneId.systemDefault)

      verificationResult <- JwtPresentation
        .verify(
          jwt,
          JwtPresentation.PresentationVerificationOptions(
            maybeProofPurpose = Some(VerificationRelationship.Authentication),
            verifySignature = true,
            verifyDates = false,
            leeway = Duration.Zero
          )
        )(didResolver)(clock)
        .mapError(errors => CredentialRequestValidationError(s"JWT presentation verification failed: $errors"))

      result <- verificationResult match
        case ZValidation.Success(log, value) => ZIO.unit
        case ZValidation.Failure(log, error) =>
          ZIO.fail(CredentialRequestValidationError(s"JWT presentation verification failed: $error"))

      jwtPresentation <- ZIO
        .fromTry(JwtPresentation.decodeJwt(jwt))
        .mapError(t => CredentialRequestValidationError(s"JWT presentation decoding failed: ${t.getMessage()}"))
    } yield jwtPresentation
  }

  def publishCredentialBatch(
      credentials: Seq[W3cCredentialPayload],
      issuer: Issuer
  ): IO[CredentialServiceError, PublishedBatchData] = {
    import scala.jdk.CollectionConverters.*

    val hashes = credentials
      .map { c =>
        val encoded = W3CCredential.toEncodedJwt(c, issuer)
        Sha256.compute(encoded.value.getBytes)
      }
      .toBuffer
      .asJava

    val merkelRootAndProofs = MerkleTreeKt.generateProofs(hashes)
    val root = merkelRootAndProofs.component1()
    val proofs = merkelRootAndProofs.component2().asScala.toSeq

    val irisOperation = IrisOperation(
      IrisOperation.Operation.IssueCredentialsBatch(
        IssueCredentialsBatch(
          issuerDid = issuer.did.value,
          merkleRoot = ByteString.copyFrom(root.getHash.component1)
        )
      )
    )

    val credentialsAndProofs = credentials.zip(proofs)

    val result = ZIO
      .fromFuture(_ => irisClient.scheduleOperation(irisOperation))
      .mapBoth(
        IrisError(_),
        irisOpeRes =>
          PublishedBatchData(
            operationId = IrisOperationId(irisOpeRes.operationId),
            credentialsAnsProofs = credentialsAndProofs
          )
      )

    result
  }

  override def markCredentialRecordsAsPublishQueued(
      credentialsAndProofs: Seq[(W3cCredentialPayload, MerkleInclusionProof)]
  ): ZIO[WalletAccessContext, CredentialServiceError, Int] = {

    /*
     * Since id of the credential is optional according to W3 spec,
     * it is of a type Option in W3cCredentialPayload since it is a generic W3 credential payload
     * but for our use-case, credentials must have an id, so if for some reason at least one
     * credential does not have an id, we return an error
     *
     */
    val maybeUndefinedId = credentialsAndProofs.find(x => extractIdFromCredential(x._1).isEmpty)

    if (maybeUndefinedId.isDefined) then ZIO.fail(CredentialIdNotDefined(maybeUndefinedId.get._1))
    else
      val idStateAndProof = credentialsAndProofs.map { credentialAndProof =>
        (
          extractIdFromCredential(credentialAndProof._1).get, // won't fail because of checks above
          IssueCredentialRecord.PublicationState.PublicationQueued,
          credentialAndProof._2
        )
      }

      credentialRepository
        .updateCredentialRecordStateAndProofByCredentialIdBulk(idStateAndProof)
        .mapError(RepositoryError(_))

  }

}
