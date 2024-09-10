package org.hyperledger.identus.agent.server.jobs

import org.hyperledger.identus.agent.walletapi.model.{ManagedDIDState, PublicationState}
import org.hyperledger.identus.agent.walletapi.model.error.DIDSecretStorageError.{KeyNotFoundError, WalletNotFoundError}
import org.hyperledger.identus.agent.walletapi.model.error.GetManagedDIDError
import org.hyperledger.identus.agent.walletapi.service.ManagedDIDService
import org.hyperledger.identus.agent.walletapi.storage.DIDNonSecretStorage
import org.hyperledger.identus.castor.core.model.did.{
  EllipticCurve,
  LongFormPrismDID,
  PrismDID,
  VerificationRelationship
}
import org.hyperledger.identus.castor.core.model.error.DIDResolutionError
import org.hyperledger.identus.castor.core.service.DIDService
import org.hyperledger.identus.mercury.{AgentPeerService, DidAgent}
import org.hyperledger.identus.mercury.model.DidId
import org.hyperledger.identus.mercury.protocol.invitation.v2.Invitation
import org.hyperledger.identus.pollux.core.model.error.{CredentialServiceError, PresentationError}
import org.hyperledger.identus.pollux.core.model.DidCommID
import org.hyperledger.identus.pollux.core.service.CredentialService
import org.hyperledger.identus.pollux.sdjwt.SDJWT.*
import org.hyperledger.identus.pollux.vc.jwt.{
  DIDResolutionFailed,
  DIDResolutionSucceeded,
  DidResolver as JwtDidResolver,
  ES256KSigner,
  Issuer as JwtIssuer,
  *
}
import org.hyperledger.identus.shared.crypto.*
import org.hyperledger.identus.shared.models.{KeyId, WalletAccessContext}
import zio.{ZIO, ZLayer}

import java.time.Instant
import java.util.Base64

trait BackgroundJobsHelper {

  def getLongForm(
      did: PrismDID,
      allowUnpublishedIssuingDID: Boolean = false
  ): ZIO[ManagedDIDService & WalletAccessContext, BackgroundJobError | GetManagedDIDError, LongFormPrismDID] = {
    for {
      managedDIDService <- ZIO.service[ManagedDIDService]
      didState <- managedDIDService
        .getManagedDIDState(did.asCanonical)
        .someOrFail(BackgroundJobError.InvalidState(s"Issuer DID does not exist in the wallet: $did"))
        .flatMap {
          case s @ ManagedDIDState(_, _, PublicationState.Published(_)) => ZIO.succeed(s)
          case s =>
            ZIO.cond(
              allowUnpublishedIssuingDID,
              s,
              BackgroundJobError.InvalidState(s"Issuer DID must be published: $did")
            )
        }
      longFormPrismDID = PrismDID.buildLongFormFromOperation(didState.createOperation)
    } yield longFormPrismDID
  }

  def createJwtIssuer(
      jwtIssuerDID: PrismDID,
      verificationRelationship: VerificationRelationship
  ): ZIO[
    DIDService & ManagedDIDService & WalletAccessContext,
    BackgroundJobError | GetManagedDIDError | DIDResolutionError,
    JwtIssuer
  ] = {
    for {
      managedDIDService <- ZIO.service[ManagedDIDService]
      didService <- ZIO.service[DIDService]
      // Automatically infer keyId to use by resolving DID and choose the corresponding VerificationRelationship
      issuingKeyId <- didService
        .resolveDID(jwtIssuerDID)
        .someOrFail(BackgroundJobError.InvalidState(s"Issuing DID resolution result is not found"))
        .map { case (_, didData) =>
          didData.publicKeys
            .find(pk => pk.purpose == verificationRelationship && pk.publicKeyData.crv == EllipticCurve.SECP256K1)
            .map(_.id)
        }
        .someOrFail(
          BackgroundJobError.InvalidState(
            s"Issuing DID doesn't have a key in ${verificationRelationship.name} to use: $jwtIssuerDID"
          )
        )
      jwtIssuer <- managedDIDService
        .findDIDKeyPair(jwtIssuerDID.asCanonical, issuingKeyId)
        .flatMap {
          case None =>
            ZIO.fail(
              BackgroundJobError
                .InvalidState(s"Issuer key-pair does not exist in the wallet: ${jwtIssuerDID.toString}#$issuingKeyId")
            )
          case Some(Ed25519KeyPair(publicKey, privateKey)) =>
            ZIO.fail(
              BackgroundJobError.InvalidState(
                s"Issuer key-pair '$issuingKeyId' is of the type Ed25519. It's not supported by this feature in this version"
              )
            )
          case Some(X25519KeyPair(publicKey, privateKey)) =>
            ZIO.fail(
              BackgroundJobError.InvalidState(
                s"Issuer key-pair '$issuingKeyId' is of the type X25519. It's not supported by this feature in this version"
              )
            )
          case Some(Secp256k1KeyPair(publicKey, privateKey)) =>
            ZIO.succeed(
              JwtIssuer(
                jwtIssuerDID.did,
                ES256KSigner(privateKey.toJavaPrivateKey),
                publicKey.toJavaPublicKey
              )
            )
        }
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

  def findHolderEd25519SigningKey(
      proverDid: PrismDID,
      verificationRelationship: VerificationRelationship,
      keyId: KeyId
  ): ZIO[
    DIDService & ManagedDIDService & WalletAccessContext,
    DIDResolutionError | BackgroundJobError,
    Ed25519KeyPair
  ] = {
    for {
      managedDIDService <- ZIO.service[ManagedDIDService]
      didService <- ZIO.service[DIDService]
      issuingKeyId <- didService
        .resolveDID(proverDid)
        .mapError(e =>
          BackgroundJobError.InvalidState(
            s"Error occured while resolving Issuing DID during VC creation: ${e.toString}"
          )
        )
        .someOrFail(BackgroundJobError.InvalidState(s"Issuing DID resolution result is not found"))
        .map { case (_, didData) =>
          didData.publicKeys
            .find(pk =>
              pk.id == keyId.value
                && pk.purpose == verificationRelationship && pk.publicKeyData.crv == EllipticCurve.ED25519
            )
            .map(_.id)
        }
        .someOrFail(
          BackgroundJobError.InvalidState(
            s"Issuing DID doesn't have a key in ${verificationRelationship.name} to use: $proverDid"
          )
        )
      ed25519keyPair <- managedDIDService
        .findDIDKeyPair(proverDid.asCanonical, issuingKeyId)
        .map(_.collect { case keyPair: Ed25519KeyPair => keyPair })
        .someOrFail(
          BackgroundJobError.InvalidState(
            s"Issuer key-pair does not exist in the wallet: ${proverDid.toString}#$issuingKeyId"
          )
        )
    } yield ed25519keyPair
  }

  def resolveToEd25519PublicKey(did: String): ZIO[JwtDidResolver, PresentationError, Ed25519PublicKey] = {
    for {
      didResolverService <- ZIO.service[JwtDidResolver]
      didResolutionResult <- didResolverService.resolve(did)
      publicKeyBase64 <- didResolutionResult match {
        case failed: DIDResolutionFailed =>
          ZIO.fail(
            PresentationError.DIDResolutionFailed(did, failed.error.toString)
          )
        case succeeded: DIDResolutionSucceeded =>
          succeeded.didDocument.verificationMethod
            .find(vm => succeeded.didDocument.assertionMethod.contains(vm.id))
            .flatMap(_.publicKeyJwk.flatMap(_.x))
            .toRight(PresentationError.DIDDocumentMissing(did))
            .fold(ZIO.fail(_), ZIO.succeed(_))
      }
      ed25519PublicKey <- ZIO
        .fromTry {
          val decodedKey = Base64.getUrlDecoder.decode(publicKeyBase64)
          KmpEd25519KeyOps.publicKeyFromEncoded(decodedKey)
        }
        .mapError(t => PresentationError.PublicKeyDecodingError(t.getMessage))
    } yield ed25519PublicKey
  }

  def checkInvitationExpiry(
      id: DidCommID,
      invitation: Option[Invitation]
  ): ZIO[CredentialService & WalletAccessContext, CredentialServiceError, Unit] = {
    invitation.flatMap(_.expires_time) match {
      case Some(expiryTime) if Instant.now().getEpochSecond > expiryTime =>
        for {
          service <- ZIO.service[CredentialService]
          _ <- service.markCredentialOfferInvitationExpired(id)
          _ <- ZIO.fail(CredentialServiceError.InvitationExpired(expiryTime))
        } yield ()
      case _ => ZIO.unit
    }
  }
}
