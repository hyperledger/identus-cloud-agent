package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.crypto.KeyGeneratorWrapper
import io.iohk.atala.agent.walletapi.model.{
  CommitmentPurpose,
  DIDPublicKeyTemplate,
  ECKeyPair,
  ManagedDIDCreateTemplate,
  ManagedDIDUpdatePatch,
  ManagedDIDUpdateTemplate
}
import io.iohk.atala.agent.walletapi.util.SeqExtensions.*
import io.iohk.atala.agent.walletapi.model.ECCoordinates.*
import io.iohk.atala.agent.walletapi.model.error.{CreateManagedDIDError, PublishManagedDIDError, UpdateManagedDIDError}
import io.iohk.atala.agent.walletapi.service.ManagedDIDService.{CreateDIDSecret, UpdateDIDSecret}
import io.iohk.atala.agent.walletapi.storage.{
  DIDNonSecretStorage,
  DIDSecretStorage,
  InMemoryDIDNonSecretStorage,
  InMemoryDIDSecretStorage
}
import io.iohk.atala.castor.core.model.did.DIDOperationHashes.DIDOperationHash
import io.iohk.atala.castor.core.model.did.{
  DID,
  DIDDocument,
  DIDStatePatch,
  DIDStorage,
  EllipticCurve,
  LongFormPrismDIDV1,
  PrismDID,
  PrismDIDV1,
  PublicKey,
  PublicKeyJwk,
  PublishedDIDOperation,
  PublishedDIDOperationOutcome,
  UpdateOperationDelta
}
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.castor.core.util.DIDOperationValidator
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.shared.models.HexStrings.*
import zio.*

/** A wrapper around Castor's DIDService providing key-management capability. Analogous to the secretAPI in
  * indy-wallet-sdk.
  */
final class ManagedDIDService private[walletapi] (
    didService: DIDService,
    didOpValidator: DIDOperationValidator,
    private[walletapi] val secretStorage: DIDSecretStorage,
    private[walletapi] val nonSecretStorage: DIDNonSecretStorage
) {

  private val CURVE = EllipticCurve.SECP256K1

  def publishStoredDID(did: PrismDID): IO[PublishManagedDIDError, PublishedDIDOperationOutcome] = {
    for {
      canonicalDID <- ZIO.fromEither(canonicalizeDID(did)).mapError(PublishManagedDIDError.UnsupportedDIDType.apply)
      createOperation <- nonSecretStorage
        .getCreatedDID(canonicalDID)
        .mapError(PublishManagedDIDError.WalletStorageError.apply)
        .map(_.toRight(PublishManagedDIDError.DIDNotFound(canonicalDID)))
        .absolve
      outcome <- didService
        .createPublishedDID(createOperation)
        .mapError(PublishManagedDIDError.OperationError.apply)
      _ <- nonSecretStorage.savePublishedDID(canonicalDID).mapError(PublishManagedDIDError.WalletStorageError.apply)
    } yield outcome
  }

  def createAndStoreDID(didTemplate: ManagedDIDCreateTemplate): IO[CreateManagedDIDError, LongFormPrismDIDV1] = {
    for {
      generated <- generateCreateOperation(didTemplate)
      (createOperation, secret) = generated
      longFormDID = LongFormPrismDIDV1.fromCreateOperation(createOperation)
      did = longFormDID.toCanonical
      operationHash = DIDOperationHash.fromOperation(createOperation)
      _ <- nonSecretStorage
        .getCreatedDID(did)
        .mapError(CreateManagedDIDError.WalletStorageError.apply)
        .filterOrFail(_.isEmpty)(CreateManagedDIDError.DIDAlreadyExists(did))
      _ <- ZIO
        .fromEither(didOpValidator.validate(createOperation))
        .mapError(CreateManagedDIDError.OperationError.apply)
      _ <- secretStorage
        .upsertDIDCommitmentKey(did, CommitmentPurpose.Update, secret.updateCommitmentSecret)
        .mapError(CreateManagedDIDError.WalletStorageError.apply)
      _ <- secretStorage
        .upsertDIDCommitmentKey(did, CommitmentPurpose.Recovery, secret.recoveryCommitmentSecret)
        .mapError(CreateManagedDIDError.WalletStorageError.apply)
      _ <- ZIO
        .foreachDiscard(secret.keyPairs) { case (keyId, keyPair) => secretStorage.upsertKey(did, keyId, keyPair) }
        .mapError(CreateManagedDIDError.WalletStorageError.apply)
      _ <- nonSecretStorage
        .upsertDIDVersion(did, operationHash.toHexString)
        .mapError(CreateManagedDIDError.WalletStorageError.apply)
      // A DID is considered created after a successful save using saveCreatedDID
      // If some steps above failed, it is not considered created and data that
      // are persisted along the way may be garbage collected.
      _ <- nonSecretStorage
        .saveCreatedDID(did, createOperation)
        .mapError(CreateManagedDIDError.WalletStorageError.apply)
    } yield longFormDID
  }

  // Currently only supports a simple update by replacing a current key-pair with a new one.
  // This implies that when there's a fork or rollback, users can potentially lose control of their DIDs.
  // This simple key-pair inplace update also introduce another problem when the update
  // is non-atomic as the keys can be lost if error occur during persistence.
  def updateDIDAndPublish(
      did: PrismDID,
      template: ManagedDIDUpdateTemplate
  ): IO[UpdateManagedDIDError, PublishedDIDOperationOutcome] = {
    for {
      canonicalDID <- ZIO.fromEither(canonicalizeDID(did)).mapError(UpdateManagedDIDError.UnsupportedDIDType.apply)
      _ <- nonSecretStorage.listPublishedDID
        .mapError(UpdateManagedDIDError.WalletStorageError.apply)
        .filterOrFail(_.contains(canonicalDID))(UpdateManagedDIDError.DIDNotPublished(canonicalDID))
      previousVersion <- nonSecretStorage
        .getDIDVersion(canonicalDID)
        .mapError(UpdateManagedDIDError.WalletStorageError.apply)
        .map(_.toRight(UpdateManagedDIDError.DIDNotFound(canonicalDID)))
        .absolve
      updateKeyPair <- secretStorage
        .getDIDCommitmentKey(canonicalDID, CommitmentPurpose.Update)
        .mapError(UpdateManagedDIDError.WalletStorageError.apply)
        .map(_.toRight(UpdateManagedDIDError.DIDNotFound(canonicalDID)))
        .absolve
      generated <- generateUpdateOperation(canonicalDID, template, updateKeyPair, previousVersion)
      (updateOperation, secret) = generated
      currentVersion = DIDOperationHash.fromOperation(updateOperation).toHexString
      _ <- ZIO.fromEither(didOpValidator.validate(updateOperation)).mapError(UpdateManagedDIDError.OperationError.apply)
      _ <- secretStorage
        .upsertDIDCommitmentKey(did, CommitmentPurpose.Update, secret.updateCommitmentSecret)
        .mapError(UpdateManagedDIDError.WalletStorageError.apply)
      _ <- ZIO
        .foreachDiscard(secret.keyPairs) { case (keyId, keyPair) => secretStorage.upsertKey(did, keyId, keyPair) }
        .mapError(UpdateManagedDIDError.WalletStorageError.apply)
      _ <- nonSecretStorage
        .upsertDIDVersion(canonicalDID, currentVersion)
        .mapError(UpdateManagedDIDError.WalletStorageError.apply)
      outcome <- didService
        .updatePublishedDID(updateOperation)
        .mapError(UpdateManagedDIDError.OperationError.apply)
    } yield outcome
  }

  private def generateCreateOperation(
      didTemplate: ManagedDIDCreateTemplate
  ): IO[CreateManagedDIDError, (PublishedDIDOperation.Create, CreateDIDSecret)] = {
    for {
      keys <- ZIO
        .foreach(didTemplate.publicKeys.sortBy(_.id))(generateKeyPairAndPublicKey)
        .mapError(CreateManagedDIDError.KeyGenerationError.apply)
      updateCommitmentSecret <- KeyGeneratorWrapper
        .generateECKeyPair(CURVE)
        .mapError(CreateManagedDIDError.KeyGenerationError.apply)
      recoveryCommitmentSecret <- KeyGeneratorWrapper
        .generateECKeyPair(CURVE)
        .mapError(CreateManagedDIDError.KeyGenerationError.apply)
      updateCommitmentRevealValue = updateCommitmentSecret.publicKey.toEncoded(CURVE)
      recoveryCommitmentRevealValue = recoveryCommitmentSecret.publicKey.toEncoded(CURVE)
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
        updateCommitmentSecret = updateCommitmentSecret,
        recoveryCommitmentSecret = recoveryCommitmentSecret,
        keyPairs = keys.map { case (keyPair, publicKey) => publicKey.id -> keyPair }.toMap
      )
    } yield operation -> secret
  }

  private def generateUpdateOperation(
      did: PrismDIDV1,
      updateTemplate: ManagedDIDUpdateTemplate,
      updateKeyPair: ECKeyPair,
      previousVersion: HexString
  ): IO[UpdateManagedDIDError, (PublishedDIDOperation.Update, UpdateDIDSecret)] = {
    def generateKeyAndConvertToDomain(
        patch: ManagedDIDUpdatePatch
    ): IO[UpdateManagedDIDError, (DIDStatePatch, Option[(ECKeyPair, PublicKey)])] =
      patch match {
        case p: ManagedDIDUpdatePatch.AddPublicKey =>
          generateKeyPairAndPublicKey(p.template)
            .mapError(UpdateManagedDIDError.KeyGenerationError.apply)
            .map { case (keyPair, publicKey) =>
              DIDStatePatch.AddPublicKey(publicKey) -> Some(keyPair -> publicKey)
            }
        case ManagedDIDUpdatePatch.RemovePublicKey(id) => ZIO.succeed(DIDStatePatch.RemovePublicKey(id) -> None)
        case ManagedDIDUpdatePatch.AddService(service) => ZIO.succeed(DIDStatePatch.AddService(service) -> None)
        case ManagedDIDUpdatePatch.RemoveService(id)   => ZIO.succeed(DIDStatePatch.RemoveService(id) -> None)
      }

    for {
      patchAndKeys <- ZIO.foreach(updateTemplate.patches)(generateKeyAndConvertToDomain)
      updateCommitmentSecret <- KeyGeneratorWrapper
        .generateECKeyPair(CURVE)
        .mapError(UpdateManagedDIDError.KeyGenerationError.apply)
      updateCommitmentRevealValue = updateCommitmentSecret.publicKey.toEncoded(CURVE)
      operation = PublishedDIDOperation.Update(
        did = did,
        updateKey = Base64UrlString.fromByteArray(updateKeyPair.publicKey.toEncoded(CURVE)),
        previousVersion = previousVersion,
        delta = UpdateOperationDelta(
          patches = patchAndKeys.map(_._1),
          updateCommitment = HexString.fromByteArray(Sha256.compute(updateCommitmentRevealValue).getValue)
        ),
        signature = ??? // TODO: generate signature
      )
      secret = UpdateDIDSecret(
        updateCommitmentSecret = updateCommitmentSecret,
        // "addPublicKey" action must overwrite the existing key.
        // Thus only the last key in the same update operation is kept in the secret storage
        // https://identity.foundation/sidetree/spec/#add-public-keys
        keyPairs = patchAndKeys
          .collect { case (_, Some((keyPair, publicKey))) => publicKey.id -> keyPair }
          .distinctBy(_._1, keepFirst = false)
          .toMap
      )
    } yield operation -> secret
  }

  private def generateKeyPairAndPublicKey(template: DIDPublicKeyTemplate): Task[(ECKeyPair, PublicKey)] = {
    for {
      keyPair <- KeyGeneratorWrapper.generateECKeyPair(CURVE)
      publicKey = PublicKey.JsonWebKey2020(
        id = template.id,
        purposes = Seq(template.purpose),
        publicKeyJwk = PublicKeyJwk.ECPublicKeyData(
          crv = CURVE,
          x = Base64UrlString.fromByteArray(keyPair.publicKey.p.x.toPaddedByteArray(CURVE)),
          y = Base64UrlString.fromByteArray(keyPair.publicKey.p.y.toPaddedByteArray(CURVE))
        )
      )
    } yield (keyPair, publicKey)
  }

  private def canonicalizeDID(did: PrismDID): Either[String, PrismDIDV1] = did match {
    case d: LongFormPrismDIDV1 => Right(d.toCanonical)
    case _                     => Left(s"only Prism method v1 is allow for agent managed keys")
  }

}

object ManagedDIDService {

  private final case class CreateDIDSecret(
      updateCommitmentSecret: ECKeyPair,
      recoveryCommitmentSecret: ECKeyPair,
      keyPairs: Map[String, ECKeyPair]
  )

  private final case class UpdateDIDSecret(updateCommitmentSecret: ECKeyPair, keyPairs: Map[String, ECKeyPair])

  def inMemoryStorage: URLayer[DIDService & DIDOperationValidator, ManagedDIDService] =
    (InMemoryDIDNonSecretStorage.layer ++ InMemoryDIDSecretStorage.layer) >>> ZLayer.fromFunction(
      ManagedDIDService(_, _, _, _)
    )
}
