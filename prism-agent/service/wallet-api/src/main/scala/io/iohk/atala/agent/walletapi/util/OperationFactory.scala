package io.iohk.atala.agent.walletapi.util

import io.iohk.atala.agent.walletapi.crypto.{ECKeyPair, ECPublicKey}
import io.iohk.atala.agent.walletapi.model.{DIDPublicKeyTemplate, ManagedDIDTemplate, UpdateManagedDIDAction}
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

private[walletapi] final case class CreateDIDSecret(
    keyPairs: Map[String, ECKeyPair],
    internalKeyPairs: Map[String, ECKeyPair]
)

private[walletapi] final case class UpdateDIDSecret(newKeyPairs: Map[String, ECKeyPair])

object OperationFactory {

  def makeCreateOperation(
      masterKeyId: String,
      keyGenerator: () => Task[ECKeyPair]
  )(
      didTemplate: ManagedDIDTemplate
  ): IO[CreateManagedDIDError, (PrismDIDOperation.Create, CreateDIDSecret)] = {
    for {
      keys <- ZIO
        .foreach(didTemplate.publicKeys)(generateKeyPairAndPublicKey(keyGenerator))
        .mapError(CreateManagedDIDError.KeyGenerationError.apply)
      masterKey <- generateKeyPairAndInternalPublicKey(keyGenerator)(masterKeyId, InternalKeyPurpose.Master)
        .mapError(
          CreateManagedDIDError.KeyGenerationError.apply
        )
      operation = PrismDIDOperation.Create(
        publicKeys = keys.map(_._2) ++ Seq(masterKey._2),
        services = didTemplate.services,
        context = Seq() // TODO: expose context in the API
      )
      secret = CreateDIDSecret(
        keyPairs = keys.map { case (keyPair, publicKey) => publicKey.id -> keyPair }.toMap,
        internalKeyPairs = Map(masterKey._2.id -> masterKey._1)
      )
    } yield operation -> secret
  }

  def makeUpdateOperation(
      keyGenerator: () => Task[ECKeyPair]
  )(
      did: CanonicalPrismDID,
      previousOperationHash: Array[Byte],
      actions: Seq[UpdateManagedDIDAction]
  ): IO[UpdateManagedDIDError, (PrismDIDOperation.Update, UpdateDIDSecret)] = {
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
      secret = UpdateDIDSecret(
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

  private def generateKeyPairAndPublicKey(keyGenerator: () => Task[ECKeyPair])(
      template: DIDPublicKeyTemplate
  ): Task[(ECKeyPair, PublicKey)] = {
    for {
      keyPair <- keyGenerator()
      publicKey = PublicKey(template.id, template.purpose, toPublicKeyData(keyPair.publicKey))
    } yield (keyPair, publicKey)
  }

  private def generateKeyPairAndInternalPublicKey(keyGenerator: () => Task[ECKeyPair])(
      id: String,
      purpose: InternalKeyPurpose
  ): Task[(ECKeyPair, InternalPublicKey)] = {
    for {
      keyPair <- keyGenerator()
      internalPublicKey = InternalPublicKey(id, purpose, toPublicKeyData(keyPair.publicKey))
    } yield (keyPair, internalPublicKey)
  }

  private def toPublicKeyData(publicKey: ECPublicKey): PublicKeyData = PublicKeyData.ECCompressedKeyData(
    crv = publicKey.curve,
    data = Base64UrlString.fromByteArray(publicKey.encode),
  )

}
