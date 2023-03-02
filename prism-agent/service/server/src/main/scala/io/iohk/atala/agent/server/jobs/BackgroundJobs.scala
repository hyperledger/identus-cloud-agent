package io.iohk.atala.agent.server.jobs

import scala.jdk.CollectionConverters.*
import zio.*
import io.iohk.atala.pollux.core.service.CredentialService
import io.iohk.atala.pollux.core.model.IssueCredentialRecord
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import io.iohk.atala.pollux.core.service.CredentialService
import io.iohk.atala.pollux.vc.jwt.W3cCredentialPayload
import zio.Duration
import java.time.Instant
import java.time.Clock
import java.time.ZoneId
import io.iohk.atala.mercury._
import io.iohk.atala.mercury.model._
import io.iohk.atala.mercury.model.error._
import io.iohk.atala.mercury.protocol.issuecredential._
import io.iohk.atala.mercury.protocol.presentproof._
import io.iohk.atala.mercury.protocol.reportproblem.v2._
import io.iohk.atala.resolvers.DIDResolver
import io.iohk.atala.resolvers.UniversalDidResolver
import java.io.IOException
import io.iohk.atala.pollux.vc.jwt._
import io.iohk.atala.pollux.vc.jwt.W3CCredential
import io.iohk.atala.pollux.core.model._
import io.iohk.atala.pollux.core.service.PresentationService
import io.iohk.atala.pollux.core.model.error.PresentationError
import io.iohk.atala.pollux.core.model.error.PresentationError._
import io.iohk.atala.agent.server.http.model.{InvalidState, NotImplemented}
import org.didcommx.didcomm.DIDComm
import io.iohk.atala.agent.walletapi.model._
import io.iohk.atala.agent.walletapi.model.error._
import io.iohk.atala.agent.walletapi.model.error.DIDSecretStorageError.KeyNotFoundError
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.pollux.vc.jwt.ES256KSigner
import io.iohk.atala.castor.core.model.did._
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
import io.circe.syntax._
import io.iohk.atala.pollux.vc.jwt.JWT
import io.iohk.atala.pollux.vc.jwt.{DidResolver => JwtDidResolver}
import io.iohk.atala.agent.server.config.AppConfig
import io.circe.parser._
import zio.prelude.AssociativeBothOps
import zio.prelude.Validation
import zio.prelude.ZValidation._
import cats.syntax.all._
import io.iohk.atala.castor.core.service.DIDService
import java.util.UUID

object BackgroundJobs {

  val issueCredentialDidCommExchanges = {
    for {
      credentialService <- ZIO.service[CredentialService]
      config <- ZIO.service[AppConfig]
      records <- credentialService
        .getIssueCredentialRecordsByStates(
          IssueCredentialRecord.ProtocolState.OfferPending,
          IssueCredentialRecord.ProtocolState.RequestPending,
          IssueCredentialRecord.ProtocolState.RequestReceived,
          IssueCredentialRecord.ProtocolState.CredentialPending,
          IssueCredentialRecord.ProtocolState.CredentialGenerated
        )
        .mapError(err => Throwable(s"Error occurred while getting Issue Credential records: $err"))
      _ <- ZIO.foreachPar(records)(performExchange).withParallelism(config.pollux.issueBgJobProcessingParallelism)
    } yield ()
  }
  val presentProofExchanges = {
    for {
      presentationService <- ZIO.service[PresentationService]
      config <- ZIO.service[AppConfig]
      records <- presentationService
        .getPresentationRecordsByStates(
          PresentationRecord.ProtocolState.RequestPending,
          PresentationRecord.ProtocolState.PresentationPending,
          PresentationRecord.ProtocolState.PresentationGenerated,
          PresentationRecord.ProtocolState.PresentationReceived
        )
        .mapError(err => Throwable(s"Error occurred while getting Presentation records: $err"))
      _ <- ZIO
        .foreachPar(records)(performPresentation)
        .withParallelism(config.pollux.presentationBgJobProcessingParallelism)
    } yield ()
  }

  private[this] def performExchange(
      record: IssueCredentialRecord
  ): URIO[
    DidOps & DIDResolver & JwtDidResolver & HttpClient & CredentialService & DIDService & ManagedDIDService,
    Unit
  ] = {
    import IssueCredentialRecord._
    import IssueCredentialRecord.ProtocolState._
    import IssueCredentialRecord.PublicationState._
    val aux = for {
      _ <- ZIO.logDebug(s"Running action with records => $record")
      _ <- record match {
        // Offer should be sent from Issuer to Holder
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
              _,
              OfferPending,
              _,
              Some(offer),
              _,
              _,
              _,
              _,
              _,
              _,
              _,
            ) =>
          for {
            _ <- ZIO.log(s"IssueCredentialRecord: OfferPending (START)")
            didCommAgent <- buildDIDCommAgent(offer.from)
            resp <- MessagingService
              .send(offer.makeMessage)
              .provideSomeLayer(didCommAgent)
            credentialService <- ZIO.service[CredentialService]
            _ <- {
              if (resp.status >= 200 && resp.status < 300) credentialService.markOfferSent(id)
              else ZIO.logWarning(s"DIDComm sending error: [${resp.status}] - ${resp.bodyAsString}")
            }
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
              _,
              _,
              _,
            ) =>
          for {
            didCommAgent <- buildDIDCommAgent(request.from)
            resp <- MessagingService
              .send(request.makeMessage)
              .provideSomeLayer(didCommAgent)
            credentialService <- ZIO.service[CredentialService]
            _ <- {
              if (resp.status >= 200 && resp.status < 300) credentialService.markRequestSent(id)
              else ZIO.logWarning(s"DIDComm sending error: [${resp.status}] - ${resp.bodyAsString}")
            }
          } yield ()

        // 'automaticIssuance' is TRUE. Issuer automatically accepts the Request
        case IssueCredentialRecord(
              id,
              _,
              _,
              _,
              _,
              Role.Issuer,
              _,
              _,
              Some(true),
              _,
              RequestReceived,
              _,
              _,
              _,
              _,
              _,
              _,
              _,
              _,
            ) =>
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
              Some(issuingDID),
              _,
              _,
              _,
            ) =>
          // Generate the JWT Credential and store it in DB as an attacment to IssueCredentialData
          // Set ProtocolState to CredentialGenerated
          // Set PublicationState to PublicationPending
          for {
            credentialService <- ZIO.service[CredentialService]
            issuer <- createPrismDIDIssuer(issuingDID)
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
              credentials = Map("prims/jwt" -> signedJwtCredential.value.getBytes)
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
              _,
              _,
              _,
            ) =>
          for {
            didCommAgent <- buildDIDCommAgent(issue.from)
            resp <- MessagingService
              .send(issue.makeMessage)
              .provideSomeLayer(didCommAgent)
            credentialService <- ZIO.service[CredentialService]
            _ <- {
              if (resp.status >= 200 && resp.status < 300) credentialService.markCredentialSent(id)
              else ZIO.logWarning(s"DIDComm sending error: [${resp.status}] - ${resp.bodyAsString}")
            }
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
              _,
              _,
              _,
              _
            ) =>
          for {
            didCommAgent <- buildDIDCommAgent(issue.from)
            resp <- MessagingService.send(issue.makeMessage).provideSomeLayer(didCommAgent)
            credentialService <- ZIO.service[CredentialService]
            _ <- {
              if (resp.status >= 200 && resp.status < 300) credentialService.markCredentialSent(id)
              else ZIO.logWarning(s"DIDComm sending error: [${resp.status}] - ${resp.bodyAsString}")
            }
          } yield ()

        case IssueCredentialRecord(id, _, _, _, _, _, _, _, _, _, ProblemReportPending, _, _, _, _, _, _, _, _, _) =>
          ???
        case IssueCredentialRecord(id, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _) => ZIO.unit
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
  // - For now, we include the long form in the JWT credential to facilitate validation on client-side, but resolution should be used instead.
  // - Improve consistency in error handling (ATL-3210)
  private[this] def createPrismDIDIssuer(
      issuingDID: PrismDID,
      verificationRelationship: VerificationRelationship = VerificationRelationship.AssertionMethod,
      allowUnpublishedIssuingDID: Boolean = false
  ): ZIO[DIDService & ManagedDIDService, Throwable, Issuer] = {
    for {
      managedDIDService <- ZIO.service[ManagedDIDService]
      didService <- ZIO.service[DIDService]
      didState <- managedDIDService
        .getManagedDIDState(issuingDID.asCanonical)
        .mapError(e => RuntimeException(s"Error occured while getting did from wallet: ${e.toString}"))
        .someOrFail(RuntimeException(s"Issuer DID does not exist in the wallet: $issuingDID"))
        .flatMap {
          case s: ManagedDIDState.Published    => ZIO.succeed(s)
          case s if allowUnpublishedIssuingDID => ZIO.succeed(s)
          case _ => ZIO.fail(RuntimeException(s"Issuer DID must be published: $issuingDID"))
        }
      longFormPrismDID = PrismDID.buildLongFormFromOperation(didState.createOperation)
      // Automatically infer keyId to use by resolving DID and choose the corresponding VerificationRelationship
      issuingKeyId <- didService
        .resolveDID(issuingDID)
        .mapError(e => RuntimeException(s"Error occured while resolving Issuing DID during VC creation: ${e.toString}"))
        .someOrFail(RuntimeException(s"Issuing DID resolution result is not found"))
        .map { case (_, didData) => didData.publicKeys.find(_.purpose == verificationRelationship).map(_.id) }
        .someOrFail(
          RuntimeException(s"Issuing DID doesn't have a key in ${verificationRelationship.name} to use: $issuingDID")
        )
      ecKeyPair <- managedDIDService
        .javaKeyPairWithDID(issuingDID.asCanonical, issuingKeyId)
        .mapError(e => RuntimeException(s"Error occurred while getting issuer key-pair: ${e.toString}"))
        .someOrFail(
          RuntimeException(s"Issuer key-pair does not exist in the wallet: ${issuingDID.toString}#$issuingKeyId")
        )
      (privateKey, publicKey) = ecKeyPair
      issuer = Issuer(
        io.iohk.atala.pollux.vc.jwt.DID(longFormPrismDID.toString),
        ES256KSigner(privateKey),
        publicKey
      )
    } yield issuer
  }

  private[this] def createPrismDIDIssuerFromPresentationCredentials(
      presentationId: DidCommID,
      credentialsToUse: Seq[String]
  ) =
    for {
      credentialService <- ZIO.service[CredentialService]
      // Choose first credential from the list to detect the subject DID to be used in Presentation.
      // Holder binding check implies that any credential record can be chosen to detect the DID to use in VP.
      credentialRecordId <- ZIO
        .fromOption(credentialsToUse.headOption)
        .mapError(_ =>
          PresentationError.UnexpectedError(s"No credential found in the Presentation record: $presentationId")
        )
      credentialRecordUuid <- ZIO
        .attempt(DidCommID(credentialRecordId))
        .mapError(_ => PresentationError.UnexpectedError(s"$credentialRecordId is not a valid DidCommID"))
      vcSubjectId <- credentialService
        .getIssueCredentialRecord(credentialRecordUuid)
        .someOrFail(CredentialServiceError.RecordIdNotFound(credentialRecordUuid))
        .map(_.subjectId)
      proverDID <- ZIO
        .fromEither(PrismDID.fromString(vcSubjectId))
        .mapError(e =>
          PresentationError
            .UnexpectedError(
              s"One of the credential(s) subject is not a valid Prism DID: ${vcSubjectId}"
            )
        )
      prover <- createPrismDIDIssuer(
        proverDID,
        verificationRelationship = VerificationRelationship.Authentication,
        allowUnpublishedIssuingDID = true
      )
    } yield prover

  private[this] def performPresentation(
      record: PresentationRecord
  ): URIO[
    DidOps & DIDResolver & JwtDidResolver & HttpClient & PresentationService & CredentialService & DIDService &
      ManagedDIDService,
    Unit
  ] = {
    import io.iohk.atala.pollux.core.model.PresentationRecord.ProtocolState._

    val aux = for {
      _ <- ZIO.logDebug(s"Running action with records => $record")
      _ <- record match {
        // ##########################
        // ### PresentationRecord ###
        // ##########################
        case PresentationRecord(id, _, _, _, _, _, _, _, ProposalPending, _, _, _, _, _, _, _) =>
          ZIO.fail(NotImplemented)
        case PresentationRecord(id, _, _, _, _, _, _, _, ProposalSent, _, _, _, _, _, _, _) => ZIO.fail(NotImplemented)
        case PresentationRecord(id, _, _, _, _, _, _, _, ProposalReceived, _, _, _, _, _, _, _) =>
          ZIO.fail(NotImplemented)
        case PresentationRecord(id, _, _, _, _, _, _, _, ProposalRejected, _, _, _, _, _, _, _) =>
          ZIO.fail(NotImplemented)

        case PresentationRecord(id, _, _, _, _, _, _, _, RequestPending, oRecord, _, _, _, _, _, _) => // Verifier
          oRecord match
            case None => ZIO.fail(InvalidState("PresentationRecord 'RequestPending' with no Record"))
            case Some(record) =>
              for {
                _ <- ZIO.log(s"PresentationRecord: RequestPending (Send Massage)")
                didOps <- ZIO.service[DidOps]
                didCommAgent <- buildDIDCommAgent(record.from)
                resp <- MessagingService.send(record.makeMessage).provideSomeLayer(didCommAgent)
                service <- ZIO.service[PresentationService]
                _ <- {
                  if (resp.status >= 200 && resp.status < 300) service.markRequestPresentationSent(id)
                  else ZIO.logWarning(s"DIDComm sending error: [${resp.status}] - ${resp.bodyAsString}")
                }
              } yield ()

        case PresentationRecord(id, _, _, _, _, _, _, _, RequestSent, _, _, _, _, _, _, _) => // Verifier
          ZIO.logDebug("PresentationRecord: RequestSent") *> ZIO.unit
        case PresentationRecord(id, _, _, _, _, _, _, _, RequestReceived, _, _, _, _, _, _, _) => // Prover
          ZIO.logDebug("PresentationRecord: RequestReceived") *> ZIO.unit
        case PresentationRecord(id, _, _, _, _, _, _, _, RequestRejected, _, _, _, _, _, _, _) => // Prover
          ZIO.logDebug("PresentationRecord: RequestRejected") *> ZIO.unit
        case PresentationRecord(id, _, _, _, _, _, _, _, ProblemReportPending, _, _, _, _, _, _, _) =>
          ZIO.fail(NotImplemented)
        case PresentationRecord(id, _, _, _, _, _, _, _, ProblemReportSent, _, _, _, _, _, _, _) =>
          ZIO.fail(NotImplemented)
        case PresentationRecord(id, _, _, _, _, _, _, _, ProblemReportReceived, _, _, _, _, _, _, _) =>
          ZIO.fail(NotImplemented)
        case PresentationRecord(
              id,
              _,
              _,
              _,
              _,
              _,
              _,
              _,
              PresentationPending,
              oRequestPresentation,
              _,
              _,
              credentialsToUse,
              _,
              _,
              _
            ) => // Prover
          for {
            presentationService <- ZIO.service[PresentationService]
            prover <- createPrismDIDIssuerFromPresentationCredentials(id, credentialsToUse.getOrElse(Nil))
            presentationPayload <- presentationService.createPresentationPayloadFromRecord(
              id,
              prover,
              Instant.now()
            )
            signedJwtPresentation = JwtPresentation.encodeJwt(presentationPayload.toJwtPresentationPayload, prover)
            presentation <- oRequestPresentation match
              case None => ZIO.fail(InvalidState("PresentationRecord 'RequestPending' with no Record"))
              case Some(requestPresentation) => { // TODO create build method in mercury for Presentation
                ZIO.succeed(
                  Presentation(
                    body = Presentation.Body(
                      goal_code = requestPresentation.body.goal_code,
                      comment = requestPresentation.body.comment
                    ),
                    attachments = Seq(
                      AttachmentDescriptor
                        .buildBase64Attachment(
                          payload = signedJwtPresentation.value.getBytes(),
                          mediaType = Some("prism/jwt")
                        )
                    ),
                    thid = requestPresentation.thid.orElse(Some(requestPresentation.id)),
                    from = requestPresentation.to,
                    to = requestPresentation.from
                  )
                )
              }
            _ <- presentationService.markPresentationGenerated(id, presentation)

          } yield ()
        case PresentationRecord(id, _, _, _, _, _, _, _, PresentationGenerated, _, _, presentation, _, _, _, _) =>
          ZIO.logDebug("PresentationRecord: PresentationGenerated") *> ZIO.unit
          presentation match
            case None => ZIO.fail(InvalidState("PresentationRecord in 'PresentationPending' with no Presentation"))
            case Some(p) =>
              for {
                _ <- ZIO.log(s"PresentationRecord: PresentationPending (Send Message)")
                didCommAgent <- buildDIDCommAgent(p.from)
                resp <- MessagingService
                  .send(p.makeMessage)
                  .provideSomeLayer(didCommAgent)
                service <- ZIO.service[PresentationService]
                _ <- {
                  if (resp.status >= 200 && resp.status < 300) service.markPresentationSent(id)
                  else ZIO.logWarning(s"DIDComm sending error: [${resp.status}] - ${resp.bodyAsString}")
                }
              } yield ()
        case PresentationRecord(id, _, _, _, _, _, _, _, PresentationSent, _, _, _, _, _, _, _) =>
          ZIO.logDebug("PresentationRecord: PresentationSent") *> ZIO.unit
        case PresentationRecord(
              id,
              _,
              _,
              _,
              _,
              _,
              _,
              _,
              PresentationReceived,
              mayBeRequestPresentation,
              _,
              presentation,
              _,
              _,
              _,
              _
            ) => // Verifier
          ZIO.logDebug("PresentationRecord: PresentationReceived") *> ZIO.unit
          val clock = java.time.Clock.system(ZoneId.systemDefault)
          presentation match
            case None => ZIO.fail(InvalidState("PresentationRecord in 'PresentationReceived' with no Presentation"))
            case Some(p) =>
              for {
                _ <- ZIO.log(s"PresentationRecord: 'PresentationReceived' ")
                didResolverService <- ZIO.service[JwtDidResolver]
                credentialsValidationResult <- p.attachments.head.data match {
                  case Base64(data) =>
                    val base64Decoded = new String(java.util.Base64.getDecoder().decode(data))
                    val maybePresentationOptions
                        : Either[PresentationError, Option[io.iohk.atala.pollux.core.model.presentation.Options]] =
                      mayBeRequestPresentation
                        .map(
                          _.attachments.headOption
                            .map(attachment =>
                              decode[io.iohk.atala.mercury.model.JsonData](attachment.data.asJson.noSpaces)
                                .flatMap(data =>
                                  io.iohk.atala.pollux.core.model.presentation.PresentationAttachment.given_Decoder_PresentationAttachment
                                    .decodeJson(data.json.asJson)
                                    .map(_.options)
                                    .leftMap(err =>
                                      PresentationDecodingError(
                                        new Throwable(s"PresentationAttachment decoding error: $err")
                                      )
                                    )
                                )
                                .leftMap(err =>
                                  PresentationDecodingError(new Throwable(s"JsonData decoding error: $err"))
                                )
                            )
                            .getOrElse(Right(None))
                        )
                        .getOrElse(Left(UnexpectedError("RequestPresentation NotFound")))
                    for {
                      _ <- ZIO.fromEither(maybePresentationOptions.map { maybeOptions =>
                        maybeOptions match
                          case Some(options) =>
                            JwtPresentation.validatePresentation(JWT(base64Decoded), options.domain, options.challenge)
                          case _ => Validation.unit
                      })
                      // https://www.w3.org/TR/vc-data-model/#proofs-signatures-0
                      // A proof is typically attached to a verifiable presentation for authentication purposes
                      // and to a verifiable credential as a method of assertion.
                      result <- JwtPresentation.verify(
                        JWT(base64Decoded),
                        JwtPresentation.PresentationVerificationOptions(
                          maybeProofPurpose = Some(VerificationRelationship.Authentication),
                          verifySignature = true,
                          verifyDates = false,
                          leeway = Duration.Zero,
                          maybeCredentialOptions = Some(
                            CredentialVerification.CredentialVerificationOptions(
                              verifySignature = true,
                              verifyDates = false,
                              leeway = Duration.Zero,
                              maybeProofPurpose = Some(VerificationRelationship.AssertionMethod)
                            )
                          )
                        )
                      )(didResolverService)(clock)
                    } yield result

                  case any => ZIO.fail(NotImplemented)
                }
                _ <- ZIO.log(s"CredentialsValidationResult: $credentialsValidationResult")
                service <- ZIO.service[PresentationService]
                _ <- credentialsValidationResult match {
                  case Success(log, value) => service.markPresentationVerified(id)
                  case Failure(log, error) => {
                    for {
                      _ <- service.markPresentationVerificationFailed(id)
                      didCommAgent <- buildDIDCommAgent(p.from)
                      reportproblem = ReportProblem.build(
                        fromDID = p.to,
                        toDID = p.from,
                        pthid = p.thid.getOrElse(p.id),
                        code = ProblemCode("e.p.presentation-verification-failed"),
                        comment = Some(error.mkString)
                      )
                      resp <- MessagingService
                        .send(reportproblem.toMessage)
                        .provideSomeLayer(didCommAgent)
                      _ <- ZIO.log(s"CredentialsValidationResult: $error")
                    } yield ()
                  }
                }

              } yield ()
        case PresentationRecord(id, _, _, _, _, _, _, _, PresentationVerificationFailed, _, _, _, _) =>
          ZIO.logDebug("PresentationRecord: PresentationVerificationFailed") *> ZIO.unit
        case PresentationRecord(id, _, _, _, _, _, _, _, PresentationAccepted, _, _, _, _, _, _, _) =>
        case PresentationRecord(id, _, _, _, _, _, _, _, PresentationVerified, _, _, _, _, _, _, _) =>
          ZIO.logDebug("PresentationRecord: PresentationVerified") *> ZIO.unit
        case PresentationRecord(id, _, _, _, _, _, _, _, PresentationAccepted, _, _, _, _) =>
          ZIO.logDebug("PresentationRecord: PresentationVerifiedAccepted") *> ZIO.unit
        case PresentationRecord(id, _, _, _, _, _, _, _, PresentationRejected, _, _, _, _, _, _, _) =>
          ZIO.logDebug("PresentationRecord: PresentationRejected") *> ZIO.unit
      }
    } yield ()

    aux
      .catchAll {
        case ex: MercuryException =>
          ZIO.logErrorCause(s"DIDComm communication error processing record: ${record.id}", Cause.fail(ex))
        case ex: CredentialServiceError =>
          ZIO.logErrorCause(s"Credential service error processing record: ${record.id} ", Cause.fail(ex))
        case ex: PresentationError =>
          ZIO.logErrorCause(s"Presentation service error processing record: ${record.id} ", Cause.fail(ex))
        case ex: DIDSecretStorageError =>
          ZIO.logErrorCause(s"DID secret storage error processing record: ${record.id} ", Cause.fail(ex))
      }
      .catchAllDefect { case throwable =>
        ZIO.logErrorCause(s"Proof Presentation protocol defect processing record: ${record.id}", Cause.fail(throwable))
      }
  }

  private[this] def buildDIDCommAgent(
      myDid: DidId
  ): ZIO[ManagedDIDService, KeyNotFoundError, ZLayer[Any, Nothing, DidAgent]] = {
    for {
      managedDidService <- ZIO.service[ManagedDIDService]
      peerDID <- managedDidService.getPeerDID(myDid)
      agent = AgentPeerService.makeLayer(peerDID)
    } yield agent
  }

  val publishCredentialsToDlt = {
    for {
      credentialService <- ZIO.service[CredentialService]
      _ <- performPublishCredentialsToDlt(credentialService)
    } yield ()

  }

  private[this] def performPublishCredentialsToDlt(credentialService: CredentialService) = {
    val res: ZIO[Any, CredentialServiceError, Unit] = for {
      records <- credentialService.getIssueCredentialRecordsByStates(
        IssueCredentialRecord.ProtocolState.CredentialPending
      )
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

  val syncDIDPublicationStateFromDlt =
    for {
      managedDidService <- ZIO.service[ManagedDIDService]
      _ <- managedDidService.syncManagedDIDState
      _ <- managedDidService.syncUnconfirmedUpdateOperations
    } yield ()

}
