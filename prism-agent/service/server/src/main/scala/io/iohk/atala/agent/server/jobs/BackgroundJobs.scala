package io.iohk.atala.agent.server.jobs

import cats.syntax.all.*
import io.circe.parser.*
import io.circe.syntax.*
import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.server.jobs.BackgroundJobError.{
  ErrorResponseReceivedFromPeerAgent,
  InvalidState,
  NotImplemented
}
import io.iohk.atala.agent.walletapi.model.*
import io.iohk.atala.agent.walletapi.model.error.*
import io.iohk.atala.agent.walletapi.model.error.DIDSecretStorageError.KeyNotFoundError
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.castor.core.model.did.*
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.mercury.*
import io.iohk.atala.mercury.model.*
import io.iohk.atala.mercury.protocol.issuecredential.*
import io.iohk.atala.mercury.protocol.presentproof.*
import io.iohk.atala.mercury.protocol.reportproblem.v2.*
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.error.PresentationError.*
import io.iohk.atala.pollux.core.model.error.{CredentialServiceError, PresentationError}
import io.iohk.atala.pollux.core.service.{CredentialService, PresentationService}
import io.iohk.atala.pollux.vc.jwt.{
  ES256KSigner,
  JWT,
  JwtPresentation,
  W3CCredential,
  DidResolver as JwtDidResolver,
  Issuer as JwtIssuer
}
import zio.*
import zio.prelude.ZValidation.*
import zio.prelude.Validation
import java.time.{Clock, Instant, ZoneId}

object BackgroundJobs {

  val issueCredentialDidCommExchanges = {
    for {
      credentialService <- ZIO.service[CredentialService]
      config <- ZIO.service[AppConfig]
      records <- credentialService
        .getIssueCredentialRecordsByStates(
          ignoreWithZeroRetries = true,
          limit = config.pollux.issueBgJobRecordsLimit,
          IssueCredentialRecord.ProtocolState.OfferPending,
          IssueCredentialRecord.ProtocolState.RequestPending,
          IssueCredentialRecord.ProtocolState.RequestGenerated,
          IssueCredentialRecord.ProtocolState.RequestReceived,
          IssueCredentialRecord.ProtocolState.CredentialPending,
          IssueCredentialRecord.ProtocolState.CredentialGenerated
        )
        .mapError(err => Throwable(s"Error occurred while getting Issue Credential records: $err"))
      _ <- ZIO
        .foreachPar(records)(performIssueCredentialExchange)
        .withParallelism(config.pollux.issueBgJobProcessingParallelism)
    } yield ()
  }
  val presentProofExchanges = {
    for {
      presentationService <- ZIO.service[PresentationService]
      config <- ZIO.service[AppConfig]
      records <- presentationService
        .getPresentationRecordsByStates(
          ignoreWithZeroRetries = true,
          limit = config.pollux.presentationBgJobRecordsLimit,
          PresentationRecord.ProtocolState.RequestPending,
          PresentationRecord.ProtocolState.PresentationPending,
          PresentationRecord.ProtocolState.PresentationGenerated,
          PresentationRecord.ProtocolState.PresentationReceived
        )
        .mapError(err => Throwable(s"Error occurred while getting Presentation records: $err"))
      _ <- ZIO
        .foreachPar(records)(performPresentProofExchange)
        .withParallelism(config.pollux.presentationBgJobProcessingParallelism)
    } yield ()
  }

  private[this] def performIssueCredentialExchange(record: IssueCredentialRecord) = {
    import IssueCredentialRecord.*
    import IssueCredentialRecord.ProtocolState.*
    import IssueCredentialRecord.PublicationState.*
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
              else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp))
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
              Some(subjectId),
              _,
              _,
              _,
              RequestPending,
              _,
              _,
              None,
              _,
              _,
              _,
              _,
              _,
              _
            ) =>
          for {
            credentialService <- ZIO.service[CredentialService]
            subjectDID <- ZIO
              .fromEither(PrismDID.fromString(subjectId))
              .mapError(_ => CredentialServiceError.UnsupportedDidFormat(subjectId))
            longFormPrismDID <- getLongForm(subjectDID, true)
            jwtIssuer <- createJwtIssuer(longFormPrismDID, VerificationRelationship.Authentication)
            presentationPayload <- credentialService.createPresentationPayload(id, jwtIssuer)
            signedPayload = JwtPresentation.encodeJwt(presentationPayload.toJwtPresentationPayload, jwtIssuer)
            _ <- credentialService.generateCredentialRequest(id, signedPayload)
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
              RequestGenerated,
              _,
              _,
              Some(request),
              _,
              _,
              _,
              _,
              _,
              _
            ) =>
          for {
            didCommAgent <- buildDIDCommAgent(request.from)
            resp <- MessagingService
              .send(request.makeMessage)
              .provideSomeLayer(didCommAgent)
            credentialService <- ZIO.service[CredentialService]
            _ <- {
              if (resp.status >= 200 && resp.status < 300) credentialService.markRequestSent(id)
              else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp))
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
              Some(issuerDID),
              _,
              _,
              _,
            ) =>
          // Generate the JWT Credential and store it in DB as an attacment to IssueCredentialData
          // Set ProtocolState to CredentialGenerated
          // Set PublicationState to PublicationPending
          for {
            credentialService <- ZIO.service[CredentialService]
            longFormPrismDID <- getLongForm(issuerDID, true)
            jwtIssuer <- createJwtIssuer(longFormPrismDID, VerificationRelationship.AssertionMethod)
            w3Credential <- credentialService.createCredentialPayloadFromRecord(
              record,
              jwtIssuer,
              Instant.now()
            )
            signedJwtCredential = W3CCredential.toEncodedJwt(w3Credential, jwtIssuer)
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
              else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp))
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
              else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp))
            }
          } yield ()

        case IssueCredentialRecord(id, _, _, _, _, _, _, _, _, _, ProblemReportPending, _, _, _, _, _, _, _, _, _) =>
          ???
        case IssueCredentialRecord(id, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _) => ZIO.unit
      }
    } yield ()

    aux
      .tapError(e =>
        for {
          credentialService <- ZIO.service[CredentialService]
          _ <- credentialService
            .reportProcessingFailure(record.id, Some(e.toString))
            .tapError(err =>
              ZIO.logErrorCause(
                s"Issue Credential - failed to report processing failure: ${record.id}",
                Cause.fail(err)
              )
            )
        } yield ()
      )
      .catchAll(e => ZIO.logErrorCause(s"Issue Credential - Error processing record: ${record.id} ", Cause.fail(e)))
      .catchAllDefect(d =>
        ZIO.logErrorCause(s"Issue Credential - Defect processing record: ${record.id}", Cause.fail(d))
      )

  }

  private[this] def getLongForm(
      did: PrismDID,
      allowUnpublishedIssuingDID: Boolean = false
  ): ZIO[ManagedDIDService, Throwable, LongFormPrismDID] = {
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

  // TODO: Improvements needed here:
  // - Improve consistency in error handling (ATL-3210)
  private[this] def createJwtIssuer(
      jwtIssuerDID: PrismDID,
      verificationRelationship: VerificationRelationship
  ): ZIO[DIDService & ManagedDIDService, Throwable, JwtIssuer] = {
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
        io.iohk.atala.pollux.vc.jwt.DID(jwtIssuerDID.toString),
        ES256KSigner(privateKey),
        publicKey
      )
    } yield jwtIssuer
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
        .someOrFail(
          CredentialServiceError.UnexpectedError(s"VC SubjectId not found in credential record: $credentialRecordUuid")
        )
      proverDID <- ZIO
        .fromEither(PrismDID.fromString(vcSubjectId))
        .mapError(e =>
          PresentationError
            .UnexpectedError(
              s"One of the credential(s) subject is not a valid Prism DID: ${vcSubjectId}"
            )
        )
      longFormPrismDID <- getLongForm(proverDID, true)
      jwtIssuer <- createJwtIssuer(longFormPrismDID, VerificationRelationship.Authentication)
    } yield jwtIssuer

  private[this] def performPresentProofExchange(record: PresentationRecord) = {
    import io.iohk.atala.pollux.core.model.PresentationRecord.ProtocolState.*

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
                  else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp))
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
            signedJwtPresentation = JwtPresentation.toEncodedJwt(
              presentationPayload.toW3CPresentationPayload,
              prover
            )
            // signedJwtPresentation = JwtPresentation.toEncodedJwt(w3cPresentationPayload, prover)
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
                  else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp))
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
                      verificationConfig <- ZIO.service[AppConfig].map(_.agent.verification)
                      _ <- ZIO.log(s"VerificationConfig: ${verificationConfig}")

                      // https://www.w3.org/TR/vc-data-model/#proofs-signatures-0
                      // A proof is typically attached to a verifiable presentation for authentication purposes
                      // and to a verifiable credential as a method of assertion.
                      result <- JwtPresentation.verify(
                        JWT(base64Decoded),
                        verificationConfig.toPresentationVerificationOptions()
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
        case PresentationRecord(id, _, _, _, _, _, _, _, PresentationVerificationFailed, _, _, _, _, _, _, _) =>
          ZIO.logDebug("PresentationRecord: PresentationVerificationFailed") *> ZIO.unit
        case PresentationRecord(id, _, _, _, _, _, _, _, PresentationAccepted, _, _, _, _, _, _, _) =>
          ZIO.logDebug("PresentationRecord: PresentationVerifiedAccepted") *> ZIO.unit
        case PresentationRecord(id, _, _, _, _, _, _, _, PresentationVerified, _, _, _, _, _, _, _) =>
          ZIO.logDebug("PresentationRecord: PresentationVerified") *> ZIO.unit
        case PresentationRecord(id, _, _, _, _, _, _, _, PresentationRejected, _, _, _, _, _, _, _) =>
          ZIO.logDebug("PresentationRecord: PresentationRejected") *> ZIO.unit
      }
    } yield ()

    aux
      .tapError(e =>
        for {
          presentationService <- ZIO.service[PresentationService]
          _ <- presentationService
            .reportProcessingFailure(record.id, Some(e.toString))
            .tapError(err =>
              ZIO.logErrorCause(
                s"Present Proof - failed to report processing failure: ${record.id}",
                Cause.fail(err)
              )
            )
        } yield ()
      )
      .catchAll(e => ZIO.logErrorCause(s"Present Proof - Error processing record: ${record.id} ", Cause.fail(e)))
      .catchAllDefect(d => ZIO.logErrorCause(s"Present Proof - Defect processing record: ${record.id}", Cause.fail(d)))
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

  val syncDIDPublicationStateFromDlt =
    for {
      managedDidService <- ZIO.service[ManagedDIDService]
      _ <- managedDidService.syncManagedDIDState
      _ <- managedDidService.syncUnconfirmedUpdateOperations
    } yield ()

}
