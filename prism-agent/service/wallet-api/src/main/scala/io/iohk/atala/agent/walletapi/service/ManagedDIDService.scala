package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.crypto.KeyGeneratorWrapper
import io.iohk.atala.agent.walletapi.model.{DIDPublicKeyTemplate, ECKeyPair, ManagedDIDTemplate}
import io.iohk.atala.agent.walletapi.model.ECCoordinates.*
import io.iohk.atala.agent.walletapi.model.error.{CreateManagedDIDError, PublishManagedDIDError}
import io.iohk.atala.agent.walletapi.service.ManagedDIDService.CreateDIDSecret
import io.iohk.atala.agent.walletapi.storage.{
  DIDNonSecretStorage,
  DIDSecretStorage,
  InMemoryDIDNonSecretStorage,
  InMemoryDIDSecretStorage
}
import io.iohk.atala.castor.core.model.did.{
  DID,
  EllipticCurve,
  InternalPublicKey,
  InternalKeyPurpose,
  LongFormPrismDID,
  PrismDID,
  PrismDIDOperation,
  PublicKey,
  PublicKeyData,
  ScheduleDIDOperationOutcome,
  SignedPrismDIDOperation
}
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.prism.crypto.util.Random
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.shared.models.HexStrings.*
import zio.*

/** A wrapper around Castor's DIDService providing key-management capability. Analogous to the secretAPI in
  * indy-wallet-sdk.
  */
final class ManagedDIDService private[walletapi] (
    didService: DIDService,
    private[walletapi] val secretStorage: DIDSecretStorage,
    private[walletapi] val nonSecretStorage: DIDNonSecretStorage
) {

  private val DEFAULT_MASTER_KEY_ID = "master0"
  private val CURVE = EllipticCurve.SECP256K1

  def publishStoredDID(did: PrismDID): IO[PublishManagedDIDError, ScheduleDIDOperationOutcome] = {
    val canonicalDID = did.asCanonical
    for {
      createOperation <- nonSecretStorage
        .getCreatedDID(canonicalDID)
        .mapError(PublishManagedDIDError.WalletStorageError.apply)
        .flatMap(op => ZIO.fromOption(op).mapError(_ => PublishManagedDIDError.DIDNotFound(canonicalDID)))
      outcome <- didService
        .createPublishedDID(???) // TODO: sign CreateDID operation
        .mapError(PublishManagedDIDError.OperationError.apply)
    } yield outcome
  }

  def createAndStoreDID(didTemplate: ManagedDIDTemplate): IO[CreateManagedDIDError, LongFormPrismDID] = {
    for {
      generated <- generateCreateOperation(didTemplate)
      (createOperation, secret) = generated
      longFormDID = PrismDID.buildLongFormFromOperation(createOperation)
      did = longFormDID.asCanonical
      _ <- nonSecretStorage
        .getCreatedDID(did)
        .mapError(CreateManagedDIDError.WalletStorageError.apply)
        .filterOrFail(_.isEmpty)(CreateManagedDIDError.DIDAlreadyExists(did))
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
  ): IO[CreateManagedDIDError, (PrismDIDOperation.Create, CreateDIDSecret)] = {
    for {
      keys <- ZIO
        .foreach(didTemplate.publicKeys.sortBy(_.id))(generateKeyPairAndPublicKey)
        .mapError(CreateManagedDIDError.KeyGenerationError.apply)
      masterKey <- generateKeyPairAndInternalPublicKey(DEFAULT_MASTER_KEY_ID, InternalKeyPurpose.Master).mapError(
        CreateManagedDIDError.KeyGenerationError.apply
      )
      operation = PrismDIDOperation.Create(
        publicKeys = keys.map(_._2),
        internalKeys = Seq(masterKey._2)
      )
      secret = CreateDIDSecret(
        keyPairs = keys.map { case (keyPair, template) => template.id -> keyPair }.toMap,
        internalKeyPairs = Map(masterKey._2.id -> masterKey._1)
      )
    } yield operation -> secret
  }

  private def generateKeyPairAndPublicKey(template: DIDPublicKeyTemplate): Task[(ECKeyPair, PublicKey)] = {
    for {
      keyPair <- KeyGeneratorWrapper.generateECKeyPair(CURVE)
      publicKey = PublicKey(template.id, template.purpose, publicKeyData = toPublicKeyData(keyPair))
    } yield (keyPair, publicKey)
  }

  private def generateKeyPairAndInternalPublicKey(
      id: String,
      purpose: InternalKeyPurpose
  ): Task[(ECKeyPair, InternalPublicKey)] = {
    for {
      keyPair <- KeyGeneratorWrapper.generateECKeyPair(CURVE)
      internalPublicKey = InternalPublicKey(id, purpose, toPublicKeyData(keyPair))
    } yield (keyPair, internalPublicKey)
  }

  private def toPublicKeyData(keyPair: ECKeyPair): PublicKeyData = PublicKeyData.ECKeyData(
    crv = CURVE,
    x = Base64UrlString.fromByteArray(keyPair.publicKey.p.x.toPaddedByteArray(CURVE)),
    y = Base64UrlString.fromByteArray(keyPair.publicKey.p.y.toPaddedByteArray(CURVE))
  )

}

object ManagedDIDService {

  private final case class CreateDIDSecret(
      keyPairs: Map[String, ECKeyPair],
      internalKeyPairs: Map[String, ECKeyPair]
  )

  def inMemoryStorage: URLayer[DIDService, ManagedDIDService] =
    (InMemoryDIDNonSecretStorage.layer ++ InMemoryDIDSecretStorage.layer) >>> ZLayer.fromFunction(
      ManagedDIDService(_, _, _)
    )
}
