package io.iohk.atala.agent.server.jobs

import scala.jdk.CollectionConverters.*
import zio.*
import io.iohk.atala.pollux.core.service.CredentialService
import io.iohk.atala.pollux.core.model.IssueCredentialRecord
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import io.iohk.atala.pollux.core.service.CredentialService
import io.iohk.atala.pollux.vc.jwt.W3cCredentialPayload
import zio.*

import java.time.Instant
import io.iohk.atala.mercury.DidComm
import io.iohk.atala.mercury.MediaTypes
import io.iohk.atala.mercury.MessagingService
import io.iohk.atala.mercury.HttpClient
import io.iohk.atala.mercury.model.*
import io.iohk.atala.mercury.model.error.*
import io.iohk.atala.mercury.protocol.issuecredential.*
import io.iohk.atala.mercury.protocol.presentproof.RequestPresentation
import io.iohk.atala.resolvers.DIDResolver
import io.iohk.atala.resolvers.UniversalDidResolver

import java.io.IOException
import zhttp.service.*
import zhttp.http.*
import io.iohk.atala.pollux.vc.jwt.W3CCredential
import io.iohk.atala.pollux.core.model.PresentationRecord
import io.iohk.atala.pollux.core.service.PresentationService
import io.iohk.atala.pollux.core.model.error.PresentationError
import io.iohk.atala.agent.server.http.model.{InvalidState, NotImplemented}
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.mercury.AgentServiceAny
import org.didcommx.didcomm.DIDComm
import io.iohk.atala.agent.walletapi.model.error.DIDSecretStorageError
import io.iohk.atala.agent.walletapi.model.{DIDPublicKeyTemplate, ManagedDIDTemplate}
import io.iohk.atala.agent.walletapi.sql.JdbcDIDSecretStorage
import io.iohk.atala.pollux.vc.jwt.ES256KSigner
import io.iohk.atala.castor.core.model.did.{EllipticCurve, VerificationRelationship}

import java.security.KeyFactory
import java.security.spec.EncodedKeySpec
import java.security.spec.ECPrivateKeySpec
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import org.bouncycastle.jce.ECNamedCurveTable
import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.Sign
import io.iohk.atala.pollux.vc.jwt.Issuer

import java.security.spec.ECPublicKeySpec
import java.security.spec.ECPoint
import org.bouncycastle.jce.provider.BouncyCastleProvider
import io.circe.Json
import io.circe.syntax.*
import io.iohk.atala.agent.walletapi.storage.DIDSecretStorage
import io.iohk.atala.agent.walletapi.model.error.CreateManagedDIDError

object BackgroundJobs {

  val didCommExchanges = {
    for {
      credentialService <- ZIO.service[CredentialService]
      records <- credentialService
        .getIssueCredentialRecords()
        .mapError(err => Throwable(s"Error occured while getting issue credential records: $err"))
      _ <- ZIO.foreach(records)(performExchange)
    } yield ()
  }
  val presentProofExchanges = {
    for {
      presentationService <- ZIO.service[PresentationService]
      records <- presentationService
        .getPresentationRecords()
        .mapError(err => Throwable(s"Error occured while getting issue credential records: $err"))
      _ <- ZIO.foreach(records)(performPresentation)
    } yield ()
  }

  private[this] def performExchange(
      record: IssueCredentialRecord
  ): URIO[DIDResolver & HttpClient & CredentialService & ManagedDIDService & DIDSecretStorage, Unit] = {
    import IssueCredentialRecord._
    import IssueCredentialRecord.ProtocolState._
    import IssueCredentialRecord.PublicationState._
    val aux = for {
      _ <- ZIO.logDebug(s"Running action with records => $record")
      _ <- record match {
        // Offer should be sent from Issuer to Holder
        case IssueCredentialRecord(id, _, _, _, _, Role.Issuer, _, _, _, _, OfferPending, _, Some(offer), _, _, _) =>
          for {
            _ <- ZIO.log(s"IssueCredentialRecord: OfferPending (START)")
            didCommAgent <- buildDIDCommAgent(offer.from)
            _ <- MessagingService.send(offer.makeMessage).provideSomeLayer(didCommAgent)
            credentialService <- ZIO.service[CredentialService]
            _ <- credentialService.markOfferSent(id)
          } yield ()

        // Request should be sent from Holder to Issuer
        case IssueCredentialRecord(
              id,
              _,
              _,
              _,
              _,
              Role.Holder,
              _,
              _,
              _,
              _,
              RequestPending,
              _,
              _,
              Some(request),
              _,
              _,
            ) =>
          for {
            didCommAgent <- buildDIDCommAgent(request.from)
            _ <- MessagingService.send(request.makeMessage).provideSomeLayer(didCommAgent)
            credentialService <- ZIO.service[CredentialService]
            _ <- credentialService.markRequestSent(id)
          } yield ()

        // 'automaticIssuance' is TRUE. Issuer automatically accepts the Request
        case IssueCredentialRecord(id, _, _, _, _, Role.Issuer, _, _, Some(true), _, RequestReceived, _, _, _, _, _) =>
          for {
            credentialService <- ZIO.service[CredentialService]
            _ <- credentialService.acceptCredentialRequest(id)
          } yield ()

        // Credential is pending, can be generated by Issuer and optionally published on-chain
        case IssueCredentialRecord(
              id,
              _,
              _,
              _,
              _,
              Role.Issuer,
              _,
              _,
              _,
              Some(awaitConfirmation),
              CredentialPending,
              _,
              _,
              _,
              Some(issue),
              _,
            ) =>
          // Generate the JWT Credential and store it in DB as an attacment to IssueCredentialData
          // Set ProtocolState to CredentialGenerated
          // Set PublicationState to PublicationPending
          for {
            credentialService <- ZIO.service[CredentialService]
            // issuer = credentialService.createIssuer
            issuer <- createPrismDIDIssuer()
            w3Credential <- credentialService.createCredentialPayloadFromRecord(
              record,
              issuer,
              Instant.now()
            )
            signedJwtCredential = W3CCredential.toEncodedJwt(w3Credential, issuer)
            issueCredential = IssueCredential.build(
              fromDID = issue.from,
              toDID = issue.to,
              thid = issue.thid,
              credentials = Map("prims/jwt" -> signedJwtCredential.value)
            )
            _ <- credentialService.markCredentialGenerated(id, issueCredential)

          } yield ()

        // Credential has been generated and can be sent directly to the Holder
        case IssueCredentialRecord(
              id,
              _,
              _,
              _,
              _,
              Role.Issuer,
              _,
              _,
              _,
              Some(false),
              CredentialGenerated,
              None,
              _,
              _,
              Some(issue),
              _,
            ) =>
          for {
            didCommAgent <- buildDIDCommAgent(issue.from)
            _ <- MessagingService.send(issue.makeMessage).provideSomeLayer(didCommAgent)
            credentialService <- ZIO.service[CredentialService]
            _ <- credentialService.markCredentialSent(id)
          } yield ()

        // Credential has been generated, published, and can now be sent to the Holder
        case IssueCredentialRecord(
              id,
              _,
              _,
              _,
              _,
              Role.Issuer,
              _,
              _,
              _,
              Some(true),
              CredentialGenerated,
              Some(Published),
              _,
              _,
              Some(issue),
              _,
            ) =>
          for {
            didCommAgent <- buildDIDCommAgent(issue.from)
            _ <- MessagingService.send(issue.makeMessage).provideSomeLayer(didCommAgent)
            credentialService <- ZIO.service[CredentialService]
            _ <- credentialService.markCredentialSent(id)
          } yield ()

        case IssueCredentialRecord(id, _, _, _, _, _, _, _, _, _, ProblemReportPending, _, _, _, _, _) => ???
        case IssueCredentialRecord(id, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)                    => ZIO.unit
      }
    } yield ()

    aux
      .catchAll {
        case ex: MercuryException =>
          ZIO.logErrorCause(s"DIDComm communication error processing record: ${record.id}", Cause.fail(ex))
        case ex: CredentialServiceError =>
          ZIO.logErrorCause(s"Credential service error processing record: ${record.id} ", Cause.fail(ex))
        case ex: DIDSecretStorageError =>
          ZIO.logErrorCause(s"DID secret storage error processing record: ${record.id} ", Cause.fail(ex))
      }
      .catchAllDefect { case throwable =>
        ZIO.logErrorCause(s"Issue Credential protocol defect processing record: ${record.id}", Cause.fail(throwable))
      }
  }

  // TODO: Improvements needed here:
  // - A single PrismDID genrated at agent startup should be used.
  // - For now, we include the long form in the JWT credential to facilitate validation on client-side, but resolution should be used instead.
  // - There should be a way to retrieve the 'default' PrismDID from ManagedDIDService (use of an alias in DB record?)
  // - Simplify convertion of ECKeyPair to JDK security classes
  // - ECPrivateKey should probably remain 'private' and signing operation occur in ManagedDIDService
  private[this] def createPrismDIDIssuer(): ZIO[ManagedDIDService & DIDSecretStorage, Throwable, Issuer] = {
    val ISSUING_KEY_ID = "issuing0"
    val issuerDidTemplate = ManagedDIDTemplate(
      Seq(DIDPublicKeyTemplate(ISSUING_KEY_ID, VerificationRelationship.AssertionMethod)),
      Nil
    )
    for {
      managedDIDService <- ZIO.service[ManagedDIDService]
      longFormPrismDID <- managedDIDService.createAndStoreDID(issuerDidTemplate)
      didSecretStorage <- ZIO.service[DIDSecretStorage]
      maybeECKeyPair <- didSecretStorage.getKey(longFormPrismDID.asCanonical, ISSUING_KEY_ID)
      _ <- ZIO.logInfo(s"ECKeyPair => $maybeECKeyPair")
      maybeIssuer <- ZIO.succeed(maybeECKeyPair.map(ecKeyPair => {
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
        Issuer(io.iohk.atala.pollux.vc.jwt.DID(longFormPrismDID.toString), ES256KSigner(privateKey), publicKey)
      }))
      issuer = maybeIssuer.get
    } yield issuer
  }

  private[this] def performPresentation(
      record: PresentationRecord
  ): URIO[DIDResolver & HttpClient & PresentationService & ManagedDIDService, Unit] = {
    import io.iohk.atala.pollux.core.model.PresentationRecord.ProtocolState._

    val aux = for {
      _ <- ZIO.logDebug(s"Running action with records => $record")
      _ <- record match {
        // ##########################
        // ### PresentationRecord ###
        // ##########################
        case PresentationRecord(id, _, _, _, _, _, _, _, ProposalPending, _, _, _)  => ZIO.fail(NotImplemented)
        case PresentationRecord(id, _, _, _, _, _, _, _, ProposalSent, _, _, _)     => ZIO.fail(NotImplemented)
        case PresentationRecord(id, _, _, _, _, _, _, _, ProposalReceived, _, _, _) => ZIO.fail(NotImplemented)
        case PresentationRecord(id, _, _, _, _, _, _, _, ProposalRejected, _, _, _) => ZIO.fail(NotImplemented)

        case PresentationRecord(id, _, _, _, _, _, _, _, RequestPending, oRecord, _, _) => // Verifier
          oRecord match
            case None => ZIO.fail(InvalidState("PresentationRecord 'RequestPending' with no Record"))
            case Some(record) =>
              for {
                _ <- ZIO.log(s"PresentationRecord: RequestPending (Send Massage)")
                didCommAgent <- buildDIDCommAgent(record.from)
                _ <- MessagingService.send(record.makeMessage).provideSomeLayer(didCommAgent)
                service <- ZIO.service[PresentationService]
                _ <- service.markRequestPresentationSent(id)
              } yield ()

        case PresentationRecord(id, _, _, _, _, _, _, _, RequestSent, _, _, _) => // Verifier
          ZIO.logDebug("PresentationRecord: RequestSent") *> ZIO.unit
        case PresentationRecord(id, _, _, _, _, _, _, _, RequestReceived, _, _, _) => // Prover
          ZIO.logDebug("PresentationRecord: RequestReceived") *> ZIO.unit
        case PresentationRecord(id, _, _, _, _, _, _, _, RequestRejected, _, _, _) => // Prover
          ZIO.logDebug("PresentationRecord: RequestRejected") *> ZIO.unit
        case PresentationRecord(id, _, _, _, _, _, _, _, ProblemReportPending, _, _, _)  => ZIO.fail(NotImplemented)
        case PresentationRecord(id, _, _, _, _, _, _, _, ProblemReportSent, _, _, _)     => ZIO.fail(NotImplemented)
        case PresentationRecord(id, _, _, _, _, _, _, _, ProblemReportReceived, _, _, _) => ZIO.fail(NotImplemented)
        case PresentationRecord(id, _, _, _, _, _, _, _, PresentationPending, _, _, presentation) => // Prover
          presentation match
            case None => ZIO.fail(InvalidState("PresentationRecord in 'PresentationPending' with no Presentation"))
            case Some(p) =>
              for {
                _ <- ZIO.log(s"PresentationRecord: PresentationPending (Send Massage)")
                didCommAgent <- buildDIDCommAgent(p.from)
                _ <- MessagingService.send(p.makeMessage).provideSomeLayer(didCommAgent)
                service <- ZIO.service[PresentationService]
                _ <- service.markPresentationSent(id)
              } yield ()
        case PresentationRecord(id, _, _, _, _, _, _, _, PresentationSent, _, _, _) =>
          ZIO.logDebug("PresentationRecord: PresentationSent") *> ZIO.unit
        case PresentationRecord(id, _, _, _, _, _, _, _, PresentationReceived, _, _, _) =>
          ZIO.logDebug("PresentationRecord: PresentationReceived") *> ZIO.unit
          for {
            _ <- ZIO.log(s"PresentationRecord: PresentationPending (Send Massage)")
            // TODO Verify  https://input-output.atlassian.net/browse/ATL-2702
            service <- ZIO.service[PresentationService]
            _ <- service.markPresentationVerified(id)
          } yield ()
        // TODO move the state to PresentationVerified
        case PresentationRecord(id, _, _, _, _, _, _, _, PresentationVerified, _, _, _) =>
          ZIO.logDebug("PresentationRecord: PresentationVerified") *> ZIO.unit
        case PresentationRecord(id, _, _, _, _, _, _, _, PresentationAccepted, _, _, _) =>
          ZIO.logDebug("PresentationRecord: PresentationVerifiedAccepted") *> ZIO.unit
        case PresentationRecord(id, _, _, _, _, _, _, _, PresentationRejected, _, _, _) =>
          ZIO.logDebug("PresentationRecord: PresentationRejected") *> ZIO.unit
      }
    } yield ()

    aux
      .catchAll {
        case ex: MercuryException =>
          ZIO.logErrorCause(s"DIDComm communication error processing record: ${record.id}", Cause.fail(ex))
        case ex: PresentationError =>
          ZIO.logErrorCause(s"Presentation service error processing record: ${record.id} ", Cause.fail(ex))
        case ex: DIDSecretStorageError =>
          ZIO.logErrorCause(s"DID secret storage error processing record: ${record.id} ", Cause.fail(ex))
      }
      .catchAllDefect { case throwable =>
        ZIO.logErrorCause(s"Proof Presentation protocol defect processing record: ${record.id}", Cause.fail(throwable))
      }
  }

  private[this] def buildDIDCommAgent(myDid: DidId) = {
    for {
      managedDidService <- ZIO.service[ManagedDIDService]
      peerDID <- managedDidService.getPeerDID(myDid)
      didCommAgent = ZLayer.succeed(
        AgentServiceAny(
          new DIDComm(UniversalDidResolver, peerDID.getSecretResolverInMemory),
          peerDID.did
        )
      )
    } yield didCommAgent
  }

  val publishCredentialsToDlt = {
    for {
      credentialService <- ZIO.service[CredentialService]
      _ <- performPublishCredentialsToDlt(credentialService)
    } yield ()

  }

  private[this] def performPublishCredentialsToDlt(credentialService: CredentialService) = {
    val res: ZIO[Any, CredentialServiceError, Unit] = for {
      records <- credentialService.getCredentialRecordsByState(IssueCredentialRecord.ProtocolState.CredentialPending)
      // NOTE: the line below is a potentially slow operation, because <createCredentialPayloadFromRecord> makes a database SELECT call,
      // so calling this function n times will make n database SELECT calls, while it can be optimized to get
      // all data in one query, this function here has to be refactored as well. Consider doing this if this job is too slow
      credentials <- ZIO.foreach(records) { record =>
        credentialService.createCredentialPayloadFromRecord(record, credentialService.createIssuer, Instant.now())
      }
      // FIXME: issuer here should come from castor not from credential service, this needs to be done before going to prod
      publishedBatchData <- credentialService.publishCredentialBatch(credentials, credentialService.createIssuer)
      _ <- credentialService.markCredentialRecordsAsPublishQueued(publishedBatchData.credentialsAnsProofs)
      // publishedBatchData gives back irisOperationId, which should be persisted to track the status
    } yield ()

    ZIO.unit
  }

}
