package io.iohk.atala.agent.walletapi.util

import io.iohk.atala.agent.walletapi.crypto.{ECKeyPair, ECPublicKey}
import io.iohk.atala.agent.walletapi.model.{
  DIDPublicKeyTemplate,
  ManagedDIDTemplate,
  UpdateManagedDIDAction,
  CreateDidHdKey,
  CreateDIDRandKey,
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
import io.iohk.atala.agent.walletapi.model.ManagedDidHdKeyCounter
import io.iohk.atala.agent.walletapi.crypto.Apollo
import io.iohk.atala.castor.core.model.did.EllipticCurve
import io.iohk.atala.agent.walletapi.model.ManagedDidHdKeyPath

private[util] final case class KeyDerivationOutcome[PK](
    publicKey: PK,
    path: ManagedDidHdKeyPath,
    nextCounter: ManagedDidHdKeyCounter
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
  ): IO[CreateManagedDIDError, (PrismDIDOperation.Create, CreateDidHdKey)] = {
    val initKeysWithCounter = (Vector.empty[(PublicKey, ManagedDidHdKeyPath)], ManagedDidHdKeyCounter.zero(didIndex))
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
      hdKeys = CreateDidHdKey(
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
        apollo.ecKeyFactory
          .deriveKeyPair(EllipticCurve.SECP256K1, randomSeed)(path.derivationPath: _*)
          .mapBoth(CreateManagedDIDError.KeyGenerationError.apply, id -> _)
      }
      internalKeyPairs <- ZIO.foreach(hdKeys.internalKeyPaths) { case (id, path) =>
        apollo.ecKeyFactory
          .deriveKeyPair(EllipticCurve.SECP256K1, randomSeed)(path.derivationPath: _*)
          .mapBoth(CreateManagedDIDError.KeyGenerationError.apply, id -> _)
      }
    } yield operation -> CreateDIDRandKey(
      keyPairs = keyPairs,
      internalKeyPairs = internalKeyPairs
    )
  }

  def makeUpdateOperationRandKey(
      keyGenerator: () => Task[ECKeyPair]
  )(
      did: CanonicalPrismDID,
      previousOperationHash: Array[Byte],
      actions: Seq[UpdateManagedDIDAction]
  ): IO[UpdateManagedDIDError, (PrismDIDOperation.Update, UpdateDIDRandKey)] = {
    val actionsWithSecret = actions.map {
      case a @ UpdateManagedDIDAction.AddKey(template) =>
        a -> generateKeyPairAndPublicKey(keyGenerator)(template)
          .mapError(UpdateManagedDIDError.KeyGenerationError.apply)
          .asSome
      case a => a -> ZIO.none
    }

    for {
      actionsWithSecret <- ZIO
        .foreach(actionsWithSecret) { case (action, secret) => secret.map(action -> _) }
      transformedActions <- ZIO.foreach(actionsWithSecret)(transformUpdateAction)
      keys = actionsWithSecret.collect { case (UpdateManagedDIDAction.AddKey(_), Some(secret)) => secret }
      operation = PrismDIDOperation.Update(
        did = did,
        previousOperationHash = ArraySeq.from(previousOperationHash),
        actions = transformedActions
      )
      secret = UpdateDIDRandKey(
        // NOTE: Prism DID specification currently doesn't allow updating existing key with the same key-id.
        // Duplicated key-id in AddKey action can be ignored as the specification will reject the whole update operation.
        // If the specification supports updating existing key, the key that will be stored in the wallet
        // MUST be aligned with the spec (e.g. keep first / keep last in the action list)
        newKeyPairs = keys.map { case (keyPair, publicKey) => publicKey.id -> keyPair }.toMap
      )
    } yield operation -> secret
  }

  private def transformUpdateAction(
      updateAction: UpdateManagedDIDAction,
      secret: Option[(ECKeyPair, PublicKey)]
  ): UIO[UpdateDIDAction] = {
    updateAction match {
      case UpdateManagedDIDAction.AddKey(_) =>
        secret match {
          case Some((_, publicKey)) => ZIO.succeed(UpdateDIDAction.AddKey(publicKey))
          case None                 =>
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

  // TODO: remove
  private def generateKeyPairAndPublicKey(keyGenerator: () => Task[ECKeyPair])(
      template: DIDPublicKeyTemplate
  ): Task[(ECKeyPair, PublicKey)] = {
    for {
      keyPair <- keyGenerator()
      publicKey = PublicKey(template.id, template.purpose, toPublicKeyData(keyPair.publicKey))
    } yield (keyPair, publicKey)
  }

  // TODO: remove
  private def generateKeyPairAndInternalPublicKey(keyGenerator: () => Task[ECKeyPair])(
      id: String,
      purpose: InternalKeyPurpose
  ): Task[(ECKeyPair, InternalPublicKey)] = {
    for {
      keyPair <- keyGenerator()
      internalPublicKey = InternalPublicKey(id, purpose, toPublicKeyData(keyPair.publicKey))
    } yield (keyPair, internalPublicKey)
  }

  private def derivePublicKey(seed: Array[Byte])(
      template: DIDPublicKeyTemplate,
      keyCounter: ManagedDidHdKeyCounter
  ): Task[KeyDerivationOutcome[PublicKey]] = {
    val purpose = template.purpose
    val keyPath = keyCounter.path(purpose)
    for {
      keyPair <- apollo.ecKeyFactory.deriveKeyPair(EllipticCurve.SECP256K1, seed)(keyPath.derivationPath: _*)
      publicKey = PublicKey(template.id, purpose, toPublicKeyData(keyPair.publicKey))
    } yield KeyDerivationOutcome(publicKey, keyPath, keyCounter.next(purpose))
  }

  private def deriveInternalPublicKey(seed: Array[Byte])(
      id: String,
      purpose: InternalKeyPurpose,
      keyCounter: ManagedDidHdKeyCounter
  ): Task[KeyDerivationOutcome[InternalPublicKey]] = {
    val keyPath = keyCounter.path(purpose)
    for {
      keyPair <- apollo.ecKeyFactory.deriveKeyPair(EllipticCurve.SECP256K1, seed)(keyPath.derivationPath: _*)
      internalPublicKey = InternalPublicKey(id, purpose, toPublicKeyData(keyPair.publicKey))
    } yield KeyDerivationOutcome(internalPublicKey, keyPath, keyCounter.next(purpose))
  }

  private def toPublicKeyData(publicKey: ECPublicKey): PublicKeyData = PublicKeyData.ECCompressedKeyData(
    crv = publicKey.curve,
    data = Base64UrlString.fromByteArray(publicKey.encode),
  )

}
