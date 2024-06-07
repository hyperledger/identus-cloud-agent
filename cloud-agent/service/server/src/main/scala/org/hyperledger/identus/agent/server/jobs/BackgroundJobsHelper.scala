package org.hyperledger.identus.agent.server.jobs

import org.hyperledger.identus.agent.walletapi.model.{ManagedDIDState, PublicationState}
import org.hyperledger.identus.agent.walletapi.model.error.DIDSecretStorageError.{KeyNotFoundError, WalletNotFoundError}
import org.hyperledger.identus.agent.walletapi.service.ManagedDIDService
import org.hyperledger.identus.agent.walletapi.storage.DIDNonSecretStorage
import org.hyperledger.identus.castor.core.model.did.{LongFormPrismDID, PrismDID, VerificationRelationship}
import org.hyperledger.identus.castor.core.model.did.EllipticCurve
import org.hyperledger.identus.castor.core.service.DIDService
import org.hyperledger.identus.mercury.{AgentPeerService, DidAgent}
import org.hyperledger.identus.mercury.model.DidId
import org.hyperledger.identus.pollux.core.model.error.PresentationError
import org.hyperledger.identus.pollux.sdjwt.SDJWT.*
import org.hyperledger.identus.pollux.vc.jwt.{DIDResolutionFailed, DIDResolutionSucceeded, ES256KSigner, EdSigner, *}
import org.hyperledger.identus.pollux.vc.jwt.{DidResolver as JwtDidResolver, Issuer as JwtIssuer}
import org.hyperledger.identus.shared.crypto.{Ed25519KeyPair, Ed25519PublicKey, KmpEd25519KeyOps}
import org.hyperledger.identus.shared.crypto.KmpEd25519KeyOps
import org.hyperledger.identus.shared.crypto.Secp256k1KeyPair
import org.hyperledger.identus.shared.crypto.X25519KeyPair
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.{ZIO, ZLayer}

import java.util.Base64
trait BackgroundJobsHelper {

  def getLongForm(
      did: PrismDID,
      allowUnpublishedIssuingDID: Boolean = false
  ): ZIO[ManagedDIDService & WalletAccessContext, RuntimeException, LongFormPrismDID] = {
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
  ): ZIO[DIDService & ManagedDIDService & WalletAccessContext, RuntimeException, JwtIssuer] = {
    for {
      managedDIDService <- ZIO.service[ManagedDIDService]
      didService <- ZIO.service[DIDService]
      // Automatically infer keyId to use by resolving DID and choose the corresponding VerificationRelationship
      issuingKeyId <- didService
        .resolveDID(jwtIssuerDID)
        .mapError(e => RuntimeException(s"Error occured while resolving Issuing DID during VC creation: ${e.toString}"))
        .someOrFail(RuntimeException(s"Issuing DID resolution result is not found"))
        .map { case (_, didData) =>
          didData.publicKeys
            .find(pk => pk.purpose == verificationRelationship && pk.publicKeyData.crv == EllipticCurve.SECP256K1)
            .map(_.id)
        }
        .someOrFail(
          RuntimeException(s"Issuing DID doesn't have a key in ${verificationRelationship.name} to use: $jwtIssuerDID")
        )
      jwtIssuer <- managedDIDService
        .findDIDKeyPair(jwtIssuerDID.asCanonical, issuingKeyId)
        .flatMap {
          case None =>
            ZIO.fail(
              RuntimeException(s"Issuer key-pair does not exist in the wallet: ${jwtIssuerDID.toString}#$issuingKeyId")
            )
          case Some(Ed25519KeyPair(publicKey, privateKey)) =>
            ZIO.fail(
              RuntimeException(
                s"Issuer key-pair '$issuingKeyId' is of the type Ed25519. It's not supported by this feature in this version"
              )
            )
          case Some(X25519KeyPair(publicKey, privateKey)) =>
            ZIO.fail(
              RuntimeException(
                s"Issuer key-pair '$issuingKeyId' is of the type X25519. It's not supported by this feature in this version"
              )
            )
          case Some(Secp256k1KeyPair(publicKey, privateKey)) =>
            ZIO.succeed(
              JwtIssuer(
                org.hyperledger.identus.pollux.vc.jwt.DID(jwtIssuerDID.toString),
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

  def getEd25519SigningKeyPair(
      jwtIssuerDID: PrismDID,
      verificationRelationship: VerificationRelationship
  ): ZIO[DIDService & ManagedDIDService & WalletAccessContext, RuntimeException, Ed25519KeyPair] = {
    for {
      managedDIDService <- ZIO.service[ManagedDIDService]
      didService <- ZIO.service[DIDService]
      issuingKeyId <- didService
        .resolveDID(jwtIssuerDID)
        .mapError(e => RuntimeException(s"Error occured while resolving Issuing DID during VC creation: ${e.toString}"))
        .someOrFail(RuntimeException(s"Issuing DID resolution result is not found"))
        .map { case (_, didData) =>
          didData.publicKeys
            .find(pk => pk.purpose == verificationRelationship && pk.publicKeyData.crv == EllipticCurve.ED25519)
            .map(_.id)
        }
        .someOrFail(
          RuntimeException(s"Issuing DID doesn't have a key in ${verificationRelationship.name} to use: $jwtIssuerDID")
        )
      ed25519keyPair <- managedDIDService
        .findDIDKeyPair(jwtIssuerDID.asCanonical, issuingKeyId)
        .map(_.collect { case keyPair: Ed25519KeyPair => keyPair })
        .someOrFail(
          RuntimeException(s"Issuer key-pair does not exist in the wallet: ${jwtIssuerDID.toString}#$issuingKeyId")
        )
    } yield ed25519keyPair
  }

  /** @param jwtIssuerDID
    *   This can holder prism did / issuer prism did
    * @param verificationRelationship
    *   Holder it Authentication and Issuer it is AssertionMethod
    * @return
    *   JwtIssuer
    * @see
    *   org.hyperledger.identus.pollux.vc.jwt.Issuer
    */
  def getSDJwtIssuer(
      jwtIssuerDID: PrismDID,
      verificationRelationship: VerificationRelationship
  ): ZIO[DIDService & ManagedDIDService & WalletAccessContext, RuntimeException, JwtIssuer] = {
    for {
      ed25519keyPair <- getEd25519SigningKeyPair(jwtIssuerDID, verificationRelationship)
    } yield {
      JwtIssuer(
        org.hyperledger.identus.pollux.vc.jwt.DID(jwtIssuerDID.toString),
        EdSigner(ed25519keyPair),
        Ed25519PublicKey.toJavaEd25519PublicKey(ed25519keyPair.publicKey.getEncoded)
      )
    }
  }

  def resolveToEd25519PublicKey(did: String): ZIO[JwtDidResolver, PresentationError, Ed25519PublicKey] = {
    for {
      didResolverService <- ZIO.service[JwtDidResolver]
      didResolutionResult <- didResolverService.resolve(did)
      publicKeyBase64 <- didResolutionResult match {
        case failed: DIDResolutionFailed =>
          ZIO.fail(
            PresentationError.UnexpectedError(
              s"DIDResolutionFailed for $did: ${failed.error.toString}"
            )
          )
        case succeeded: DIDResolutionSucceeded =>
          succeeded.didDocument.verificationMethod
            .find(vm => succeeded.didDocument.assertionMethod.contains(vm.id))
            .flatMap(_.publicKeyJwk.flatMap(_.x))
            .toRight(
              PresentationError.UnexpectedError(
                s"Did Document is missing the required publicKey: $did"
              )
            )
            .fold(ZIO.fail(_), ZIO.succeed(_))
      }
      ed25519PublicKey <- ZIO
        .fromTry {
          val decodedKey = Base64.getUrlDecoder.decode(publicKeyBase64)
          KmpEd25519KeyOps.publicKeyFromEncoded(decodedKey)
        }
        .mapError(t => PresentationError.UnexpectedError(t.getMessage))
    } yield ed25519PublicKey
  }

}
