package io.iohk.atala.pollux.core.service

import com.google.protobuf.ByteString
import io.circe.Json
import io.circe.syntax.*
import io.iohk.atala.iris.proto.dlt.IrisOperation
import io.iohk.atala.iris.proto.service.IrisOperationId
import io.iohk.atala.iris.proto.service.IrisServiceGrpc.IrisServiceStub
import io.iohk.atala.iris.proto.vc_operations.IssueCredentialsBatch
import io.iohk.atala.pollux.core.model.EncodedJWTCredential
import io.iohk.atala.pollux.core.model.IssueCredentialRecord
import io.iohk.atala.pollux.core.model.PublishedBatchData
import io.iohk.atala.pollux.core.model.error.CreateCredentialPayloadFromRecordError
import io.iohk.atala.pollux.core.model.error.MarkCredentialRecordsAsPublishQueuedError
import io.iohk.atala.pollux.core.model.error.IssueCredentialError
import io.iohk.atala.pollux.core.model.error.IssueCredentialError._
import io.iohk.atala.pollux.core.model.error.PublishCredentialBatchError
import io.iohk.atala.pollux.core.repository.CredentialRepository
import io.iohk.atala.pollux.vc.jwt.*
import io.iohk.atala.prism.crypto.MerkleInclusionProof
import io.iohk.atala.prism.crypto.MerkleTreeKt
import io.iohk.atala.prism.crypto.Sha256
import zio.*

import java.rmi.UnexpectedException
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.util.UUID

import io.iohk.atala.mercury.protocol.issuecredential.OfferCredential
import io.iohk.atala.mercury.protocol.issuecredential.RequestCredential
import io.iohk.atala.mercury.protocol.issuecredential.IssueCredential
import io.iohk.atala.mercury.protocol.issuecredential.Attribute
import io.iohk.atala.mercury.protocol.issuecredential.CredentialPreview
import io.iohk.atala.mercury.model.AttachmentDescriptor
import io.iohk.atala.mercury.DidComm
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.model.Message

trait CredentialService {

  /** Copy pasted from Castor codebase for now TODO: replace with actual data from castor later
    *
    * @param method
    * @param methodSpecificId
    */
  final case class DID(
      method: String,
      methodSpecificId: String
  ) {
    override def toString: String = s"did:$method:$methodSpecificId"
  }

  // FIXME: this function is used as a temporary replacement
  // eventually, prism-agent should use castor library to get the issuer (issuance key and did)
  def createIssuer: Issuer = {
    val keyGen = KeyPairGenerator.getInstance("EC")
    val ecSpec = ECGenParameterSpec("secp256r1")
    keyGen.initialize(ecSpec, SecureRandom())
    val keyPair = keyGen.generateKeyPair()
    val privateKey = keyPair.getPrivate
    val publicKey = keyPair.getPublic
    val uuid = UUID.randomUUID().toString
    Issuer(
      did = io.iohk.atala.pollux.vc.jwt.DID(s"did:prism:$uuid"),
      signer = io.iohk.atala.pollux.vc.jwt.ES256Signer(privateKey),
      publicKey = publicKey
    )
  }

  def extractIdFromCredential(credential: W3cCredentialPayload): Option[UUID]

  def createIssueCredentialRecord(
      thid: UUID,
      subjectId: String,
      schemaId: Option[String],
      claims: Map[String, String],
      validityPeriod: Option[Double] = None,
      automaticIssuance: Option[Boolean],
      awaitConfirmation: Option[Boolean]
  ): IO[IssueCredentialError, IssueCredentialRecord]

  def getIssueCredentialRecords(): IO[IssueCredentialError, Seq[IssueCredentialRecord]]

  def getCredentialRecordsByState(
      state: IssueCredentialRecord.ProtocolState
  ): IO[IssueCredentialError, Seq[IssueCredentialRecord]]

  def getIssueCredentialRecord(recordId: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def receiveCredentialOffer(offer: OfferCredential): IO[IssueCredentialError, IssueCredentialRecord]

  def acceptCredentialOffer(recordId: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def receiveCredentialRequest(request: RequestCredential): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def acceptCredentialRequest(recordId: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def createCredentialPayloadFromRecord(
      record: IssueCredentialRecord,
      issuer: Issuer,
      issuanceDate: Instant
  ): IO[CreateCredentialPayloadFromRecordError, W3cCredentialPayload]

  def publishCredentialBatch(
      credentials: Seq[W3cCredentialPayload],
      issuer: Issuer
  ): IO[PublishCredentialBatchError, PublishedBatchData]

  def markCredentialRecordsAsPublishQueued(
      credentialsAndProofs: Seq[(W3cCredentialPayload, MerkleInclusionProof)]
  ): IO[MarkCredentialRecordsAsPublishQueuedError, Int]

  def receiveCredentialIssue(issue: IssueCredential): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def markOfferSent(recordId: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def markRequestSent(recordId: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def markCredentialGenerated(recordId: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def markCredentialSent(recordId: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def markCredentialPublicationPending(recordId: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def markCredentialPublicationQueued(recordId: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def markCredentialPublished(recordId: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

}

object CredentialServiceImpl {
  val layer: URLayer[IrisServiceStub & CredentialRepository[Task] & DidComm, CredentialService] =
    ZLayer.fromFunction(CredentialServiceImpl(_, _, _))
}

private class CredentialServiceImpl(
    irisClient: IrisServiceStub,
    credentialRepository: CredentialRepository[Task],
    didComm: DidComm
) extends CredentialService {

  import IssueCredentialRecord._

  override def extractIdFromCredential(credential: W3cCredentialPayload): Option[UUID] =
    credential.maybeId.map(_.split("/").last).map(UUID.fromString)

  override def getIssueCredentialRecords(): IO[IssueCredentialError, Seq[IssueCredentialRecord]] = {
    for {
      records <- credentialRepository
        .getIssueCredentialRecords()
        .mapError(RepositoryError.apply)
    } yield records
  }

  override def getIssueCredentialRecord(recordId: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = {
    for {
      record <- credentialRepository
        .getIssueCredentialRecord(recordId)
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def createIssueCredentialRecord(
      thid: UUID,
      subjectId: String,
      schemaId: Option[String],
      claims: Map[String, String],
      validityPeriod: Option[Double],
      automaticIssuance: Option[Boolean],
      awaitConfirmation: Option[Boolean]
  ): IO[IssueCredentialError, IssueCredentialRecord] = {
    for {
      offer <- ZIO.succeed(createDidCommOfferCredential(claims, thid, subjectId))
      record <- ZIO.succeed(
        IssueCredentialRecord(
          id = UUID.randomUUID(),
          credentialId = UUID.randomUUID(),
          merkleInclusionProof = None,
          thid = thid,
          schemaId = schemaId,
          role = IssueCredentialRecord.Role.Issuer,
          subjectId = subjectId,
          validityPeriod = validityPeriod,
          automaticIssuance = automaticIssuance,
          awaitConfirmation = awaitConfirmation,
          protocolState = IssueCredentialRecord.ProtocolState.OfferPending,
          publicationState = None,
          offerCredentialData = Some(offer),
          requestCredentialData = None,
          issueCredentialData = None
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

  override def getCredentialRecordsByState(
      state: IssueCredentialRecord.ProtocolState
  ): IO[IssueCredentialError, Seq[IssueCredentialRecord]] = {
    for {
      records <- credentialRepository
        .getIssueCredentialRecordsByState(state)
        .mapError(RepositoryError.apply)
    } yield records
  }

  override def receiveCredentialOffer(
      offer: OfferCredential
  ): IO[IssueCredentialError, IssueCredentialRecord] = {
    for {
      record <- ZIO.succeed(
        IssueCredentialRecord(
          id = UUID.randomUUID(),
          credentialId = UUID.randomUUID(),
          merkleInclusionProof = None,
          thid = UUID.fromString(offer.thid.getOrElse(offer.id)),
          schemaId = None,
          role = IssueCredentialRecord.Role.Holder,
          subjectId = offer.to.value,
          validityPeriod = None,
          automaticIssuance = None,
          awaitConfirmation = None,
          protocolState = IssueCredentialRecord.ProtocolState.OfferReceived,
          publicationState = None,
          offerCredentialData = Some(offer),
          requestCredentialData = None,
          issueCredentialData = None
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

  override def acceptCredentialOffer(recordId: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = {
    for {
      maybeRecord <- credentialRepository
        .getIssueCredentialRecord(recordId)
        .mapError(RepositoryError.apply)
      record <- ZIO
        .fromOption(maybeRecord)
        .mapError(_ => RecordIdNotFound(recordId))
      offer <- ZIO
        .fromOption(record.offerCredentialData)
        .mapError(_ => InvalidFlowStateError(s"No offer found for this record: $recordId"))
      request = createDidCommRequestCredential(offer)
      count <- credentialRepository
        .updateWithRequestCredential(recordId, request, ProtocolState.RequestPending)
        .mapError(RepositoryError.apply)
      _ <- count match
        case 1 => ZIO.succeed(())
        case n => ZIO.fail(RecordIdNotFound(recordId))
      record <- credentialRepository
        .getIssueCredentialRecord(record.id)
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def receiveCredentialRequest(
      request: RequestCredential
  ): IO[IssueCredentialError, Option[IssueCredentialRecord]] = {
    for {
      record <- getRecordFromThreadId(request.thid)
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
    } yield record
  }

  override def acceptCredentialRequest(recordId: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = {
    for {
      maybeRecord <- credentialRepository
        .getIssueCredentialRecord(recordId)
        .mapError(RepositoryError.apply)
      record <- ZIO
        .fromOption(maybeRecord)
        .mapError(_ => RecordIdNotFound(recordId))
      request <- ZIO
        .fromOption(record.requestCredentialData)
        .mapError(_ => InvalidFlowStateError(s"No request found for this record: $recordId"))
      // TODO: Generate the JWT credential and use it to create the IssueCredential object
      issue = createDidCommIssueCredential(request)
      count <- credentialRepository
        .updateWithIssueCredential(recordId, issue, ProtocolState.CredentialPending)
        .mapError(RepositoryError.apply)
      _ <- count match
        case 1 => ZIO.succeed(())
        case n => ZIO.fail(RecordIdNotFound(recordId))
      record <- credentialRepository
        .getIssueCredentialRecord(record.id)
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def receiveCredentialIssue(
      issue: IssueCredential
  ): IO[IssueCredentialError, Option[IssueCredentialRecord]] = {
    for {
      record <- getRecordFromThreadId(issue.thid)
      _ <- credentialRepository
        .updateWithIssueCredential(record.id, issue, ProtocolState.CredentialReceived)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
      record <- credentialRepository
        .getIssueCredentialRecord(record.id)
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def markOfferSent(recordId: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] =
    updateCredentialRecordProtocolState(
      recordId,
      IssueCredentialRecord.ProtocolState.OfferPending,
      IssueCredentialRecord.ProtocolState.OfferSent
    )

  override def markRequestSent(recordId: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] =
    updateCredentialRecordProtocolState(
      recordId,
      IssueCredentialRecord.ProtocolState.RequestPending,
      IssueCredentialRecord.ProtocolState.RequestSent
    )

  override def markCredentialGenerated(recordId: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] =
    updateCredentialRecordProtocolState(
      recordId,
      IssueCredentialRecord.ProtocolState.CredentialPending,
      IssueCredentialRecord.ProtocolState.CredentialGenerated
    )

  override def markCredentialSent(recordId: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] =
    updateCredentialRecordProtocolState(
      recordId,
      IssueCredentialRecord.ProtocolState.CredentialGenerated,
      IssueCredentialRecord.ProtocolState.CredentialSent
    )

  override def markCredentialPublicationPending(
      recordId: UUID
  ): IO[IssueCredentialError, Option[IssueCredentialRecord]] =
    updateCredentialRecordPublicationState(
      recordId,
      None,
      Some(IssueCredentialRecord.PublicationState.PublicationPending)
    )

  override def markCredentialPublicationQueued(
      recordId: UUID
  ): IO[IssueCredentialError, Option[IssueCredentialRecord]] =
    updateCredentialRecordPublicationState(
      recordId,
      Some(IssueCredentialRecord.PublicationState.PublicationPending),
      Some(IssueCredentialRecord.PublicationState.PublicationQueued)
    )

  override def markCredentialPublished(recordId: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] =
    updateCredentialRecordPublicationState(
      recordId,
      Some(IssueCredentialRecord.PublicationState.PublicationQueued),
      Some(IssueCredentialRecord.PublicationState.Published)
    )

  private[this] def getRecordFromThreadId(
      thid: Option[String]
  ): IO[IssueCredentialError, IssueCredentialRecord] = {
    for {
      thid <- ZIO
        .fromOption(thid)
        .mapError(_ => UnexpectedError("No `thid` found in credential request"))
        .map(UUID.fromString)
      maybeRecord <- credentialRepository
        .getIssueCredentialRecordByThreadId(thid)
        .mapError(RepositoryError.apply)
      record <- ZIO
        .fromOption(maybeRecord)
        .mapError(_ => ThreadIdNotFound(thid))
    } yield record
  }

  private[this] def createDidCommOfferCredential(
      claims: Map[String, String],
      thid: UUID,
      subjectId: String
  ): OfferCredential = {
    val attributes = claims.map { case (k, v) => Attribute(k, v) }
    val credentialPreview = CredentialPreview(attributes = attributes.toSeq)
    val body = OfferCredential.Body(goal_code = Some("Offer Credential"), credential_preview = credentialPreview)
    val attachmentDescriptor = AttachmentDescriptor.buildAttachment[CredentialPreview](payload = credentialPreview)

    OfferCredential(
      body = body,
      attachments = Seq(attachmentDescriptor),
      to = DidId(subjectId),
      from = didComm.myDid,
      thid = Some(thid.toString())
    )
  }

  private[this] def createDidCommRequestCredential(offer: OfferCredential): RequestCredential = {
    RequestCredential(
      body = RequestCredential.Body(
        goal_code = offer.body.goal_code,
        comment = offer.body.comment,
        formats = offer.body.formats
      ),
      attachments = offer.attachments,
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
        formats = request.body.formats
      ),
      attachments = request.attachments,
      thid = request.thid.orElse(Some(request.id)),
      from = request.to,
      to = request.from
    )
  }

  private[this] def updateCredentialRecordProtocolState(
      id: UUID,
      from: IssueCredentialRecord.ProtocolState,
      to: IssueCredentialRecord.ProtocolState
  ): IO[IssueCredentialError, Option[IssueCredentialRecord]] = {
    for {
      outcome <- credentialRepository
        .updateCredentialRecordProtocolState(id, from, to)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
      record <- credentialRepository
        .getIssueCredentialRecord(id)
        .mapError(RepositoryError.apply)
    } yield record
  }

  private[this] def updateCredentialRecordPublicationState(
      id: UUID,
      from: Option[IssueCredentialRecord.PublicationState],
      to: Option[IssueCredentialRecord.PublicationState]
  ): IO[IssueCredentialError, Option[IssueCredentialRecord]] = {
    for {
      outcome <- credentialRepository
        .updateCredentialRecordPublicationState(id, from, to)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
      record <- credentialRepository
        .getIssueCredentialRecord(id)
        .mapError(RepositoryError.apply)
    } yield record
  }

  private def sendCredential(
      jwtCredential: JwtCredentialPayload,
      holderDid: DID,
      inclusionProof: MerkleInclusionProof
  ): Nothing = ???

  override def createCredentialPayloadFromRecord(
      record: IssueCredentialRecord,
      issuer: Issuer,
      issuanceDate: Instant
  ): IO[CreateCredentialPayloadFromRecordError, W3cCredentialPayload] = {

    val claims = for {
      requestCredential <- record.requestCredentialData
      preview <- requestCredential.getCredential[CredentialPreview]("credential-preview")
      claims <- Some(preview.attributes.map(attr => attr.name -> attr.value).toMap)
    } yield claims

    val credential = for {
      claims <- ZIO.fromEither(
        Either.cond(
          claims.isDefined,
          claims.get,
          CreateCredentialPayloadFromRecordError.CouldNotExtractClaimsError(
            new Throwable("Could not extract claims from \"requestCredential\" Didcome message")
          )
        )
      )
      // TODO: get schema when schema registry is available if schema ID is provided
      credential = W3cCredentialPayload(
        `@context` = Set.empty, // TODO: his information should come from Schema registry by record.schemaId
        maybeId = None,
        `type` = Set.empty, // TODO: This information should come from Schema registry by record.schemaId
        issuer = issuer.did,
        issuanceDate = issuanceDate,
        maybeExpirationDate = record.validityPeriod.map(sec => issuanceDate.plusSeconds(sec.toLong)),
        maybeCredentialSchema = None,
        credentialSubject = claims.updated("id", record.subjectId).asJson,
        maybeCredentialStatus = None,
        maybeRefreshService = None,
        maybeEvidence = None,
        maybeTermsOfUse = None
      )
    } yield credential

    credential

  }

  def publishCredentialBatch(
      credentials: Seq[W3cCredentialPayload],
      issuer: Issuer
  ): IO[PublishCredentialBatchError, PublishedBatchData] = {
    import collection.JavaConverters.*

    val hashes = credentials
      .map { c =>
        val encoded = JwtCredential.toEncodedJwt(c, issuer)
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
        PublishCredentialBatchError.IrisError(_),
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
  ): IO[MarkCredentialRecordsAsPublishQueuedError, Int] = {

    /*
     * Since id of the credential is optional according to W3 spec,
     * it is of a type Option in W3cCredentialPayload since it is a generic W3 credential payload
     * but for our use-case, credentials must have an id, so if for some reason at least one
     * credential does not have an id, we return an error
     *
     */
    val maybeUndefinedId = credentialsAndProofs.find(x => extractIdFromCredential(x._1).isEmpty)

    if (maybeUndefinedId.isDefined) then
      ZIO.fail(MarkCredentialRecordsAsPublishQueuedError.CredentialIdNotDefined(maybeUndefinedId.get._1))
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
        .mapError(MarkCredentialRecordsAsPublishQueuedError.RepositoryError(_))

  }

}
