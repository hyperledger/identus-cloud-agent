package io.iohk.atala.agent.walletapi.util

import io.iohk.atala.agent.walletapi.crypto.{ECKeyPair, ECPublicKey}
import io.iohk.atala.agent.walletapi.model.{
  DIDPublicKeyTemplate,
  ManagedDIDTemplate,
  UpdateManagedDIDAction,
  CreateDIDHdKey,
  CreateDIDRandKey,
  UpdateDIDHdKey,
  UpdateDIDRandKey
}
import io.iohk.atala.agent.walletapi.model.error.{CreateManagedDIDError, UpdateManagedDIDError}
import io.iohk.atala.castor.core.model.did.{
  CanonicalPrismDID,
  InternalKeyPurpose,
  InternalPublicKey,
  PrismDIDOperation,
  PublicKey,
  PublicKeyData,
  UpdateDIDAction
}
import io.iohk.atala.shared.models.Base64UrlString
import zio.*

import scala.collection.immutable.ArraySeq
import io.iohk.atala.agent.walletapi.model.HdKeyIndexCounter
import io.iohk.atala.agent.walletapi.crypto.Apollo
import io.iohk.atala.castor.core.model.did.EllipticCurve
import io.iohk.atala.agent.walletapi.model.ManagedDIDHdKeyPath

private[util] final case class KeyDerivationOutcome[PK](
    publicKey: PK,
    path: ManagedDIDHdKeyPath,
    nextCounter: HdKeyIndexCounter
)

class OperationFactory(apollo: Apollo) {

  /** Generates a key pair and a public key from a DID template
    *
    * @param masterKeyId
    *   The key id of the master key
    * @param seed
    *   The seed to use for the key generation
    * @param didTemplate
    *   The DID template
    * @param didIndex
    *   The index of the DID to be used for the key derivation
    */
  def makeCreateOperationHdKey(
      masterKeyId: String,
      seed: Array[Byte]
  )(
      didIndex: Int,
      didTemplate: ManagedDIDTemplate
  ): IO[CreateManagedDIDError, (PrismDIDOperation.Create, CreateDIDHdKey)] = {
    val initKeysWithCounter = (Vector.empty[(PublicKey, ManagedDIDHdKeyPath)], HdKeyIndexCounter.zero(didIndex))
    val result = for {
      keysWithCounter <- ZIO.foldLeft(didTemplate.publicKeys)(initKeysWithCounter) {
        case ((keys, keyCounter), template) =>
          derivePublicKey(seed)(template, keyCounter)
            .map { outcome =>
              val newKeys = keys :+ (outcome.publicKey, outcome.path)
              (newKeys, outcome.nextCounter)
            }
      }
      masterKeyOutcome <- deriveInternalPublicKey(seed)(
        masterKeyId,
        InternalKeyPurpose.Master,
        keysWithCounter._2
      )
      operation = PrismDIDOperation.Create(
        publicKeys = keysWithCounter._1.map(_._1) ++ Seq(masterKeyOutcome.publicKey),
        services = didTemplate.services,
        context = Seq() // TODO: expose context in the api
      )
      hdKeys = CreateDIDHdKey(
        keyPaths = keysWithCounter._1.map { case (publicKey, path) => publicKey.id -> path }.toMap,
        internalKeyPaths = Map(masterKeyOutcome.publicKey.id -> masterKeyOutcome.path),
        counter = masterKeyOutcome.nextCounter
      )
    } yield operation -> hdKeys

    result.mapError(CreateManagedDIDError.KeyGenerationError.apply)
  }

  def makeCreateOperationRandKey(
      masterKeyId: String
  )(didTemplate: ManagedDIDTemplate): IO[CreateManagedDIDError, (PrismDIDOperation.Create, CreateDIDRandKey)] = {
    for {
      randomSeed <- apollo.ecKeyFactory.randomBip32Seed().mapError(CreateManagedDIDError.KeyGenerationError.apply)
      operationWithHdKey <- makeCreateOperationHdKey(masterKeyId, randomSeed)(0, didTemplate)
      (operation, hdKeys) = operationWithHdKey
      keyPairs <- ZIO.foreach(hdKeys.keyPaths) { case (id, path) =>
        deriveSecp256k1KeyPair(randomSeed, path).mapBoth(CreateManagedDIDError.KeyGenerationError.apply, id -> _)
      }
      internalKeyPairs <- ZIO.foreach(hdKeys.internalKeyPaths) { case (id, path) =>
        deriveSecp256k1KeyPair(randomSeed, path).mapBoth(CreateManagedDIDError.KeyGenerationError.apply, id -> _)
      }
    } yield operation -> CreateDIDRandKey(
      keyPairs = keyPairs,
      internalKeyPairs = internalKeyPairs
    )
  }

  def makeUpdateOperationHdKey(seed: Array[Byte])(
      did: CanonicalPrismDID,
      previousOperationHash: Array[Byte],
      actions: Seq[UpdateManagedDIDAction],
      fromKeyCounter: HdKeyIndexCounter
  ): IO[UpdateManagedDIDError, (PrismDIDOperation.Update, UpdateDIDHdKey)] = {
    val initKeysWithCounter =
      (Vector.empty[(UpdateManagedDIDAction, Option[(PublicKey, ManagedDIDHdKeyPath)])], fromKeyCounter)
    val actionsWithKeyMaterial = ZIO.foldLeft(actions)(initKeysWithCounter) { case ((acc, keyCounter), action) =>
      val derivation = action match {
        case UpdateManagedDIDAction.AddKey(template) =>
          derivePublicKey(seed)(template, keyCounter).mapError(UpdateManagedDIDError.KeyGenerationError.apply).asSome
        case _ => ZIO.none
      }
      derivation.map {
        case Some(outcome) => (acc :+ (action -> Some((outcome.publicKey, outcome.path))), outcome.nextCounter)
        case None          => (acc :+ (action -> None), keyCounter)
      }
    }

    for {
      actionsWithKeyMaterial <- actionsWithKeyMaterial
      (actionWithHdKey, keyCounter) = actionsWithKeyMaterial
      transformedActions <- ZIO.foreach(actionWithHdKey) { case (action, keyMaterial) =>
        transformUpdateAction(action, keyMaterial.map(_._1))
      }
      keys = actionWithHdKey.collect { case (UpdateManagedDIDAction.AddKey(_), Some(secret)) => secret }
      operation = PrismDIDOperation.Update(
        did = did,
        previousOperationHash = ArraySeq.from(previousOperationHash),
        actions = transformedActions
      )
      hdKeys = UpdateDIDHdKey(
        // NOTE: Prism DID specification currently doesn't allow updating existing key with the same key-id.
        // Duplicated key-id in AddKey action can be ignored as the specification will reject the whole update operation.
        // If the specification supports updating existing key, the key that will be stored in the wallet
        // MUST be aligned with the spec (e.g. keep first / keep last in the action list)
        newKeyPaths = keys.map { case (publicKey, path) => publicKey.id -> path }.toMap,
        counter = keyCounter
      )
    } yield operation -> hdKeys
  }

  def makeUpdateOperationRandKey(
      did: CanonicalPrismDID,
      previousOperationHash: Array[Byte],
      actions: Seq[UpdateManagedDIDAction]
  ): IO[UpdateManagedDIDError, (PrismDIDOperation.Update, UpdateDIDRandKey)] = {
    for {
      randomSeed <- apollo.ecKeyFactory.randomBip32Seed().mapError(UpdateManagedDIDError.KeyGenerationError.apply)
      operationWithHdKey <- makeUpdateOperationHdKey(randomSeed)(
        did,
        previousOperationHash,
        actions,
        HdKeyIndexCounter.zero(0)
      )
      (operation, hdKeys) = operationWithHdKey
      keyPairs <- ZIO.foreach(hdKeys.newKeyPaths) { case (id, path) =>
        deriveSecp256k1KeyPair(randomSeed, path).mapBoth(UpdateManagedDIDError.KeyGenerationError.apply, id -> _)
      }
    } yield operation -> UpdateDIDRandKey(newKeyPairs = keyPairs)
  }

  private def transformUpdateAction(
      updateAction: UpdateManagedDIDAction,
      publicKey: Option[PublicKey]
  ): UIO[UpdateDIDAction] = {
    updateAction match {
      case UpdateManagedDIDAction.AddKey(_) =>
        publicKey match {
          case Some(publicKey) => ZIO.succeed(UpdateDIDAction.AddKey(publicKey))
          case None            =>
            // should be impossible otherwise it's a defect
            ZIO.dieMessage("addKey update DID action must have a generated a key-pair")
        }
      case UpdateManagedDIDAction.RemoveKey(id)       => ZIO.succeed(UpdateDIDAction.RemoveKey(id))
      case UpdateManagedDIDAction.AddService(service) => ZIO.succeed(UpdateDIDAction.AddService(service))
      case UpdateManagedDIDAction.RemoveService(id)   => ZIO.succeed(UpdateDIDAction.RemoveService(id))
      case UpdateManagedDIDAction.UpdateService(patch) =>
        ZIO.succeed(UpdateDIDAction.UpdateService(patch.id, patch.serviceType, patch.serviceEndpoints))
    }
  }

  private def derivePublicKey(seed: Array[Byte])(
      template: DIDPublicKeyTemplate,
      keyCounter: HdKeyIndexCounter
  ): Task[KeyDerivationOutcome[PublicKey]] = {
    val purpose = template.purpose
    val keyPath = keyCounter.path(purpose)
    for {
      keyPair <- deriveSecp256k1KeyPair(seed, keyPath)
      publicKey = PublicKey(template.id, purpose, toPublicKeyData(keyPair.publicKey))
    } yield KeyDerivationOutcome(publicKey, keyPath, keyCounter.next(purpose))
  }

  private def deriveInternalPublicKey(seed: Array[Byte])(
      id: String,
      purpose: InternalKeyPurpose,
      keyCounter: HdKeyIndexCounter
  ): Task[KeyDerivationOutcome[InternalPublicKey]] = {
    val keyPath = keyCounter.path(purpose)
    for {
      keyPair <- deriveSecp256k1KeyPair(seed, keyPath)
      internalPublicKey = InternalPublicKey(id, purpose, toPublicKeyData(keyPair.publicKey))
    } yield KeyDerivationOutcome(internalPublicKey, keyPath, keyCounter.next(purpose))
  }

  private def deriveSecp256k1KeyPair(seed: Array[Byte], path: ManagedDIDHdKeyPath): Task[ECKeyPair] = {
    apollo.ecKeyFactory.deriveKeyPair(EllipticCurve.SECP256K1, seed)(path.derivationPath: _*)
  }

  private def toPublicKeyData(publicKey: ECPublicKey): PublicKeyData = PublicKeyData.ECCompressedKeyData(
    crv = publicKey.curve,
    data = Base64UrlString.fromByteArray(publicKey.encode),
  )

}
