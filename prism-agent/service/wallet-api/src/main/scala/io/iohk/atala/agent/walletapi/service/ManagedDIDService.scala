package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.crypto.KeyGeneratorWrapper
import io.iohk.atala.agent.walletapi.model.{
  CommitmentPurpose,
  DIDPublicKeyTemplate,
  ECKeyPair,
  ManagedDIDCreateTemplate,
  ManagedDIDUpdateTemplate
}
import io.iohk.atala.agent.walletapi.model.ECCoordinates.*
import io.iohk.atala.agent.walletapi.model.error.{CreateManagedDIDError, PublishManagedDIDError, UpdateManagedDIDError}
import io.iohk.atala.agent.walletapi.service.ManagedDIDService.{CreateDIDSecret, UpdateDIDSecret}
import io.iohk.atala.agent.walletapi.storage.{
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

  def createAndStoreDID(didTemplate: ManagedDIDCreateTemplate): IO[CreateManagedDIDError, LongFormPrismDIDV1] = {
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
        .upsertDIDCommitmentKey(did, CommitmentPurpose.Update, secret.updateCommitmentSecret)
        .mapError(CreateManagedDIDError.WalletStorageError.apply)
      _ <- secretStorage
        .upsertDIDCommitmentKey(did, CommitmentPurpose.Recovery, secret.recoveryCommitmentSecret)
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

  // TODO: implement
  def updateDIDAndPublish(
      did: PrismDID,
      template: ManagedDIDUpdateTemplate
  ): IO[UpdateManagedDIDError, PublishedDIDOperationOutcome] = {
//    for {
//      op <-
//    } yield ???

    ???
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
        keyPairs = keys.map { case (keyPair, template) => template.id -> keyPair }.toMap
      )
    } yield operation -> secret
  }

  // TODO: implement
  private def generateUpdateOperation(
      updateTemplate: ManagedDIDUpdateTemplate
  ): IO[UpdateManagedDIDError, (PublishedDIDOperation.Update, UpdateDIDSecret)] = ???

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

}

object ManagedDIDService {

  private final case class CreateDIDSecret(
      updateCommitmentSecret: ECKeyPair,
      recoveryCommitmentSecret: ECKeyPair,
      keyPairs: Map[String, ECKeyPair]
  )

  private final case class UpdateDIDSecret()

  def inMemoryStorage: URLayer[DIDService & DIDOperationValidator, ManagedDIDService] =
    (InMemoryDIDNonSecretStorage.layer ++ InMemoryDIDSecretStorage.layer) >>> ZLayer.fromFunction(
      ManagedDIDService(_, _, _, _)
    )
}
