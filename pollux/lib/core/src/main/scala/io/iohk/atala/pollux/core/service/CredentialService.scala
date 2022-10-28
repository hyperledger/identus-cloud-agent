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

trait CredentialService {

  /** Copy pasted from Castor codebase for now TODO: replace with actual data from castor laster
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

  def createCredentials(batchId: String, credentials: Seq[EncodedJWTCredential]): IO[IssueCredentialError, Unit]

  def getCredentials(batchId: String): IO[IssueCredentialError, Seq[EncodedJWTCredential]]

  def createCredentialOffer(
      subjectId: String,
      schemaId: String,
      claims: Map[String, String],
      validityPeriod: Option[Double] = None
  ): IO[IssueCredentialError, IssueCredentialRecord]

  def getCredentialRecords(): IO[IssueCredentialError, Seq[IssueCredentialRecord]]

  def getCredentialRecordsByState(
      state: IssueCredentialRecord.State
  ): IO[IssueCredentialError, Seq[IssueCredentialRecord]]

  def getCredentialRecord(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def acceptCredentialOffer(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def issueCredential(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def createCredentialPayloadFromRecord(
      record: IssueCredentialRecord,
      issuer: Issuer,
      issuanceDate: Instant
  ): IO[CreateCredentialPayloadFromRecordError, W3cCredentialPayload]

}

object MockCredentialService {
  val layer: ULayer[CredentialService] = ZLayer.succeed {
    new CredentialService {

      override def extractIdFromCredential(credential: W3cCredentialPayload): Option[UUID] = ???

      override def createCredentialPayloadFromRecord(
          record: IssueCredentialRecord,
          issuer: Issuer,
          issuanceDate: Instant
      ): IO[CreateCredentialPayloadFromRecordError, W3cCredentialPayload] = ???

      override def getCredentialRecordsByState(
          state: IssueCredentialRecord.State
      ): IO[IssueCredentialError, Seq[IssueCredentialRecord]] = ???

      override def getCredentialRecords(): IO[IssueCredentialError, Seq[IssueCredentialRecord]] = ???

      override def createCredentialOffer(
          subjectId: String,
          schemaId: String,
          claims: Map[String, String],
          validityPeriod: Option[Double]
      ): IO[IssueCredentialError, IssueCredentialRecord] = {
        ZIO.succeed(
          IssueCredentialRecord(
            UUID.randomUUID(),
            UUID.randomUUID(),
            schemaId,
            subjectId,
            validityPeriod,
            claims,
            IssueCredentialRecord.State.OfferSent
          )
        )
      }

      override def acceptCredentialOffer(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ???

      override def issueCredential(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ???

      override def getCredentialRecord(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ???

      override def createCredentials(
          batchId: String,
          credentials: Seq[EncodedJWTCredential]
      ): IO[IssueCredentialError, Unit] =
        ZIO.succeed(())

      override def getCredentials(did: String): IO[IssueCredentialError, Seq[EncodedJWTCredential]] = ZIO.succeed(Nil)
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

  override def getCredentials(batchId: String): IO[IssueCredentialError, Seq[EncodedJWTCredential]] = {
    credentialRepository.getCredentials(batchId).mapError(IssueCredentialError.RepositoryError.apply)
  }

  override def createCredentials(
      batchId: String,
      credentials: Seq[EncodedJWTCredential]
  ): IO[IssueCredentialError, Unit] = {

    credentialRepository.createCredentials(batchId, credentials).mapError(IssueCredentialError.RepositoryError.apply)
  }

  override def createCredentialOffer(
      subjectId: String,
      schemaId: String,
      claims: Map[String, String],
      validityPeriod: Option[Double] = None
  ): IO[IssueCredentialError, IssueCredentialRecord] = {
    for {
      record <- ZIO.succeed(
        IssueCredentialRecord(
          UUID.randomUUID(),
          UUID.randomUUID(),
          schemaId,
          subjectId,
          validityPeriod,
          claims,
          IssueCredentialRecord.State.OfferPending
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
      state: IssueCredentialRecord.State
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

  override def acceptCredentialOffer(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] =
    updateCredentialRecordState(
      id,
      IssueCredentialRecord.State.OfferReceived,
      IssueCredentialRecord.State.RequestPending
    )

  override def issueCredential(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] =
    updateCredentialRecordState(
      id,
      IssueCredentialRecord.State.RequestReceived,
      IssueCredentialRecord.State.CredentialPending
    )

  private[this] def updateCredentialRecordState(
      id: UUID,
      from: IssueCredentialRecord.State,
      to: IssueCredentialRecord.State
  ): IO[IssueCredentialError, Option[IssueCredentialRecord]] = {
    for {
      outcome <- credentialRepository
        .updateCredentialRecordState(id, from, to)
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
  ): IO[CreateCredentialPayloadFromRecordError, W3cCredentialPayload] = { // This function will get schema from database when it is available, that's why it returns IO
    val now = Instant.now()
    val claims = record.claims.map(kv => kv._1 -> Json.fromString(kv._2))
    val schemas = Set( // TODO: This information should come from Schema registry by record.schemaId
      "https://www.w3.org/2018/credentials/v1"
    )
    ZIO.succeed(
      W3cCredentialPayload(
        `@context` = schemas,
        maybeId = Some(s"https://atala.io/prism/credentials/${record.credentialId.toString}"), //TODO: this URL should probably come from env or config
        `type` =
          Set("VerifiableCredential"), // TODO: This information should come from Schema registry by record.schemaId
        issuer = issuer.did,
        issuanceDate = issuanceDate,
        maybeExpirationDate = record.validityPeriod.map(sec => now.plusSeconds(sec.toLong)),
        maybeCredentialSchema = None,
        credentialSubject = claims.updated("id", Json.fromString(record.subjectId)).asJson,
        maybeCredentialStatus = None,
        maybeRefreshService = None,
        maybeEvidence = None,
        maybeTermsOfUse = None
      )
    )

  }

  private def publishCredentialBatch(
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

}
