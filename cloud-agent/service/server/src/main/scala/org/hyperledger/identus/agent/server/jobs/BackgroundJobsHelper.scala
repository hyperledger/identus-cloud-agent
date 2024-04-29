package org.hyperledger.identus.agent.server.jobs

import org.hyperledger.identus.agent.walletapi.model.error.DIDSecretStorageError.KeyNotFoundError
import org.hyperledger.identus.agent.walletapi.model.{ManagedDIDState, PublicationState}
import org.hyperledger.identus.agent.walletapi.service.ManagedDIDService
import org.hyperledger.identus.castor.core.model.did.{LongFormPrismDID, PrismDID, VerificationRelationship}
import org.hyperledger.identus.castor.core.service.DIDService
import org.hyperledger.identus.mercury.model.DidId
import org.hyperledger.identus.mercury.{AgentPeerService, DidAgent}
import org.hyperledger.identus.pollux.vc.jwt.{ES256KSigner, Issuer as JwtIssuer}
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.{ZIO, ZLayer}
import org.hyperledger.identus.agent.walletapi.storage.DIDNonSecretStorage
import org.hyperledger.identus.agent.walletapi.model.error.DIDSecretStorageError.WalletNotFoundError

trait BackgroundJobsHelper {

  def getLongForm(
      did: PrismDID,
      allowUnpublishedIssuingDID: Boolean = false
  ): ZIO[ManagedDIDService & WalletAccessContext, Throwable, LongFormPrismDID] = {
    for {
      managedDIDService <- ZIO.service[ManagedDIDService]
      didState <- managedDIDService
        .getManagedDIDState(did.asCanonical)
        .mapError(e => RuntimeException(s"Error occurred while getting did from wallet: ${e.toString}"))
        .someOrFail(RuntimeException(s"Issuer DID does not exist in the wallet: $did"))
        .flatMap {
          case s @ ManagedDIDState(_, _, PublicationState.Published(_)) => ZIO.succeed(s)
          case s => ZIO.cond(allowUnpublishedIssuingDID, s, RuntimeException(s"Issuer DID must be published: $did"))
        }
      longFormPrismDID = PrismDID.buildLongFormFromOperation(didState.createOperation)
    } yield longFormPrismDID
  }

  def createJwtIssuer(
      jwtIssuerDID: PrismDID,
      verificationRelationship: VerificationRelationship
  ): ZIO[DIDService & ManagedDIDService & WalletAccessContext, Throwable, JwtIssuer] = {
    for {
      managedDIDService <- ZIO.service[ManagedDIDService]
      didService <- ZIO.service[DIDService]
      // Automatically infer keyId to use by resolving DID and choose the corresponding VerificationRelationship
      issuingKeyId <- didService
        .resolveDID(jwtIssuerDID)
        .mapError(e => RuntimeException(s"Error occured while resolving Issuing DID during VC creation: ${e.toString}"))
        .someOrFail(RuntimeException(s"Issuing DID resolution result is not found"))
        .map { case (_, didData) => didData.publicKeys.find(_.purpose == verificationRelationship).map(_.id) }
        .someOrFail(
          RuntimeException(s"Issuing DID doesn't have a key in ${verificationRelationship.name} to use: $jwtIssuerDID")
        )
      ecKeyPair <- managedDIDService
        .javaKeyPairWithDID(jwtIssuerDID.asCanonical, issuingKeyId)
        .mapError(e => RuntimeException(s"Error occurred while getting issuer key-pair: ${e.toString}"))
        .someOrFail(
          RuntimeException(s"Issuer key-pair does not exist in the wallet: ${jwtIssuerDID.toString}#$issuingKeyId")
        )
      (privateKey, publicKey) = ecKeyPair
      jwtIssuer = JwtIssuer(
        org.hyperledger.identus.pollux.vc.jwt.DID(jwtIssuerDID.toString),
        ES256KSigner(privateKey),
        publicKey
      )
    } yield jwtIssuer
  }

  def buildDIDCommAgent(
      myDid: DidId
  ): ZIO[ManagedDIDService & WalletAccessContext, KeyNotFoundError, ZLayer[Any, Nothing, DidAgent]] = {
    for {
      managedDidService <- ZIO.service[ManagedDIDService]
      peerDID <- managedDidService.getPeerDID(myDid)
      agent = AgentPeerService.makeLayer(peerDID)
    } yield agent
  }

  def buildWalletAccessContextLayer(
      myDid: DidId
  ): ZIO[DIDNonSecretStorage, WalletNotFoundError, WalletAccessContext] = {
    for {
      nonSecretStorage <- ZIO.service[DIDNonSecretStorage]
      maybePeerDIDRecord <- nonSecretStorage.getPeerDIDRecord(myDid).orDie
      peerDIDRecord <- ZIO.fromOption(maybePeerDIDRecord).mapError(_ => WalletNotFoundError(myDid))
      _ <- ZIO.logInfo(s"PeerDID record successfully loaded in DIDComm receiver endpoint: $peerDIDRecord")
      walletAccessContext = WalletAccessContext(peerDIDRecord.walletId)
    } yield walletAccessContext
  }

}
