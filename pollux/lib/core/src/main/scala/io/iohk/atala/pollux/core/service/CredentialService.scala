package io.iohk.atala.pollux.core.service

import io.iohk.atala.pollux.core.model.IssueCredentialError.RepositoryError
import io.iohk.atala.pollux.core.model.IssueCredentialRecord
import io.iohk.atala.pollux.core.model.{IssueCredentialError, JWTCredential}
import io.iohk.atala.pollux.core.repository.CredentialRepository
import io.iohk.atala.pollux.vc.jwt.{Issuer, IssuerDID}
import zio.*

import java.security.spec.ECGenParameterSpec
import java.security.{KeyPairGenerator, SecureRandom}
import java.util.UUID
import java.{util => ju}
import java.{util => ju}
import cats.data.State

trait CredentialService {
  def createIssuer: Issuer = {
    val keyGen = KeyPairGenerator.getInstance("EC")
    val ecSpec = ECGenParameterSpec("secp256r1")
    keyGen.initialize(ecSpec, SecureRandom())
    val keyPair = keyGen.generateKeyPair()
    val privateKey = keyPair.getPrivate
    val publicKey = keyPair.getPublic
    // println(Base64.getEncoder.encodeToString(publicKey.getEncoded()))
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
  val layer = ZLayer.fromFunction(CredentialServiceImpl(_))
}

private class CredentialServiceImpl(credentialRepository: CredentialRepository[Task]) extends CredentialService {

  override def getCredentialRecords(): IO[IssueCredentialError, Seq[IssueCredentialRecord]] = ???

  override def createCredentialOffer(
      subjectId: String,
      schemaId: String,
      claims: Map[String, String],
      validityPeriod: Option[Double]
  ): IO[IssueCredentialError, IssueCredentialRecord] = ???

  override def acceptCredentialOffer(id: ju.UUID): IO[IssueCredentialError, IssueCredentialRecord] = ???

  override def issueCredential(id: ju.UUID): IO[IssueCredentialError, IssueCredentialRecord] = ???

  override def getCredentialRecord(id: ju.UUID): IO[IssueCredentialError, Option[IssueCredentialRecord]] = ???

  override def getCredentials(batchId: String): IO[IssueCredentialError, Seq[JWTCredential]] = {
    credentialRepository.getCredentials(batchId).mapError(RepositoryError.apply)
  }

  override def createCredentials(batchId: String, credentials: Seq[JWTCredential]): IO[IssueCredentialError, Unit] = {
    credentialRepository.createCredentials(batchId, credentials).mapError(RepositoryError.apply)
  }
}
