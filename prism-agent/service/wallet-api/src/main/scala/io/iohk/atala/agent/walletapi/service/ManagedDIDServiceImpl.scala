package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.crypto.Apollo
import io.iohk.atala.agent.walletapi.model.*
import io.iohk.atala.agent.walletapi.model.error.{*, given}
import io.iohk.atala.agent.walletapi.service.ManagedDIDService.DEFAULT_MASTER_KEY_ID
import io.iohk.atala.agent.walletapi.service.handler.{DIDUpdateHandler, PublicationHandler}
import io.iohk.atala.agent.walletapi.storage.{DIDNonSecretStorage, DIDSecretStorage}
import io.iohk.atala.agent.walletapi.util.*
import io.iohk.atala.castor.core.model.did.*
import io.iohk.atala.castor.core.model.error.DIDOperationError
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.castor.core.util.DIDOperationValidator
import io.iohk.atala.mercury.PeerDID
import io.iohk.atala.mercury.model.DidId
import zio.*

import java.security.{PrivateKey as JavaPrivateKey, PublicKey as JavaPublicKey}
import scala.collection.immutable.ArraySeq

/** A wrapper around Castor's DIDService providing key-management capability. Analogous to the secretAPI in
  * indy-wallet-sdk.
  */
final class ManagedDIDServiceImpl private[walletapi] (
    didService: DIDService,
    didOpValidator: DIDOperationValidator,
    private[walletapi] val secretStorage: DIDSecretStorage,
    override private[walletapi] val nonSecretStorage: DIDNonSecretStorage,
    apollo: Apollo,
    seed: Array[Byte]
) extends ManagedDIDService {

  private val CURVE = EllipticCurve.SECP256K1
  private val AGREEMENT_KEY_ID = "agreement"
  private val AUTHENTICATION_KEY_ID = "authentication"

  private val keyResolver = KeyResolver(apollo, nonSecretStorage, secretStorage)(seed)

  private val publicationHandler = PublicationHandler(didService, keyResolver)(DEFAULT_MASTER_KEY_ID)
  private val didUpdateHandler = DIDUpdateHandler(apollo, nonSecretStorage, secretStorage, publicationHandler)(seed)

  private val generateCreateOperationHdKey =
    OperationFactory(apollo).makeCreateOperationHdKey(DEFAULT_MASTER_KEY_ID, seed)

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
    nonSecretStorage
      .getManagedDIDState(did)
      .flatMap {
        case None        => ZIO.none
        case Some(state) => keyResolver.getKey(state.createOperation.did, state.keyMode, keyId)
      }
      .mapBoth(
        GetKeyError.WalletStorageError.apply,
        _.map { ecKeyPair =>
          (ecKeyPair.privateKey.toJavaPrivateKey, ecKeyPair.publicKey.toJavaPublicKey)
        }
      )
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

  // TODO: update this method to use the same handler as updateManagedDID
  def publishStoredDID(did: CanonicalPrismDID): IO[PublishManagedDIDError, ScheduleDIDOperationOutcome] = {
    def doPublish(state: ManagedDIDState) = {
      for {
        signedOperation <- publicationHandler
          .signOperationWithMasterKey[PublishManagedDIDError](state, state.createOperation)
        outcome <- publicationHandler.submitSignedOperation[PublishManagedDIDError](signedOperation)
        publicationState = PublicationState.PublicationPending(outcome.operationId)
        _ <- nonSecretStorage
          .updateManagedDID(did, ManagedDIDStatePatch(publicationState))
          .mapError(PublishManagedDIDError.WalletStorageError.apply)
      } yield outcome
    }

    for {
      _ <- computeNewDIDStateFromDLTAndPersist[PublishManagedDIDError](did)
      didState <- nonSecretStorage
        .getManagedDIDState(did)
        .mapError(PublishManagedDIDError.WalletStorageError.apply)
        .someOrFail(PublishManagedDIDError.DIDNotFound(did))
      outcome <- didState.publicationState match {
        case PublicationState.Created() => doPublish(didState)
        case PublicationState.PublicationPending(publishOperationId) =>
          ZIO.succeed(ScheduleDIDOperationOutcome(did, didState.createOperation, publishOperationId))
        case PublicationState.Published(publishOperationId) =>
          ZIO.succeed(ScheduleDIDOperationOutcome(did, didState.createOperation, publishOperationId))
      }
    } yield outcome
  }

  // TODO: update this method to use the same handler as updateManagedDID
  def createAndStoreDID(didTemplate: ManagedDIDTemplate): IO[CreateManagedDIDError, LongFormPrismDID] = {
    for {
      _ <- ZIO
        .fromEither(ManagedDIDTemplateValidator.validate(didTemplate))
        .mapError(CreateManagedDIDError.InvalidArgument.apply)
      didIndex <- nonSecretStorage
        .getMaxDIDIndex()
        .mapBoth(
          CreateManagedDIDError.WalletStorageError.apply,
          maybeIdx => maybeIdx.map(_ + 1).getOrElse(0)
        )
      generated <- generateCreateOperationHdKey(didIndex, didTemplate)
      (createOperation, hdKey) = generated
      longFormDID = PrismDID.buildLongFormFromOperation(createOperation)
      did = longFormDID.asCanonical
      _ <- ZIO
        .fromEither(didOpValidator.validate(createOperation))
        .mapError(CreateManagedDIDError.InvalidOperation.apply)
      state = ManagedDIDState(createOperation, didIndex, PublicationState.Created())
      _ <- nonSecretStorage
        .insertManagedDID(did, state, hdKey.keyPaths ++ hdKey.internalKeyPaths)
        .mapError(CreateManagedDIDError.WalletStorageError.apply)
    } yield longFormDID
  }

  def updateManagedDID(
      did: CanonicalPrismDID,
      actions: Seq[UpdateManagedDIDAction]
  ): IO[UpdateManagedDIDError, ScheduleDIDOperationOutcome] = {
    for {
      _ <- ZIO
        .fromEither(UpdateManagedDIDActionValidator.validate(actions))
        .mapError(UpdateManagedDIDError.InvalidArgument.apply)
      _ <- computeNewDIDStateFromDLTAndPersist[UpdateManagedDIDError](did)
      didState <- nonSecretStorage
        .getManagedDIDState(did)
        .mapError(UpdateManagedDIDError.WalletStorageError.apply)
        .someOrFail(UpdateManagedDIDError.DIDNotFound(did))
        .collect(UpdateManagedDIDError.DIDNotPublished(did)) {
          case s @ ManagedDIDState(_, _, PublicationState.Published(_)) => s
        }
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
      _ <- getUnconfirmedUpdateOperationByDid[UpdateManagedDIDError](Some(did))
        .filterOrFail(_.isEmpty)(UpdateManagedDIDError.MultipleInflightUpdateNotAllowed(did))
      material <- didUpdateHandler.materialize(didState, previousOperationHash, actions)
      _ <- ZIO
        .fromEither(didOpValidator.validate(material.operation))
        .mapError(UpdateManagedDIDError.InvalidOperation.apply)
      _ <- material.persist.mapError(UpdateManagedDIDError.WalletStorageError.apply)
      outcome <- publicationHandler.submitSignedOperation[UpdateManagedDIDError](material.signedOperation)
    } yield outcome
  }

  // TODO: refactor this method to use the same handler as updateManagedDID
  def deactivateManagedDID(did: CanonicalPrismDID): IO[UpdateManagedDIDError, ScheduleDIDOperationOutcome] = {
    def doDeactivate(state: ManagedDIDState, operation: PrismDIDOperation.Deactivate) = {
      for {
        signedOperation <- publicationHandler.signOperationWithMasterKey[UpdateManagedDIDError](state, operation)
        outcome <- publicationHandler.submitSignedOperation[UpdateManagedDIDError](signedOperation)
      } yield outcome
    }

    for {
      _ <- computeNewDIDStateFromDLTAndPersist[UpdateManagedDIDError](did)
      didState <- nonSecretStorage
        .getManagedDIDState(did)
        .mapError(UpdateManagedDIDError.WalletStorageError.apply)
        .someOrFail(UpdateManagedDIDError.DIDNotFound(did))
        .collect(UpdateManagedDIDError.DIDNotPublished(did)) {
          case s @ ManagedDIDState(_, _, PublicationState.Published(_)) => s
        }
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
      _ <- getUnconfirmedUpdateOperationByDid[UpdateManagedDIDError](Some(did))
        .filterOrFail(_.isEmpty)(UpdateManagedDIDError.MultipleInflightUpdateNotAllowed(did))
      deactivateOperation = PrismDIDOperation.Deactivate(did, ArraySeq.from(previousOperationHash))
      _ <- ZIO
        .fromEither(didOpValidator.validate(deactivateOperation))
        .mapError(UpdateManagedDIDError.InvalidOperation.apply)
      outcome <- doDeactivate(didState, deactivateOperation)
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
      unconfirmedOps <- getUnconfirmedUpdateOperationByDid(did)
      _ <- ZIO.foreach(unconfirmedOps)(computeNewDIDLineageStatusAndPersist[E])
    } yield ()
  }

  private def getUnconfirmedUpdateOperationByDid[E](did: Option[PrismDID])(using
      c1: Conversion[CommonWalletStorageError, E],
      c2: Conversion[DIDOperationError, E]
  ): IO[E, Seq[DIDUpdateLineage]] = {
    for {
      awaitingConfirmationOps <- nonSecretStorage
        .listUpdateLineage(did = did, status = Some(ScheduledDIDOperationStatus.AwaitingConfirmation))
        .mapError[E](CommonWalletStorageError.apply)
      pendingOps <- nonSecretStorage
        .listUpdateLineage(did = did, status = Some(ScheduledDIDOperationStatus.Pending))
        .mapError[E](CommonWalletStorageError.apply)
    } yield awaitingConfirmationOps ++ pendingOps
  }

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
      maybeNewPubState <- ZIO
        .foreach(maybeCurrentState)(i => computeNewDIDStateFromDLT(i.publicationState))
        .mapError[E](e => e)
      _ <- ZIO.foreach(maybeCurrentState zip maybeNewPubState) { case (currentState, newPubState) =>
        nonSecretStorage
          .updateManagedDID(did, ManagedDIDStatePatch(newPubState))
          .mapError[E](CommonWalletStorageError.apply)
          .when(currentState.publicationState != newPubState)
      }
    } yield ()
  }

  /** Reconcile state with DLT and return an updated state */
  private def computeNewDIDStateFromDLT(publicationState: PublicationState): IO[DIDOperationError, PublicationState] = {
    publicationState match {
      case s @ PublicationState.PublicationPending(publishOperationId) =>
        didService
          .getScheduledDIDOperationDetail(publishOperationId.toArray)
          .map {
            case Some(result) =>
              result.status match {
                case ScheduledDIDOperationStatus.Pending              => s
                case ScheduledDIDOperationStatus.AwaitingConfirmation => s
                case ScheduledDIDOperationStatus.Confirmed            => PublicationState.Published(publishOperationId)
                case ScheduledDIDOperationStatus.Rejected             => PublicationState.Created()
              }
            case None => PublicationState.Created()
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

object ManagedDIDServiceImpl {

  val layer: RLayer[
    DIDOperationValidator & DIDService & DIDSecretStorage & DIDNonSecretStorage & Apollo & SeedResolver,
    ManagedDIDService
  ] = {
    ZLayer.fromZIO {
      for {
        didService <- ZIO.service[DIDService]
        didOpValidator <- ZIO.service[DIDOperationValidator]
        secretStorage <- ZIO.service[DIDSecretStorage]
        nonSecretStorage <- ZIO.service[DIDNonSecretStorage]
        apollo <- ZIO.service[Apollo]
        seed <- ZIO.serviceWithZIO[SeedResolver](_.resolve)
      } yield ManagedDIDServiceImpl(didService, didOpValidator, secretStorage, nonSecretStorage, apollo, seed)
    }
  }

}
