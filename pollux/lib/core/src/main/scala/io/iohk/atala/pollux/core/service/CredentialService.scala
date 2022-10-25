package io.iohk.atala.pollux.core.service

import io.iohk.atala.pollux.core.model.IssueCredentialError.RepositoryError
import io.iohk.atala.pollux.core.model.IssueCredentialRecord
import io.iohk.atala.pollux.core.model.{IssueCredentialError, JWTCredential}
import io.iohk.atala.pollux.core.repository.CredentialRepository
import io.iohk.atala.pollux.vc.jwt.{Issuer, IssuerDID}
import io.iohk.atala.iris.proto.service.IrisServiceGrpc.IrisServiceStub
import io.iohk.atala.prism.crypto.MerkleTreeKt
import io.iohk.atala.prism.crypto.Sha256
import zio.*

import java.security.spec.ECGenParameterSpec
import java.security.{KeyPairGenerator, SecureRandom}
import java.util.UUID
import java.{util => ju}
import java.{util => ju}
import cats.data.State
import io.iohk.atala.pollux.vc.jwt.JwtVerifiableCredential

trait CredentialService {
  def createIssuer: Issuer = {
    val keyGen = KeyPairGenerator.getInstance("EC")
    val ecSpec = ECGenParameterSpec("secp256r1")
    keyGen.initialize(ecSpec, SecureRandom())
    val keyPair = keyGen.generateKeyPair()
    val privateKey = keyPair.getPrivate
    val publicKey = keyPair.getPublic
    val uuid = UUID.randomUUID().toString
    Issuer(
      did = IssuerDID(s"did:prism:$uuid"),
      signer = io.iohk.atala.pollux.vc.jwt.ES256Signer(privateKey),
      publicKey = publicKey
    )
  }
  def createCredentials(batchId: String, credentials: Seq[JWTCredential]): IO[IssueCredentialError, Unit]
  def getCredentials(batchId: String): IO[IssueCredentialError, Seq[JWTCredential]]

  def createCredentialOffer(
      subjectId: String,
      schemaId: String,
      claims: Map[String, String],
      validityPeriod: Option[Double] = None
  ): IO[IssueCredentialError, IssueCredentialRecord]

  def getCredentialRecords(): IO[IssueCredentialError, Seq[IssueCredentialRecord]]

  def getCredentialRecord(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]]

  def acceptCredentialOffer(id: UUID): IO[IssueCredentialError, IssueCredentialRecord]

  def issueCredential(id: UUID): IO[IssueCredentialError, IssueCredentialRecord]

}

object MockCredentialService {
  val layer: ULayer[CredentialService] = ZLayer.succeed {
    new CredentialService {

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
            schemaId,
            subjectId,
            validityPeriod,
            claims,
            IssueCredentialRecord.State.OfferSent
          )
        )
      }

      override def acceptCredentialOffer(id: ju.UUID): IO[IssueCredentialError, IssueCredentialRecord] = ???

      override def issueCredential(id: ju.UUID): IO[IssueCredentialError, IssueCredentialRecord] = ???

      override def getCredentialRecord(id: ju.UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ???

      override def createCredentials(batchId: String, credentials: Seq[JWTCredential]): IO[IssueCredentialError, Unit] =
        ZIO.succeed(())

      override def getCredentials(did: String): IO[IssueCredentialError, Seq[JWTCredential]] = ZIO.succeed(Nil)
    }
  }
}

object CredentialServiceImpl {
  val layer: URLayer[IrisServiceStub & CredentialRepository[Task], CredentialService] =
    ZLayer.fromFunction(CredentialServiceImpl(_, _))
}

private class CredentialServiceImpl(irisClient: IrisServiceStub, credentialRepository: CredentialRepository[Task])
    extends CredentialService {
  override def getCredentials(batchId: String): IO[IssueCredentialError, Seq[JWTCredential]] = {
    credentialRepository.getCredentials(batchId).mapError(RepositoryError.apply)
  }

  override def createCredentials(batchId: String, credentials: Seq[JWTCredential]): IO[IssueCredentialError, Unit] = {
    import collection.JavaConverters.*

    val hashes = credentials
      .map { c =>
        val encoded = JwtVerifiableCredential.encodeJwt(c.content, createIssuer).jwt
        val hash = Sha256.compute(encoded.getBytes)
        // val subjectDid = c.content.maybeSub
        hash
      }
      .toBuffer
      .asJava
    val merkleProofs = MerkleTreeKt.generateProofs(hashes)
    val root = merkleProofs.component1()
    val proofs = merkleProofs.component2().asScala
//    irisClient.s
    ZIO.unit
//    credentialRepository.createCredentials(batchId, credentials).mapError(RepositoryError.apply)
  }

  def createCredentialOffer(
      subjectId: String,
      schemaId: String,
      claims: Map[String, String],
      validityPeriod: Option[Double] = None
  ): IO[IssueCredentialError, IssueCredentialRecord] = {
    for {
      record <- ZIO.succeed(
        IssueCredentialRecord(
          UUID.randomUUID(),
          schemaId,
          subjectId,
          validityPeriod,
          claims,
          IssueCredentialRecord.State.OfferSent
        )
      )
      _ <- credentialRepository
        .createIssueCredentialRecord(record)
        .mapError(RepositoryError.apply)
    } yield record
  }

  def getCredentialRecords(): IO[IssueCredentialError, Seq[IssueCredentialRecord]] = {
    for {
      records <- credentialRepository
        .getIssueCredentialRecords()
        .mapError(RepositoryError.apply)
    } yield records
  }

  def getCredentialRecord(id: UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = {
    for {
      record <- credentialRepository
        .getIssueCredentialRecord(id)
        .mapError(RepositoryError.apply)
    } yield record
  }

  def acceptCredentialOffer(id: UUID): IO[IssueCredentialError, IssueCredentialRecord] = ???

  def issueCredential(id: UUID): IO[IssueCredentialError, IssueCredentialRecord] = ???

}
