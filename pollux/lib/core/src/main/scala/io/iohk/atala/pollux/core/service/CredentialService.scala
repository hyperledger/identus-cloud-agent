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

  def createCredentialOffer(
      thid: UUID,
      subjectId: String,
      schemaId: Option[String],
      claims: Map[String, String],
      validityPeriod: Option[Double] = None
  ): IO[IssueCredentialError, IssueCredentialRecord]

  def getCredentialRecords(): IO[IssueCredentialError, Seq[IssueCredentialRecord]]

  def getCredentialRecordsByState(
      state: IssueCredentialRecord.ProtocolState
  ): IO[IssueCredentialError, Seq[IssueCredentialRecord]]

  def getCredentialRecord(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def receiveCredentialOffer(offer: OfferCredential): IO[IssueCredentialError, IssueCredentialRecord]

  def acceptCredentialOffer(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def receiveCredentialRequest(request: RequestCredential): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def issueCredential(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

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

  def markOfferSent(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def markRequestSent(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def markCredentialSent(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def markCredentialPublicationPending(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def markCredentialPublicationQueued(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def markCredentialPublished(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

}

object MockCredentialService {

  val layer: ULayer[CredentialService] = ZLayer.succeed {
    new CredentialService {

      override def receiveCredentialIssue(
          issue: IssueCredential
      ): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ???

      override def markCredentialSent(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ???

      override def receiveCredentialRequest(
          request: RequestCredential
      ): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ???

      override def receiveCredentialOffer(offer: OfferCredential): IO[IssueCredentialError, IssueCredentialRecord] = ???

      override def markCredentialPublicationQueued(
          id: UUID
      ): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ???

      override def markCredentialPublicationPending(
          id: UUID
      ): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ???

      override def markOfferSent(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ???

      override def markCredentialPublished(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ???

      override def markRequestSent(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ???

      override def markCredentialRecordsAsPublishQueued(
          credentialsAndProofs: Seq[(W3cCredentialPayload, MerkleInclusionProof)]
      ): IO[MarkCredentialRecordsAsPublishQueuedError, Int] = ???

      override def publishCredentialBatch(
          credentials: Seq[W3cCredentialPayload],
          issuer: Issuer
      ): IO[PublishCredentialBatchError, PublishedBatchData] = ???

      override def extractIdFromCredential(credential: W3cCredentialPayload): Option[UUID] = ???

      override def createCredentialPayloadFromRecord(
          record: IssueCredentialRecord,
          issuer: Issuer,
          issuanceDate: Instant
      ): IO[CreateCredentialPayloadFromRecordError, W3cCredentialPayload] = ???

      override def getCredentialRecordsByState(
          state: IssueCredentialRecord.ProtocolState
      ): IO[IssueCredentialError, Seq[IssueCredentialRecord]] = ???

      override def getCredentialRecords(): IO[IssueCredentialError, Seq[IssueCredentialRecord]] = ???

      override def createCredentialOffer(
          thid: UUID,
          subjectId: String,
          schemaId: Option[String],
          claims: Map[String, String],
          validityPeriod: Option[Double]
      ): IO[IssueCredentialError, IssueCredentialRecord] = {
        ZIO.succeed(
          IssueCredentialRecord(
            id = UUID.randomUUID(),
            credentialId = UUID.randomUUID(),
            merkleInclusionProof = None,
            thid = thid,
            schemaId = schemaId,
            IssueCredentialRecord.Role.Issuer,
            subjectId = subjectId,
            validityPeriod = validityPeriod,
            claims = claims,
            protocolState = IssueCredentialRecord.ProtocolState.OfferPending,
            publicationState = None,
            offerCredentialData = None,
            requestCredentialData = None,
            issueCredentialData = None
          )
        )
      }

      override def acceptCredentialOffer(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ???

      override def issueCredential(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ???

      override def getCredentialRecord(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ???
    }

  }

}

object CredentialServiceImpl {
  val layer: URLayer[IrisServiceStub & CredentialRepository[Task], CredentialService] =
    ZLayer.fromFunction(CredentialServiceImpl(_, _))
}

private class CredentialServiceImpl(irisClient: IrisServiceStub, credentialRepository: CredentialRepository[Task])
    extends CredentialService {

  override def extractIdFromCredential(credential: W3cCredentialPayload): Option[UUID] =
    credential.maybeId.map(_.split("/").last).map(UUID.fromString)

  override def markOfferSent(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] =
    updateCredentialRecordProtocolState(
      id,
      IssueCredentialRecord.ProtocolState.OfferPending,
      IssueCredentialRecord.ProtocolState.OfferSent
    )

  override def markRequestSent(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] =
    updateCredentialRecordProtocolState(
      id,
      IssueCredentialRecord.ProtocolState.RequestPending,
      IssueCredentialRecord.ProtocolState.RequestSent
    )

  override def markCredentialSent(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] =
    updateCredentialRecordProtocolState(
      id,
      IssueCredentialRecord.ProtocolState.CredentialPending,
      IssueCredentialRecord.ProtocolState.CredentialSent
    )

  override def markCredentialPublicationPending(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] =
    updateCredentialRecordPublicationState(
      id,
      None,
      Some(IssueCredentialRecord.PublicationState.PublicationPending)
    )

  override def markCredentialPublicationQueued(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] =
    updateCredentialRecordPublicationState(
      id,
      Some(IssueCredentialRecord.PublicationState.PublicationPending),
      Some(IssueCredentialRecord.PublicationState.PublicationQueued)
    )

  override def markCredentialPublished(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] =
    updateCredentialRecordPublicationState(
      id,
      Some(IssueCredentialRecord.PublicationState.PublicationQueued),
      Some(IssueCredentialRecord.PublicationState.Published)
    )

  override def createCredentialOffer(
      thid: UUID,
      subjectId: String,
      schemaId: Option[String],
      claims: Map[String, String],
      validityPeriod: Option[Double] = None
  ): IO[IssueCredentialError, IssueCredentialRecord] = {
    for {
      record <- ZIO.succeed(
        IssueCredentialRecord(
          id = UUID.randomUUID(),
          credentialId = UUID.randomUUID(),
          merkleInclusionProof = None,
          thid = thid,
          schemaId = schemaId,
          IssueCredentialRecord.Role.Issuer,
          subjectId = subjectId,
          validityPeriod = validityPeriod,
          claims = claims,
          protocolState = IssueCredentialRecord.ProtocolState.OfferPending,
          publicationState = None,
          offerCredentialData = None,
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

  override def getCredentialRecords(): IO[IssueCredentialError, Seq[IssueCredentialRecord]] = {
    for {
      records <- credentialRepository
        .getIssueCredentialRecords()
        .mapError(RepositoryError.apply)
    } yield records
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

  override def getCredentialRecord(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = {
    for {
      record <- credentialRepository
        .getIssueCredentialRecord(id)
        .mapError(RepositoryError.apply)
    } yield record
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
          claims = Map.empty,
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

  override def acceptCredentialOffer(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] =
    updateCredentialRecordProtocolState(
      id,
      IssueCredentialRecord.ProtocolState.OfferReceived,
      IssueCredentialRecord.ProtocolState.RequestPending
    )

  override def receiveCredentialRequest(
      request: RequestCredential
  ): IO[IssueCredentialError, Option[IssueCredentialRecord]] = {
    for {
      thid <- ZIO.succeed(UUID.fromString(request.thid.getOrElse(request.id)))
      _ <- credentialRepository
        .updateWithRequestCredential(request)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
      record <- credentialRepository
        .getIssueCredentialRecord(thid)
        .mapError(RepositoryError.apply)
    } yield record
  }

  override def issueCredential(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] =
    updateCredentialRecordProtocolState(
      id,
      IssueCredentialRecord.ProtocolState.RequestReceived,
      IssueCredentialRecord.ProtocolState.CredentialPending
    )

  override def receiveCredentialIssue(
      issue: IssueCredential
  ): IO[IssueCredentialError, Option[IssueCredentialRecord]] = {
    for {
      thid <- ZIO.succeed(UUID.fromString(issue.thid.getOrElse(issue.id)))
      _ <- credentialRepository
        .updateWithIssueCredential(issue)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
      record <- credentialRepository
        .getIssueCredentialRecordByThreadId(thid)
        .mapError(RepositoryError.apply)
    } yield record
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
      // This function will get schema from database when it is available
  ): IO[CreateCredentialPayloadFromRecordError, W3cCredentialPayload] = {
    val claims = record.claims.map(kv => kv._1 -> Json.fromString(kv._2))
    val schemas = Set( // TODO: This information should come from Schema registry by record.schemaId
      "https://www.w3.org/2018/credentials/v1"
    )
    ZIO.succeed(
      W3cCredentialPayload(
        `@context` = schemas,
        // credential ID is optional id W3 spec but in PRISM use-case they have an ID always
        // NOTE: We should support PrismCredential data type where all required fields for our use-case are not optional
        maybeId = Some(
          s"https://atala.io/prism/credentials/${record.credentialId.toString}"
        ), // TODO: this URL prefix should come from env or config
        `type` =
          Set("VerifiableCredential"), // TODO: This information should come from Schema registry by record.schemaId
        issuer = issuer.did,
        issuanceDate = issuanceDate,
        maybeExpirationDate = record.validityPeriod.map(sec => issuanceDate.plusSeconds(sec.toLong)),
        maybeCredentialSchema = None,
        credentialSubject = claims.updated("id", Json.fromString(record.subjectId)).asJson,
        maybeCredentialStatus = None,
        maybeRefreshService = None,
        maybeEvidence = None,
        maybeTermsOfUse = None
      )
    )

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
