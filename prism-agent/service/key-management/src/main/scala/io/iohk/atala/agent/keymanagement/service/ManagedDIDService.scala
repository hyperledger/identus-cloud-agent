package io.iohk.atala.agent.keymanagement.service

import io.iohk.atala.agent.keymanagement.crypto.KeyGeneratorWrapper
import io.iohk.atala.agent.keymanagement.model.{CommitmentPurpose, DIDPublicKeyTemplate, ECKeyPair, ManagedDIDTemplate}
import io.iohk.atala.agent.keymanagement.model.ECCoordinates.*
import io.iohk.atala.agent.keymanagement.model.error.{CreateManagedDIDError, PublishManagedDIDError}
import io.iohk.atala.agent.keymanagement.service.ManagedDIDService.{CreateDIDSecret, KeyManagementConfig}
import io.iohk.atala.agent.keymanagement.storage.{
  DIDNonSecretStorage,
  DIDSecretStorage,
  InMemoryDIDNonSecretStorage,
  InMemoryDIDSecretStorage
}
import io.iohk.atala.castor.core.model.did.{
  DID,
  DIDDocument,
  DIDStorage,
  EllipticCurve,
  LongFormPrismDIDV1,
  PrismDID,
  PrismDIDV1,
  PublicKey,
  PublicKeyJwk,
  PublishedDIDOperation,
  PublishedDIDOperationOutcome
}
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.castor.core.util.DIDOperationValidator
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.prism.crypto.util.Random
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.shared.models.HexStrings.*
import zio.*

/** A wrapper around Castor's DIDService providing key-management capability. Analogous to the secretAPI in
  * indy-wallet-sdk.
  */
final class ManagedDIDService private[keymanagement] (
    config: KeyManagementConfig,
    didService: DIDService,
    didOpValidator: DIDOperationValidator,
    private[keymanagement] val secretStorage: DIDSecretStorage,
    private[keymanagement] val nonSecretStorage: DIDNonSecretStorage
) {

  def publishStoredDID(did: PrismDID): IO[PublishManagedDIDError, PublishedDIDOperationOutcome] = {
    val canonicalDID = did match {
      case d: LongFormPrismDIDV1 => d.toCanonical
      case d                     => d
    }

    for {
      createOperation <- nonSecretStorage
        .getCreatedDID(canonicalDID)
        .mapError(PublishManagedDIDError.WalletStorageError.apply)
        .flatMap(op => ZIO.fromOption(op).mapError(_ => PublishManagedDIDError.DIDNotFound(canonicalDID)))
      outcome <- didService
        .createPublishedDID(createOperation)
        .mapError(PublishManagedDIDError.OperationError.apply)
    } yield outcome
  }

  def createAndStoreDID(didTemplate: ManagedDIDTemplate): IO[CreateManagedDIDError, LongFormPrismDIDV1] = {
    for {
      generated <- generateCreateOperation(didTemplate)
      (createOperation, secret) = generated
      longFormDID = LongFormPrismDIDV1.fromCreateOperation(createOperation)
      did = longFormDID.toCanonical
      _ <- nonSecretStorage
        .getCreatedDID(did)
        .mapError(CreateManagedDIDError.WalletStorageError.apply)
        .filterOrFail(_.isEmpty)(CreateManagedDIDError.DIDAlreadyExists(did))
      _ <- ZIO
        .fromEither(didOpValidator.validate(createOperation))
        .mapError(CreateManagedDIDError.OperationError.apply)
      _ <- secretStorage
        .upsertDIDCommitmentRevealValue(did, CommitmentPurpose.Update, secret.updateCommitmentRevealValue)
        .mapError(CreateManagedDIDError.WalletStorageError.apply)
      _ <- secretStorage
        .upsertDIDCommitmentRevealValue(did, CommitmentPurpose.Recovery, secret.recoveryCommitmentRevealValue)
        .mapError(CreateManagedDIDError.WalletStorageError.apply)
      _ <- ZIO
        .foreachDiscard(secret.keyPairs) { case (keyId, keyPair) => secretStorage.upsertKey(did, keyId, keyPair) }
        .mapError(CreateManagedDIDError.WalletStorageError.apply)
      // A DID is considered created after a successful save using saveCreatedDID
      // If some steps above failed, it is not considered created and data that
      // are persisted along the way may be garbage collected.
      _ <- nonSecretStorage
        .saveCreatedDID(did, createOperation)
        .mapError(CreateManagedDIDError.WalletStorageError.apply)
    } yield longFormDID
  }

  private def generateCreateOperation(
      didTemplate: ManagedDIDTemplate
  ): IO[CreateManagedDIDError, (PublishedDIDOperation.Create, CreateDIDSecret)] = {
    for {
      keys <- ZIO
        .foreach(didTemplate.publicKeys.sortBy(_.id))(generateKeyPairAndPublicKey)
        .mapError(CreateManagedDIDError.KeyGenerationError.apply)
      updateCommitmentRevealValue = Random.INSTANCE.bytesOfLength(config.updateCommitmentRevealByte)
      recoveryCommitmentRevealValue = Random.INSTANCE.bytesOfLength(config.recoveryCommitmentRevealByte)
      operation = PublishedDIDOperation.Create(
        updateCommitment = HexString.fromByteArray(Sha256.compute(updateCommitmentRevealValue).getValue),
        recoveryCommitment = HexString.fromByteArray(Sha256.compute(recoveryCommitmentRevealValue).getValue),
        storage = DIDStorage.Cardano(didTemplate.storage),
        document = DIDDocument(
          publicKeys = keys.map(_._2),
          services = didTemplate.services
        )
      )
      secret = CreateDIDSecret(
        updateCommitmentRevealValue = HexString.fromByteArray(updateCommitmentRevealValue),
        recoveryCommitmentRevealValue = HexString.fromByteArray(recoveryCommitmentRevealValue),
        keyPairs = keys.map { case (keyPair, template) => template.id -> keyPair }.toMap
      )
    } yield operation -> secret
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

  private final case class CreateDIDSecret(
      updateCommitmentRevealValue: HexString,
      recoveryCommitmentRevealValue: HexString,
      keyPairs: Map[String, ECKeyPair]
  )

  final case class KeyManagementConfig(updateCommitmentRevealByte: Int, recoveryCommitmentRevealByte: Int)

  object KeyManagementConfig {
    val default: KeyManagementConfig = KeyManagementConfig(
      updateCommitmentRevealByte = 32,
      recoveryCommitmentRevealByte = 32
    )
  }

  def inMemoryStorage(
      config: KeyManagementConfig = KeyManagementConfig.default
  ): URLayer[DIDService & DIDOperationValidator, ManagedDIDService] =
    (InMemoryDIDNonSecretStorage.layer ++ InMemoryDIDSecretStorage.layer) >>> ZLayer.fromFunction(
      ManagedDIDService(config, _, _, _, _)
    )
}
