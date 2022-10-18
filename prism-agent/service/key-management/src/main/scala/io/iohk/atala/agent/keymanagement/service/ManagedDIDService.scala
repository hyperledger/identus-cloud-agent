package io.iohk.atala.agent.keymanagement.service

import io.iohk.atala.agent.keymanagement.crypto.KeyGeneratorWrapper
import io.iohk.atala.agent.keymanagement.model.{DIDPublicKeyTemplate, ECKeyPair, ManagedDIDTemplate}
import io.iohk.atala.agent.keymanagement.model.ECCoordinates.*
import io.iohk.atala.agent.keymanagement.model.error.CreateManagedDIDError
import io.iohk.atala.agent.keymanagement.service.ManagedDIDService.KeyManagementConfig
import io.iohk.atala.agent.keymanagement.storage.{DIDSecretStorage, InMemoryDIDSecretStorage}
import io.iohk.atala.castor.core.model.did.{
  DID,
  DIDDocument,
  DIDStorage,
  EllipticCurve,
  PrismDID,
  PublicKey,
  PublicKeyJwk,
  PublishedDIDOperation
}
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.prism.crypto.util.Random
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.shared.models.HexStrings.*
import zio.*

/** A wrapper around Castor's DIDService providing key-management capability. Analogous to the secretAPI in
  * indy-wallet-sdk.
  */
final class ManagedDIDService private[keymanagement] (secretStorage: DIDSecretStorage, config: KeyManagementConfig) {

  def createAndStoreDID(didTemplate: ManagedDIDTemplate): IO[CreateManagedDIDError, PrismDID] = {
    for {
      _ <- ZIO.logInfo("creating and storing managed DID")
      createOperation <- generateCreateOperation(didTemplate)
      _ <- ZIO.die(RuntimeException("DIE!!!"))
    } yield ???
  }

  private def generateCreateOperation(
      didTemplate: ManagedDIDTemplate
  ): IO[CreateManagedDIDError, PublishedDIDOperation.Create] = {
    for {
      keys <- ZIO
        .foreach(didTemplate.publicKeys.sortBy(_.id))(generateKeyPairAndPublicKey)
        .mapError(CreateManagedDIDError.KeyGenerationError.apply)
      updateCommitmentRevealValue = Random.INSTANCE.bytesOfLength(config.updateCommitmentRevealByte)
      recoveryCommitmentRevealValue = Random.INSTANCE.bytesOfLength(config.recoveryCommitmentRevealByte)
      updateCommitment = Sha256.compute(updateCommitmentRevealValue).getValue
      recoveryCommitment = Sha256.compute(recoveryCommitmentRevealValue).getValue
    } yield PublishedDIDOperation.Create(
      updateCommitment = HexString.fromByteArray(updateCommitment),
      recoveryCommitment = HexString.fromByteArray(recoveryCommitment),
      storage = DIDStorage.Cardano(didTemplate.storage),
      document = DIDDocument(
        publicKeys = keys.map(_._2),
        services = didTemplate.services
      )
    )
  }

  private def generateKeyPairAndPublicKey(template: DIDPublicKeyTemplate): Task[(ECKeyPair, PublicKey)] = {
    val curve = EllipticCurve.SECP256K1
    for {
      keyPair <- KeyGeneratorWrapper.generateECKeyPair(curve)
      publicKey = PublicKey.JsonWebKey2020(
        id = template.id,
        purposes = Seq(template.purpose),
        publicKeyJwk = PublicKeyJwk.ECPublicKeyData(
          crv = curve,
          x = Base64UrlString.fromByteArray(keyPair.publicKey.p.x.toPaddedByteArray(curve)),
          y = Base64UrlString.fromByteArray(keyPair.publicKey.p.y.toPaddedByteArray(curve))
        )
      )
    } yield (keyPair, publicKey)
  }

}

object ManagedDIDService {

  final case class KeyManagementConfig(updateCommitmentRevealByte: Int, recoveryCommitmentRevealByte: Int)

  object KeyManagementConfig {
    val default: KeyManagementConfig = KeyManagementConfig(
      updateCommitmentRevealByte = 32,
      recoveryCommitmentRevealByte = 32
    )
  }

  def inMemoryStorage(config: KeyManagementConfig): ULayer[ManagedDIDService] =
    InMemoryDIDSecretStorage.layer >>> ZLayer.fromFunction(ManagedDIDService(_, config))
}
