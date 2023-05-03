package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.crypto.{ECWrapper, KeyGeneratorWrapper}
import io.iohk.atala.agent.walletapi.model.{
  DIDPublicKeyTemplate,
  DIDUpdateLineage,
  ECKeyPair,
  ManagedDIDDetail,
  ManagedDIDState,
  ManagedDIDTemplate,
  UpdateManagedDIDAction
}
import io.iohk.atala.agent.walletapi.model.ECCoordinates.*
import io.iohk.atala.agent.walletapi.model.error.{*, given}
import io.iohk.atala.agent.walletapi.service.ManagedDIDService.DEFAULT_MASTER_KEY_ID
import io.iohk.atala.agent.walletapi.storage.{DIDNonSecretStorage, DIDSecretStorage}
import io.iohk.atala.agent.walletapi.util.{
  ManagedDIDTemplateValidator,
  OperationFactory,
  UpdateDIDSecret,
  UpdateManagedDIDActionValidator
}
import io.iohk.atala.castor.core.model.did.{
  CanonicalPrismDID,
  DID,
  DIDMetadata,
  EllipticCurve,
  InternalKeyPurpose,
  InternalPublicKey,
  LongFormPrismDID,
  PrismDID,
  PrismDIDOperation,
  PublicKey,
  PublicKeyData,
  ScheduleDIDOperationOutcome,
  ScheduledDIDOperationStatus,
  SignedPrismDIDOperation
}
import io.iohk.atala.castor.core.model.error.DIDOperationError
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.castor.core.util.DIDOperationValidator
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.prism.crypto.util.Random
import io.iohk.atala.shared.models.{Base64UrlString, HexString}
import zio.*

import scala.collection.immutable.ArraySeq
import io.iohk.atala.mercury.PeerDID
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.agent.walletapi.sql.JdbcDIDSecretStorage
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.security.spec.ECPoint
import java.security.{KeyFactory, PrivateKey as JavaPrivateKey, PublicKey as JavaPublicKey}
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import java.security.spec.ECPrivateKeySpec
import java.security.spec.ECPublicKeySpec

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
  private val AGREEMENT_KEY_ID = "agreement"
  private val AUTHENTICATION_KEY_ID = "authentication"

  private val generateCreateOperation = OperationFactory.makeCreateOperation(
    DEFAULT_MASTER_KEY_ID,
    CURVE,
    () => KeyGeneratorWrapper.generateECKeyPair(CURVE)
  )

  private val generateUpdateOperation = OperationFactory.makeUpdateOperation(
    CURVE,
    () => KeyGeneratorWrapper.generateECKeyPair(CURVE)
  )

  def syncManagedDIDState: IO[GetManagedDIDError, Unit] = nonSecretStorage
    .listManagedDID(offset = None, limit = None)
    .mapError(GetManagedDIDError.WalletStorageError.apply)
    .flatMap { case (kv, _) =>
      ZIO.foreach(kv.map(_._1.asCanonical))(computeNewDIDStateFromDLTAndPersist[GetManagedDIDError])
    }
    .unit

  def syncUnconfirmedUpdateOperations: IO[GetManagedDIDError, Unit] = syncUnconfirmedUpdateOperationsByDID(did = None)

  // FIXME
  // Instead of returning the privateKey directly, it should provide more secure interface like
  // {{{ def signWithDID(did, keyId, bytes): IO[?, Array[Byte]] }}}.
  // For the time being, the purpose of this method is just to disallow SecretStorage to be
  // used outside of this module.
  def javaKeyPairWithDID(
      did: CanonicalPrismDID,
      keyId: String
  ): IO[GetKeyError, Option[(JavaPrivateKey, JavaPublicKey)]] = {
    secretStorage
      .getKey(did, keyId)
      .mapError(GetKeyError.WalletStorageError.apply)
      .flatMap { maybeKeyPair =>
        maybeKeyPair.fold(ZIO.none) { ecKeyPair =>
          ZIO
            .attempt {
              // TODO: Simplify conversion of ECKeyPair to JDK security classes
              val ba = ecKeyPair.privateKey.toPaddedByteArray(EllipticCurve.SECP256K1)
              val keyFactory = KeyFactory.getInstance("EC", new BouncyCastleProvider())
              val ecParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
              val ecNamedCurveSpec = ECNamedCurveSpec(
                ecParameterSpec.getName(),
                ecParameterSpec.getCurve(),
                ecParameterSpec.getG(),
                ecParameterSpec.getN()
              )
              val ecPrivateKeySpec = ECPrivateKeySpec(java.math.BigInteger(1, ba), ecNamedCurveSpec)
              val privateKey = keyFactory.generatePrivate(ecPrivateKeySpec)
              val bcECPoint = ecParameterSpec
                .getG()
                .multiply(privateKey.asInstanceOf[org.bouncycastle.jce.interfaces.ECPrivateKey].getD())
              val ecPublicKeySpec = ECPublicKeySpec(
                new ECPoint(
                  bcECPoint.normalize().getAffineXCoord().toBigInteger(),
                  bcECPoint.normalize().getAffineYCoord().toBigInteger()
                ),
                ecNamedCurveSpec
              )
              val publicKey = keyFactory.generatePublic(ecPublicKeySpec)
              (privateKey, publicKey)
            }
            .mapError(GetKeyError.KeyConstructionError.apply)
            .asSome
        }
      }
  }

  def getManagedDIDState(did: CanonicalPrismDID): IO[GetManagedDIDError, Option[ManagedDIDState]] =
    for {
      // state in wallet maybe stale, update it from DLT
      _ <- computeNewDIDStateFromDLTAndPersist(did)
      state <- nonSecretStorage.getManagedDIDState(did).mapError(GetManagedDIDError.WalletStorageError.apply)
    } yield state

  /** @return A tuple containing a list of items and a count of total items */
  def listManagedDIDPage(offset: Int, limit: Int): IO[GetManagedDIDError, (Seq[ManagedDIDDetail], Int)] =
    for {
      results <- nonSecretStorage
        .listManagedDID(offset = Some(offset), limit = Some(limit))
        .mapError(GetManagedDIDError.WalletStorageError.apply)
      (dids, totalCount) = results
      details = dids.map { case (did, state) => ManagedDIDDetail(did.asCanonical, state) }
    } yield details -> totalCount

  def publishStoredDID(did: CanonicalPrismDID): IO[PublishManagedDIDError, ScheduleDIDOperationOutcome] = {
    def doPublish(operation: PrismDIDOperation.Create) = {
      for {
        signedOperation <- signOperationWithMasterKey[PublishManagedDIDError](operation)
        outcome <- submitSignedOperation[PublishManagedDIDError](signedOperation)
        _ <- nonSecretStorage
          .setManagedDIDState(did, ManagedDIDState.PublicationPending(operation, outcome.operationId))
          .mapError(PublishManagedDIDError.WalletStorageError.apply)
      } yield outcome
    }

    for {
      _ <- computeNewDIDStateFromDLTAndPersist[PublishManagedDIDError](did)
      didState <- nonSecretStorage
        .getManagedDIDState(did)
        .mapError(PublishManagedDIDError.WalletStorageError.apply)
        .someOrFail(PublishManagedDIDError.DIDNotFound(did))
      outcome <- didState match {
        case ManagedDIDState.Created(operation) => doPublish(operation)
        case ManagedDIDState.PublicationPending(operation, publishOperationId) =>
          ZIO.succeed(ScheduleDIDOperationOutcome(did, operation, publishOperationId))
        case ManagedDIDState.Published(operation, publishOperationId) =>
          ZIO.succeed(ScheduleDIDOperationOutcome(did, operation, publishOperationId))
      }
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
      _ <- ZIO
        .fromEither(didOpValidator.validate(createOperation))
        .mapError(CreateManagedDIDError.InvalidOperation.apply)
      _ <- nonSecretStorage
        .getManagedDIDState(did)
        .mapError(CreateManagedDIDError.WalletStorageError.apply)
        .filterOrFail(_.isEmpty)(CreateManagedDIDError.DIDAlreadyExists(did))
      _ <- ZIO
        .foreachDiscard(secret.keyPairs ++ secret.internalKeyPairs) { case (keyId, keyPair) =>
          secretStorage.insertKey(did, keyId, keyPair, createOperation.toAtalaOperationHash)
        }
        .mapError(CreateManagedDIDError.WalletStorageError.apply)
      // A DID is considered created after a successful setState
      // If some steps above failed, it is not considered created and data that
      // are persisted along the way may be garbage collected.
      _ <- nonSecretStorage
        .setManagedDIDState(did, ManagedDIDState.Created(createOperation))
        .mapError(CreateManagedDIDError.WalletStorageError.apply)
    } yield longFormDID
  }

  def updateManagedDID(
      did: CanonicalPrismDID,
      actions: Seq[UpdateManagedDIDAction]
  ): IO[UpdateManagedDIDError, ScheduleDIDOperationOutcome] = {
    def doUpdate(operation: PrismDIDOperation.Update, secret: UpdateDIDSecret) = {
      val operationHash = operation.toAtalaOperationHash
      for {
        signedOperation <- signOperationWithMasterKey[UpdateManagedDIDError](operation)
        updateLineage <- Clock.instant.map { now =>
          DIDUpdateLineage(
            operationId = ArraySeq.from(signedOperation.toAtalaOperationId),
            operationHash = ArraySeq.from(operation.toAtalaOperationHash),
            previousOperationHash = operation.previousOperationHash,
            status = ScheduledDIDOperationStatus.Pending,
            createdAt = now,
            updatedAt = now
          )
        }
        _ <- ZIO
          .foreachDiscard(secret.newKeyPairs) { case (keyId, keyPair) =>
            secretStorage.insertKey(did, keyId, keyPair, operationHash)
          }
          .mapError(UpdateManagedDIDError.WalletStorageError.apply)
        _ <- nonSecretStorage
          .insertDIDUpdateLineage(did, updateLineage)
          .mapError(UpdateManagedDIDError.WalletStorageError.apply)
        outcome <- submitSignedOperation[UpdateManagedDIDError](signedOperation)
      } yield outcome
    }

    for {
      _ <- ZIO
        .fromEither(UpdateManagedDIDActionValidator.validate(actions))
        .mapError(UpdateManagedDIDError.InvalidArgument.apply)
      _ <- computeNewDIDStateFromDLTAndPersist[UpdateManagedDIDError](did)
      didState <- nonSecretStorage
        .getManagedDIDState(did)
        .mapError(UpdateManagedDIDError.WalletStorageError.apply)
        .someOrFail(UpdateManagedDIDError.DIDNotFound(did))
        .collect(UpdateManagedDIDError.DIDNotPublished(did)) { case s: ManagedDIDState.Published => s }
      resolvedDID <- didService
        .resolveDID(did)
        .mapError(UpdateManagedDIDError.ResolutionError.apply)
        .someOrFail(UpdateManagedDIDError.DIDNotFound(did))
        .filterOrFail { case (metaData, _) => !metaData.deactivated }(UpdateManagedDIDError.DIDAlreadyDeactivated(did))
      previousOperationHash <- computePreviousOperationHash[UpdateManagedDIDError](
        did,
        resolvedDID._1,
        didState.createOperation
      )
      generated <- generateUpdateOperation(did, previousOperationHash, actions)
      (updateOperation, secret) = generated
      _ <- ZIO
        .fromEither(didOpValidator.validate(updateOperation))
        .mapError(UpdateManagedDIDError.InvalidOperation.apply)
      outcome <- doUpdate(updateOperation, secret)
    } yield outcome
  }

  def deactivateManagedDID(did: CanonicalPrismDID): IO[UpdateManagedDIDError, ScheduleDIDOperationOutcome] = {
    def doDeactivate(operation: PrismDIDOperation.Deactivate) = {
      for {
        signedOperation <- signOperationWithMasterKey[UpdateManagedDIDError](operation)
        outcome <- submitSignedOperation[UpdateManagedDIDError](signedOperation)
      } yield outcome
    }

    for {
      _ <- computeNewDIDStateFromDLTAndPersist[UpdateManagedDIDError](did)
      didState <- nonSecretStorage
        .getManagedDIDState(did)
        .mapError(UpdateManagedDIDError.WalletStorageError.apply)
        .someOrFail(UpdateManagedDIDError.DIDNotFound(did))
        .collect(UpdateManagedDIDError.DIDNotPublished(did)) { case s: ManagedDIDState.Published => s }
      resolvedDID <- didService
        .resolveDID(did)
        .mapError(UpdateManagedDIDError.ResolutionError.apply)
        .someOrFail(UpdateManagedDIDError.DIDNotFound(did))
        .filterOrFail { case (metaData, _) => !metaData.deactivated }(UpdateManagedDIDError.DIDAlreadyDeactivated(did))
      previousOperationHash <- computePreviousOperationHash[UpdateManagedDIDError](
        did,
        resolvedDID._1,
        didState.createOperation
      )
      deactivateOperation = PrismDIDOperation.Deactivate(did, ArraySeq.from(previousOperationHash))
      _ <- ZIO
        .fromEither(didOpValidator.validate(deactivateOperation))
        .mapError(UpdateManagedDIDError.InvalidOperation.apply)
      outcome <- doDeactivate(deactivateOperation)
    } yield outcome
  }

  /** return hash of previous operation. Currently support only last confirmed operation */
  private def computePreviousOperationHash[E](
      did: CanonicalPrismDID,
      didMetadata: DIDMetadata,
      createOperation: PrismDIDOperation.Create
  )(using c1: Conversion[CommonWalletStorageError, E], c2: Conversion[DIDOperationError, E]): IO[E, Array[Byte]] = {
    for {
      previousOperationHashInternal <- nonSecretStorage
        .listUpdateLineage(did = Some(did), status = Some(ScheduledDIDOperationStatus.Confirmed))
        .mapError[E](CommonWalletStorageError.apply)
        .map { lineage =>
          // Correlation between local timestamp and confirmed operation order on-chain is assumed.
          val lastConfirmedUpdate = lineage.maxByOption(_.createdAt)
          lastConfirmedUpdate.fold(ArraySeq.from(createOperation.toAtalaOperationHash))(_.operationHash)
        }
      // Sync operation lineage status if the tip is different from lastOperation in resolution metadata
      // This is done to avoid a race condition where users resolve the DID and see
      // the update is already applied but the agent doesn't know about it yet.
      _ <- syncUnconfirmedUpdateOperationsByDID[E](did = Some(did))
        .when(didMetadata.lastOperationHash != previousOperationHashInternal)
    } yield didMetadata.lastOperationHash.toArray
  }

  private def syncUnconfirmedUpdateOperationsByDID[E](
      did: Option[PrismDID]
  )(using c1: Conversion[CommonWalletStorageError, E], c2: Conversion[DIDOperationError, E]): IO[E, Unit] = {
    for {
      awaitingConfirmationOps <- nonSecretStorage
        .listUpdateLineage(did = did, status = Some(ScheduledDIDOperationStatus.AwaitingConfirmation))
        .mapError[E](CommonWalletStorageError.apply)
      pendingOps <- nonSecretStorage
        .listUpdateLineage(did = did, status = Some(ScheduledDIDOperationStatus.Pending))
        .mapError[E](CommonWalletStorageError.apply)
      _ <- ZIO.foreach(awaitingConfirmationOps ++ pendingOps)(computeNewDIDLineageStatusAndPersist[E])
    } yield ()
  }

  private def signOperationWithMasterKey[E](operation: PrismDIDOperation)(using
      c1: Conversion[CommonWalletStorageError, E],
      c2: Conversion[CommonCryptographyError, E]
  ): IO[E, SignedPrismDIDOperation] = {
    val did = operation.did
    for {
      masterKeyPair <-
        secretStorage
          .getKey(did, DEFAULT_MASTER_KEY_ID)
          .mapError[E](CommonWalletStorageError.apply)
          .someOrElseZIO(
            ZIO.die(Exception("master-key must exists in the wallet for signing DID operation and submit to Node"))
          )
      signedOperation <- ZIO
        .fromTry(
          ECWrapper.signBytes(CURVE, operation.toAtalaOperation.toByteArray, masterKeyPair.privateKey)
        )
        .mapError[E](CommonCryptographyError.apply)
        .map(signature =>
          SignedPrismDIDOperation(
            operation = operation,
            signature = ArraySeq.from(signature),
            signedWithKey = DEFAULT_MASTER_KEY_ID
          )
        )
    } yield signedOperation
  }

  private def submitSignedOperation[E](
      signedOperation: SignedPrismDIDOperation
  )(using c1: Conversion[DIDOperationError, E]): IO[E, ScheduleDIDOperationOutcome] =
    didService.scheduleOperation(signedOperation).mapError[E](e => e)

  private def computeNewDIDLineageStatusAndPersist[E](
      updateLineage: DIDUpdateLineage
  )(using c1: Conversion[DIDOperationError, E], c2: Conversion[CommonWalletStorageError, E]): IO[E, Unit] = {
    for {
      maybeOperationDetail <- didService
        .getScheduledDIDOperationDetail(updateLineage.operationId.toArray)
        .mapError[E](e => e)
      newStatus = maybeOperationDetail.fold(ScheduledDIDOperationStatus.Rejected)(_.status)
      _ <- nonSecretStorage
        .setDIDUpdateLineageStatus(updateLineage.operationId.toArray, newStatus)
        .mapError[E](CommonWalletStorageError.apply)
        .when(updateLineage.status != newStatus)
    } yield ()
  }

  /** Reconcile state with DLT and write new state to the storage */
  private def computeNewDIDStateFromDLTAndPersist[E](
      did: CanonicalPrismDID
  )(using
      c1: Conversion[CommonWalletStorageError, E],
      c2: Conversion[DIDOperationError, E]
  ): IO[E, Unit] = {
    for {
      maybeCurrentState <- nonSecretStorage
        .getManagedDIDState(did)
        .mapError[E](CommonWalletStorageError.apply)
      maybeNewState <- ZIO.foreach(maybeCurrentState)(computeNewDIDStateFromDLT(_).mapError[E](e => e))
      _ <- ZIO.foreach(maybeCurrentState zip maybeNewState) { case (currentState, newState) =>
        nonSecretStorage
          .setManagedDIDState(did, newState)
          .mapError[E](CommonWalletStorageError.apply)
          .when(currentState != newState)
      }
    } yield ()
  }

  /** Reconcile state with DLT and return an updated state */
  private def computeNewDIDStateFromDLT(state: ManagedDIDState): IO[DIDOperationError, ManagedDIDState] = {
    state match {
      case s @ ManagedDIDState.PublicationPending(operation, publishOperationId) =>
        didService
          .getScheduledDIDOperationDetail(publishOperationId.toArray)
          .map {
            case Some(result) =>
              result.status match {
                case ScheduledDIDOperationStatus.Pending              => s
                case ScheduledDIDOperationStatus.AwaitingConfirmation => s
                case ScheduledDIDOperationStatus.Confirmed => ManagedDIDState.Published(operation, publishOperationId)
                case ScheduledDIDOperationStatus.Rejected  => ManagedDIDState.Created(operation)
              }
            case None => ManagedDIDState.Created(operation)
          }
      case s => ZIO.succeed(s)
    }
  }

  /** PeerDID related methods
    */
  def createAndStorePeerDID(serviceEndpoint: String): UIO[PeerDID] =
    for {
      peerDID <- ZIO.succeed(PeerDID.makePeerDid(serviceEndpoint = Some(serviceEndpoint)))
      _ <- secretStorage.insertKey(peerDID.did, AGREEMENT_KEY_ID, peerDID.jwkForKeyAgreement).orDie
      _ <- secretStorage.insertKey(peerDID.did, AUTHENTICATION_KEY_ID, peerDID.jwkForKeyAuthentication).orDie
    } yield peerDID

  def getPeerDID(didId: DidId): IO[DIDSecretStorageError.KeyNotFoundError, PeerDID] =
    for {
      maybeJwkForAgreement <- secretStorage.getKey(didId, AGREEMENT_KEY_ID).orDie
      jwkForAgreement <- ZIO
        .fromOption(maybeJwkForAgreement)
        .mapError(_ => DIDSecretStorageError.KeyNotFoundError(didId, AGREEMENT_KEY_ID))
      maybeJwkForAuthentication <- secretStorage.getKey(didId, AUTHENTICATION_KEY_ID).orDie
      jwkForAuthentication <- ZIO
        .fromOption(maybeJwkForAuthentication)
        .mapError(_ => DIDSecretStorageError.KeyNotFoundError(didId, AUTHENTICATION_KEY_ID))
      peerDID <- ZIO.succeed(PeerDID(didId, jwkForAgreement, jwkForAuthentication))
    } yield peerDID

}

object ManagedDIDService {

  val DEFAULT_MASTER_KEY_ID: String = "master0"

  val reservedKeyIds: Set[String] = Set(DEFAULT_MASTER_KEY_ID)

  val layer: URLayer[DIDOperationValidator & DIDService & DIDSecretStorage & DIDNonSecretStorage, ManagedDIDService] =
    ZLayer.fromFunction(ManagedDIDService(_, _, _, _))

}
