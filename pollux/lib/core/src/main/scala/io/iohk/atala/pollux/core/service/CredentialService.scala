package io.iohk.atala.pollux.core.service

//import cats.data.State
import com.google.protobuf.ByteString
import io.iohk.atala.iris.proto.dlt.IrisOperation
import io.iohk.atala.iris.proto.service.IrisOperationId
import io.iohk.atala.iris.proto.service.IrisServiceGrpc.IrisServiceStub
import io.iohk.atala.iris.proto.vc_operations.IssueCredentialsBatch
import io.iohk.atala.pollux.core.model.EncodedJWTCredential
import io.iohk.atala.pollux.core.model.IssueCredentialRecord
import io.iohk.atala.pollux.core.model.PublishedBatchData
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
import java.util.UUID
import java.{util => ju}
import io.iohk.atala.mercury.protocol.issuecredential.OfferCredential
import io.iohk.atala.mercury.protocol.issuecredential.RequestCredential
import io.iohk.atala.mercury.protocol.issuecredential.IssueCredential

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
  def createCredentials(batchId: String, credentials: Seq[EncodedJWTCredential]): IO[IssueCredentialError, Unit]
  def getCredentials(batchId: String): IO[IssueCredentialError, Seq[EncodedJWTCredential]]

  def createCredentialOffer(
      subjectId: String,
      schemaId: Option[String],
      claims: Map[String, String],
      validityPeriod: Option[Double] = None
  ): IO[IssueCredentialError, IssueCredentialRecord]

  def getCredentialRecords(): IO[IssueCredentialError, Seq[IssueCredentialRecord]]

  def getCredentialRecord(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def receiveCredentialOffer(offer: OfferCredential): IO[IssueCredentialError, IssueCredentialRecord]

  def acceptCredentialOffer(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def receiveCredentialRequest(request: RequestCredential): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def issueCredential(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def receiveCredentialIssue(issue: IssueCredential): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def markOfferSent(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def markRequestSent(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def markCredentialSent(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def markCredentialPublicationQueued(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def markCredentialPublished(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

}

object CredentialServiceImpl {
  val layer: URLayer[IrisServiceStub & CredentialRepository[Task], CredentialService] =
    ZLayer.fromFunction(CredentialServiceImpl(_, _))
}

private class CredentialServiceImpl(irisClient: IrisServiceStub, credentialRepository: CredentialRepository[Task])
    extends CredentialService {

  override def markCredentialSent(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ???

  override def markCredentialPublicationQueued(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ???

  override def markRequestSent(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ???

  override def markCredentialPublished(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ???

  override def markOfferSent(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ???

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
      schemaId: Option[String],
      claims: Map[String, String],
      validityPeriod: Option[Double] = None
  ): IO[IssueCredentialError, IssueCredentialRecord] = {
    for {
      record <- ZIO.succeed(
        IssueCredentialRecord(
          UUID.randomUUID(),
          schemaId,
          IssueCredentialRecord.Role.Issuer,
          subjectId,
          validityPeriod,
          claims,
          IssueCredentialRecord.State.OfferPending,
          offerData = None,
          requestCredentialData = None
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
          UUID.randomUUID(),
          None,
          IssueCredentialRecord.Role.Holder,
          offer.to.value,
          None,
          Map.empty,
          IssueCredentialRecord.State.OfferReceived,
          offerData = Some(offer),
          requestCredentialData = None
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
    updateCredentialRecordState(
      id,
      IssueCredentialRecord.State.OfferReceived,
      IssueCredentialRecord.State.RequestPending
    )

  override def receiveCredentialRequest(
      request: RequestCredential
  ): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ??? // FIXME

  override def issueCredential(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] =
    updateCredentialRecordState(
      id,
      IssueCredentialRecord.State.RequestReceived,
      IssueCredentialRecord.State.CredentialPending
    )

  override def receiveCredentialIssue(
      issue: IssueCredential
  ): IO[IssueCredentialError, Option[IssueCredentialRecord]] = {
    ???
  }

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
