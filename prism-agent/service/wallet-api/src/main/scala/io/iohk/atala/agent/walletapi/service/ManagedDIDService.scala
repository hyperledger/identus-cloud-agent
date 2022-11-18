package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.crypto.{ECWrapper, KeyGeneratorWrapper}
import io.iohk.atala.agent.walletapi.model.{DIDPublicKeyTemplate, ECKeyPair, ManagedDIDState, ManagedDIDTemplate}
import io.iohk.atala.agent.walletapi.model.ECCoordinates.*
import io.iohk.atala.agent.walletapi.model.error.{CreateManagedDIDError, PublishManagedDIDError}
import io.iohk.atala.agent.walletapi.service.ManagedDIDService.{CreateDIDSecret, DEFAULT_MASTER_KEY_ID}
import io.iohk.atala.agent.walletapi.storage.{
  DIDNonSecretStorage,
  DIDSecretStorage,
  InMemoryDIDNonSecretStorage,
  InMemoryDIDSecretStorage
}
import io.iohk.atala.agent.walletapi.util.ManagedDIDTemplateValidator
import io.iohk.atala.castor.core.model.did.{
  CanonicalPrismDID,
  DID,
  EllipticCurve,
  InternalKeyPurpose,
  InternalPublicKey,
  LongFormPrismDID,
  PrismDID,
  PrismDIDOperation,
  PublicKey,
  PublicKeyData,
  ScheduleDIDOperationOutcome,
  SignedPrismDIDOperation
}
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.castor.core.util.DIDOperationValidator
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.prism.crypto.util.Random
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.shared.models.HexStrings.*
import zio.*

import scala.collection.immutable.ArraySeq

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

  def publishStoredDID(did: CanonicalPrismDID): IO[PublishManagedDIDError, ScheduleDIDOperationOutcome] = {
    for {
      operation <- nonSecretStorage
        .getManagedDIDState(did)
        .mapError(PublishManagedDIDError.WalletStorageError.apply)
        .flatMap(op => ZIO.fromOption(op).mapError(_ => PublishManagedDIDError.DIDNotFound(did)))
        .map {
          case ManagedDIDState.Created(operation) => Right(operation)
          case ManagedDIDState.PublicationAttempted(_, operationId) =>
            Left(PublishManagedDIDError.AwaitingPublication(operationId))
          case ManagedDIDState.Published(operationId) => Left(PublishManagedDIDError.AlreadyPublished(operationId))
        }
        .absolve
      masterKeyPair <-
        secretStorage
          .getKey(did, DEFAULT_MASTER_KEY_ID)
          .mapError(PublishManagedDIDError.WalletStorageError.apply)
          .flatMap(maybeKey =>
            ZIO
              .fromOption(maybeKey)
              .orDieWith(_ =>
                new Exception("master-key must exists in the wallet for create DID publication signature")
              )
          )
      signedAtalaOperation <- ZIO
        .fromTry(
          ECWrapper.signBytes(CURVE, operation.toAtalaOperation.toByteArray, masterKeyPair.privateKey)
        )
        .mapError(PublishManagedDIDError.CryptographicError.apply)
        .map(signature =>
          SignedPrismDIDOperation.Create(
            operation = operation,
            signature = ArraySeq.from(signature),
            signedWithKey = DEFAULT_MASTER_KEY_ID
          )
        )
      outcome <- didService
        .createPublishedDID(signedAtalaOperation)
        .mapError(PublishManagedDIDError.OperationError.apply)
      _ <- nonSecretStorage
        .setManagedDIDState(did, ManagedDIDState.PublicationAttempted(operation, outcome.operationId))
        .mapError(PublishManagedDIDError.WalletStorageError.apply)
    } yield outcome
  }

  def createAndStoreDID(didTemplate: ManagedDIDTemplate): IO[CreateManagedDIDError, LongFormPrismDID] = {
    for {
      _ <- ZIO
        .fromEither(ManagedDIDTemplateValidator.validate(didTemplate))
        .mapError(CreateManagedDIDError.InvalidArgument.apply)
      generated <- generateCreateOperation(didTemplate)
      (createOperation, secret) = generated
      longFormDID = PrismDID.buildLongFormFromOperation(createOperation)
      did = longFormDID.asCanonical
      _ <- ZIO.fromEither(didOpValidator.validate(createOperation)).mapError(CreateManagedDIDError.OperationError.apply)
      _ <- nonSecretStorage
        .getManagedDIDState(did)
        .mapError(CreateManagedDIDError.WalletStorageError.apply)
        .filterOrFail(_.isEmpty)(CreateManagedDIDError.DIDAlreadyExists(did))
      _ <- ZIO
        .foreachDiscard(secret.keyPairs ++ secret.internalKeyPairs) { case (keyId, keyPair) =>
          secretStorage.upsertKey(did, keyId, keyPair)
        }
        .mapError(CreateManagedDIDError.WalletStorageError.apply)
      // A DID is considered created after a successful save using saveCreatedDID
      // If some steps above failed, it is not considered created and data that
      // are persisted along the way may be garbage collected.
      _ <- nonSecretStorage
        .setManagedDIDState(did, ManagedDIDState.Created(createOperation))
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

  val DEFAULT_MASTER_KEY_ID: String = "master0"

  val reservedKeyIds: Set[String] = Set(DEFAULT_MASTER_KEY_ID)

  private final case class CreateDIDSecret(
      keyPairs: Map[String, ECKeyPair],
      internalKeyPairs: Map[String, ECKeyPair]
  )

  def inMemoryStorage: URLayer[DIDOperationValidator & DIDService, ManagedDIDService] =
    (InMemoryDIDNonSecretStorage.layer ++ InMemoryDIDSecretStorage.layer) >>> ZLayer.fromFunction(
      ManagedDIDService(_, _, _, _)
    )
}
