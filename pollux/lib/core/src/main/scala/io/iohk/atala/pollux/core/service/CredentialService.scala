package io.iohk.atala.pollux.core.service

import io.iohk.atala.pollux.core.model.CredentialError.RepositoryError
import io.iohk.atala.pollux.core.model.{CredentialError, JWTCredential}
import io.iohk.atala.pollux.core.repository.CredentialRepository
import io.iohk.atala.pollux.vc.jwt.{Issuer, IssuerDID}
import zio.*

import java.security.spec.ECGenParameterSpec
import java.security.{KeyPairGenerator, SecureRandom}
import java.util.UUID

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
  def createCredentials(batchId: String, credentials: Seq[JWTCredential]): IO[CredentialError, Unit]
  def getCredentials(batchId: String): IO[CredentialError, Seq[JWTCredential]]
}

object MockCredentialService {
  val layer: ULayer[CredentialService] = ZLayer.succeed {
    new CredentialService {
      override def createCredentials(batchId: String, credentials: Seq[JWTCredential]): IO[CredentialError, Unit] =
        ZIO.succeed(())

      override def getCredentials(did: String): IO[CredentialError, Seq[JWTCredential]] = ZIO.succeed(Nil)
    }
  }
}

object CredentialServiceImpl {
  val layer = ZLayer.fromFunction(CredentialServiceImpl(_))
}

private class CredentialServiceImpl(credentialRepository: CredentialRepository[Task]) extends CredentialService {
  override def getCredentials(batchId: String): IO[CredentialError, Seq[JWTCredential]] = {
    credentialRepository.getCredentials(batchId).mapError(RepositoryError.apply)
  }

  override def createCredentials(batchId: String, credentials: Seq[JWTCredential]): IO[CredentialError, Unit] = {
    credentialRepository.createCredentials(batchId, credentials).mapError(RepositoryError.apply)
  }
}
