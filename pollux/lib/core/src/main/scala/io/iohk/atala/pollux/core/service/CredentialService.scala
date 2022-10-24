package io.iohk.atala.pollux.core.service

import io.iohk.atala.pollux.core.model.CredentialError.RepositoryError
import io.iohk.atala.pollux.core.model.{CredentialError, JWTCredential}
import io.iohk.atala.pollux.core.repository.CredentialRepository
import io.iohk.atala.pollux.vc.jwt.{Issuer, IssuerDID}
import io.iohk.atala.iris.proto.service.IrisServiceGrpc.IrisServiceStub
import io.iohk.atala.prism.crypto.MerkleTreeKt
import io.iohk.atala.prism.crypto.Sha256
import zio.*

import java.security.spec.ECGenParameterSpec
import java.security.{KeyPairGenerator, SecureRandom}
import java.util.UUID
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
  val layer: URLayer[IrisServiceStub & CredentialRepository[Task], CredentialService] =
    ZLayer.fromFunction(CredentialServiceImpl(_, _))
}

private class CredentialServiceImpl(irisClient: IrisServiceStub, credentialRepository: CredentialRepository[Task])
    extends CredentialService {
  override def getCredentials(batchId: String): IO[CredentialError, Seq[JWTCredential]] = {
    credentialRepository.getCredentials(batchId).mapError(RepositoryError.apply)
  }

  override def createCredentials(batchId: String, credentials: Seq[JWTCredential]): IO[CredentialError, Unit] = {
    import collection.JavaConverters.*

    val hashes = credentials.map { c =>
      val encoded = JwtVerifiableCredential.encodeJwt(c.content, createIssuer).jwt
      val hash = Sha256.compute(encoded.getBytes)
      // val subjectDid = c.content.maybeSub
      hash
    }.toBuffer.asJava
    val merkleProofs = MerkleTreeKt.generateProofs(hashes)
    val root = merkleProofs.component1()
    val proofs = merkleProofs.component2().asScala
//    irisClient.s
    ZIO.unit
//    credentialRepository.createCredentials(batchId, credentials).mapError(RepositoryError.apply)
  }
}
