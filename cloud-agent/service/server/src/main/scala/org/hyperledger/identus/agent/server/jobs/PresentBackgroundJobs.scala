package org.hyperledger.identus.agent.server.jobs

import cats.syntax.all.*
import io.circe.parser.*
import io.circe.syntax.*
import org.hyperledger.identus.agent.server.config.AppConfig
import org.hyperledger.identus.agent.server.jobs.BackgroundJobError.{
  ErrorResponseReceivedFromPeerAgent,
  InvalidState,
  NotImplemented
}
import org.hyperledger.identus.castor.core.model.did.*
import org.hyperledger.identus.mercury.*
import org.hyperledger.identus.mercury.model.*
import org.hyperledger.identus.mercury.protocol.presentproof.*
import org.hyperledger.identus.mercury.protocol.reportproblem.v2.*
import org.hyperledger.identus.pollux.core.model.*
import org.hyperledger.identus.pollux.core.model.error.PresentationError.*
import org.hyperledger.identus.pollux.core.model.error.{CredentialServiceError, PresentationError}
import org.hyperledger.identus.pollux.core.service.serdes.AnoncredCredentialProofsV1
import org.hyperledger.identus.pollux.core.service.{CredentialService, PresentationService}
import org.hyperledger.identus.pollux.vc.jwt.{JWT, JwtPresentation, DidResolver as JwtDidResolver}
import org.hyperledger.identus.shared.utils.DurationOps.toMetricsSeconds
import org.hyperledger.identus.shared.utils.aspects.CustomMetricsAspect
import zio.*
import zio.json.ast.Json
import zio.metrics.*
import zio.prelude.Validation
import zio.prelude.ZValidation.*
import org.hyperledger.identus.agent.walletapi.storage.DIDNonSecretStorage
import org.hyperledger.identus.agent.walletapi.model.error.DIDSecretStorageError.WalletNotFoundError
import org.hyperledger.identus.resolvers.DIDResolver
import org.hyperledger.identus.shared.models.WalletAccessContext
import java.time.{Clock, Instant, ZoneId}
import org.hyperledger.identus.castor.core.service.DIDService
import org.hyperledger.identus.agent.walletapi.service.ManagedDIDService
import org.hyperledger.identus.shared.http.*

object PresentBackgroundJobs extends BackgroundJobsHelper {

  val presentProofExchanges = {
    val presentProofDidComExchange = for {
      presentationService <- ZIO.service[PresentationService]
      config <- ZIO.service[AppConfig]
      records <- presentationService
        .getPresentationRecordsByStatesForAllWallets(
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
    presentProofDidComExchange
  }

  private def counterMetric(key: String) = Metric
    .counterInt(key)
    .fromConst(1)

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

  private[this] def performPresentProofExchange(record: PresentationRecord): URIO[
    AppConfig & DidOps & DIDResolver & JwtDidResolver & HttpClient & PresentationService & CredentialService &
      DIDNonSecretStorage & DIDService & ManagedDIDService,
    Unit
  ] = {
    import org.hyperledger.identus.pollux.core.model.PresentationRecord.ProtocolState.*

    val VerifierReqPendingToSentSuccess = counterMetric(
      "present_proof_flow_verifier_request_pending_to_sent_success_counter"
    )
    val VerifierReqPendingToSentFailed = counterMetric(
      "present_proof_flow_verifier_request_pending_to_sent_failed_counter"
    )
    val VerifierReqPendingToSent = counterMetric(
      "present_proof_flow_verifier_request_pending_to_sent_all_counter"
    )

    val ProverPresentationPendingToGeneratedSuccess = counterMetric(
      "present_proof_flow_prover_presentation_pending_to_generated_success_counter"
    )
    val ProverPresentationPendingToGeneratedFailed = counterMetric(
      "present_proof_flow_prover_presentation_pending_to_generated_failed_counter"
    )
    val ProverPresentationPendingToGenerated = counterMetric(
      "present_proof_flow_prover_presentation_pending_to_generated_all_counter"
    )

    val ProverPresentationGeneratedToSentSuccess = counterMetric(
      "present_proof_flow_prover_presentation_generated_to_sent_success_counter"
    )
    val ProverPresentationGeneratedToSentFailed = counterMetric(
      "present_proof_flow_prover_presentation_generated_to_sent_failed_counter"
    )
    val ProverPresentationGeneratedToSent = counterMetric(
      "present_proof_flow_prover_presentation_generated_to_sent_all_counter"
    )

    val VerifierPresentationReceivedToProcessedSuccess = counterMetric(
      "present_proof_flow_verifier_presentation_received_to_verification_success_or_failure_success_counter"
    )
    val VerifierPresentationReceivedToProcessedFailed = counterMetric(
      "present_proof_flow_verifier_presentation_received_to_verification_success_or_failure_failed_counter"
    )
    val VerifierPresentationReceivedToProcessed = counterMetric(
      "present_proof_flow_verifier_presentation_received_to_verification_success_or_failure_all_counter"
    )

    val VerifierSendPresentationRequestMsgSuccess = counterMetric(
      "present_proof_flow_verifier_send_presentation_request_msg_success_counter"
    )
    val VerifierSendPresentationRequestMsgFailed = counterMetric(
      "present_proof_flow_verifier_send_presentation_request_msg_failed_counter"
    )

    val ProverSendPresentationMsgSuccess = counterMetric(
      "present_proof_flow_prover_send_presentation_msg_success_counter"
    )
    val ProverSendPresentationMsgFailed = counterMetric(
      "present_proof_flow_prover_send_presentation_msg_failed_counter"
    )

    val aux = for {
      _ <- ZIO.logDebug(s"Running action with records => $record")
      _ <- record match {
        // ##########################
        // ### PresentationRecord ###
        // ##########################
        case PresentationRecord(id, _, _, _, _, _, _, _, ProposalPending, _, _, _, _, _, _, _, _, _, _) =>
          ZIO.fail(NotImplemented)
        case PresentationRecord(id, _, _, _, _, _, _, _, ProposalSent, _, _, _, _, _, _, _, _, _, _) =>
          ZIO.fail(NotImplemented)
        case PresentationRecord(id, _, _, _, _, _, _, _, ProposalReceived, _, _, _, _, _, _, _, _, _, _) =>
          ZIO.fail(NotImplemented)
        case PresentationRecord(id, _, _, _, _, _, _, _, ProposalRejected, _, _, _, _, _, _, _, _, _, _) =>
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
              RequestPending,
              _,
              oRecord,
              _,
              _,
              _,
              _,
              _,
              _,
              _,
              _
            ) => // Verifier
          oRecord match
            case None => ZIO.fail(InvalidState("PresentationRecord 'RequestPending' with no Record"))
            case Some(record) =>
              val verifierReqPendingToSentFlow = for {
                _ <- ZIO.log(s"PresentationRecord: RequestPending (Send Message)")
                walletAccessContext <- buildWalletAccessContextLayer(record.from)
                result <- (for {
                  didOps <- ZIO.service[DidOps]
                  didCommAgent <- buildDIDCommAgent(record.from).provideSomeLayer(ZLayer.succeed(walletAccessContext))
                  resp <- MessagingService.send(record.makeMessage).provideSomeLayer(didCommAgent) @@ Metric
                    .gauge("present_proof_flow_verifier_send_presentation_request_msg_ms_gauge")
                    .trackDurationWith(_.toMetricsSeconds)
                  service <- ZIO.service[PresentationService]
                  _ <- {
                    if (resp.status >= 200 && resp.status < 300)
                      service
                        .markRequestPresentationSent(
                          id
                        )
                        .provideSomeLayer(
                          ZLayer.succeed(walletAccessContext)
                        ) @@ VerifierSendPresentationRequestMsgSuccess @@ CustomMetricsAspect.endRecordingTime(
                        s"${record.id}_present_proof_flow_verifier_req_pending_to_sent_ms_gauge",
                        "present_proof_flow_verifier_req_pending_to_sent_ms_gauge"
                      )
                    else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp)) @@ VerifierSendPresentationRequestMsgFailed
                  }
                } yield ()).mapError(e => (walletAccessContext, handlePresentationErrors(e)))
              } yield result

              verifierReqPendingToSentFlow
                @@ VerifierReqPendingToSentSuccess.trackSuccess
                @@ VerifierReqPendingToSentFailed.trackError
                @@ VerifierReqPendingToSent
                @@ Metric
                  .gauge("present_proof_flow_verifier_req_pending_to_sent_flow_ms_gauge")
                  .trackDurationWith(_.toMetricsSeconds)

        case PresentationRecord(id, _, _, _, _, _, _, _, RequestSent, _, _, _, _, _, _, _, _, _, _) => // Verifier
          ZIO.logDebug("PresentationRecord: RequestSent") *> ZIO.unit
        case PresentationRecord(id, _, _, _, _, _, _, _, RequestReceived, _, _, _, _, _, _, _, _, _, _) => // Prover
          ZIO.logDebug("PresentationRecord: RequestReceived") *> ZIO.unit
        case PresentationRecord(id, _, _, _, _, _, _, _, RequestRejected, _, _, _, _, _, _, _, _, _, _) => // Prover
          ZIO.logDebug("PresentationRecord: RequestRejected") *> ZIO.unit
        case PresentationRecord(id, _, _, _, _, _, _, _, ProblemReportPending, _, _, _, _, _, _, _, _, _, _) =>
          ZIO.fail(NotImplemented)
        case PresentationRecord(id, _, _, _, _, _, _, _, ProblemReportSent, _, _, _, _, _, _, _, _, _, _) =>
          ZIO.fail(NotImplemented)
        case PresentationRecord(id, _, _, _, _, _, _, _, ProblemReportReceived, _, _, _, _, _, _, _, _, _, _) =>
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
              CredentialFormat.JWT,
              oRequestPresentation,
              _,
              _,
              credentialsToUse,
              _,
              _,
              _,
              _,
              _
            ) => // Prover
          oRequestPresentation match
            case None => ZIO.fail(InvalidState("PresentationRecord 'RequestPending' with no Record"))
            case Some(requestPresentation) => // TODO create build method in mercury for Presentation
              val proverPresentationPendingToGeneratedFlow = for {
                walletAccessContext <- buildWalletAccessContextLayer(requestPresentation.to)
                result <- (for {
                  presentationService <- ZIO.service[PresentationService]
                  prover <- createPrismDIDIssuerFromPresentationCredentials(id, credentialsToUse.getOrElse(Nil))
                    .provideSomeLayer(ZLayer.succeed(walletAccessContext))
                  presentation <-
                    for {
                      presentationPayload <-
                        presentationService
                          .createJwtPresentationPayloadFromRecord(
                            id,
                            prover,
                            Instant.now()
                          )
                          .provideSomeLayer(ZLayer.succeed(walletAccessContext))
                      signedJwtPresentation = JwtPresentation.toEncodedJwt(
                        presentationPayload.toW3CPresentationPayload,
                        prover
                      )
                      presentation <- ZIO.succeed(
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
                    } yield presentation
                  _ <- presentationService
                    .markPresentationGenerated(id, presentation)
                    .provideSomeLayer(ZLayer.succeed(walletAccessContext))
                } yield ()).mapError(e => (walletAccessContext, handlePresentationErrors(e)))
              } yield result

              proverPresentationPendingToGeneratedFlow
                @@ ProverPresentationPendingToGeneratedSuccess.trackSuccess
                @@ ProverPresentationPendingToGeneratedFailed.trackError
                @@ ProverPresentationPendingToGenerated
                @@ Metric
                  .gauge("present_proof_flow_prover_presentation_pending_to_generated_flow_ms_gauge")
                  .trackDurationWith(_.toMetricsSeconds)
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
              CredentialFormat.AnonCreds,
              oRequestPresentation,
              _,
              _,
              _,
              _,
              None,
              _,
              _,
              _
            ) => // Prover
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
              CredentialFormat.AnonCreds,
              oRequestPresentation,
              _,
              _,
              _,
              _,
              Some(credentialsToUseJson),
              _,
              _,
              _
            ) => // Prover
          oRequestPresentation match
            case None => ZIO.fail(InvalidState("PresentationRecord 'RequestPending' with no Record"))
            case Some(requestPresentation) => // TODO create build method in mercury for Presentation
              val proverPresentationPendingToGeneratedFlow = for {
                walletAccessContext <- buildWalletAccessContextLayer(requestPresentation.to)
                result <- (for {
                  presentationService <- ZIO.service[PresentationService]
                  anoncredCredentialProofs <-
                    AnoncredCredentialProofsV1.schemaSerDes
                      .deserialize(credentialsToUseJson)
                      .mapError(error => PresentationError.UnexpectedError(error.error))
                  presentation <-
                    presentationService
                      .createAnoncredPresentation(
                        requestPresentation,
                        id,
                        anoncredCredentialProofs,
                        Instant.now()
                      )
                      .provideSomeLayer(ZLayer.succeed(walletAccessContext))
                  _ <- presentationService
                    .markPresentationGenerated(id, presentation)
                    .provideSomeLayer(ZLayer.succeed(walletAccessContext))
                } yield ()).mapError(e => (walletAccessContext, handlePresentationErrors(e)))
              } yield result

              proverPresentationPendingToGeneratedFlow
                @@ ProverPresentationPendingToGeneratedSuccess.trackSuccess
                @@ ProverPresentationPendingToGeneratedFailed.trackError
                @@ ProverPresentationPendingToGenerated
                @@ Metric
                  .gauge("present_proof_flow_prover_presentation_pending_to_generated_flow_ms_gauge")
                  .trackDurationWith(_.toMetricsSeconds)

        case PresentationRecord(
              id,
              _,
              _,
              _,
              _,
              _,
              _,
              _,
              PresentationGenerated,
              _,
              _,
              _,
              presentation,
              _,
              _,
              _,
              _,
              _,
              _
            ) => // Prover
          ZIO.logDebug("PresentationRecord: PresentationGenerated") *> ZIO.unit
          presentation match
            case None => ZIO.fail(InvalidState("PresentationRecord in 'PresentationPending' with no Presentation"))
            case Some(p) =>
              val ProverPresentationGeneratedToSentFlow = for {
                _ <- ZIO.log(s"PresentationRecord: PresentationPending (Send Message)")
                walletAccessContext <- buildWalletAccessContextLayer(p.from)
                result <- (for {
                  didCommAgent <- buildDIDCommAgent(p.from).provideSomeLayer(ZLayer.succeed(walletAccessContext))
                  resp <- MessagingService
                    .send(p.makeMessage)
                    .provideSomeLayer(didCommAgent) @@ Metric
                    .gauge("present_proof_flow_prover_send_presentation_msg_ms_gauge")
                    .trackDurationWith(_.toMetricsSeconds)
                  service <- ZIO.service[PresentationService]
                  _ <- {
                    if (resp.status >= 200 && resp.status < 300)
                      service
                        .markPresentationSent(id)
                        .provideSomeLayer(
                          ZLayer.succeed(walletAccessContext)
                        ) @@ ProverSendPresentationMsgSuccess @@ CustomMetricsAspect
                        .endRecordingTime(
                          s"${record.id}_present_proof_flow_prover_presentation_generated_to_sent_ms_gauge",
                          "present_proof_flow_prover_presentation_generated_to_sent_ms_gauge"
                        )
                    else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp)) @@ ProverSendPresentationMsgFailed
                  }
                } yield ()).mapError(e => (walletAccessContext, handlePresentationErrors(e)))

              } yield result

              ProverPresentationGeneratedToSentFlow
                @@ ProverPresentationGeneratedToSentSuccess.trackSuccess
                @@ ProverPresentationGeneratedToSentFailed.trackError
                @@ ProverPresentationGeneratedToSent
                @@ Metric
                  .gauge("present_proof_flow_prover_presentation_generated_to_sent_flow_ms_gauge")
                  .trackDurationWith(_.toMetricsSeconds)

        case PresentationRecord(id, _, _, _, _, _, _, _, PresentationSent, _, _, _, _, _, _, _, _, _, _) =>
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
              CredentialFormat.JWT,
              mayBeRequestPresentation,
              _,
              presentation,
              _,
              _,
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
              val verifierPresentationReceivedToProcessed =
                for {
                  walletAccessContext <- buildWalletAccessContextLayer(p.to)
                  result <- (for {
                    didResolverService <- ZIO.service[JwtDidResolver]
                    credentialsValidationResult <- p.attachments.head.data match {
                      case Base64(data) =>
                        val base64Decoded = new String(java.util.Base64.getDecoder.decode(data))
                        val maybePresentationOptions: Either[PresentationError, Option[
                          org.hyperledger.identus.pollux.core.model.presentation.Options
                        ]] =
                          mayBeRequestPresentation
                            .map(
                              _.attachments.headOption
                                .map(attachment =>
                                  decode[org.hyperledger.identus.mercury.model.JsonData](
                                    attachment.data.asJson.noSpaces
                                  )
                                    .flatMap(data =>
                                      org.hyperledger.identus.pollux.core.model.presentation.PresentationAttachment.given_Decoder_PresentationAttachment
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
                        val presentationValidationResult = for {
                          _ <- ZIO.fromEither(maybePresentationOptions.map {
                            case Some(options) =>
                              JwtPresentation.validatePresentation(
                                JWT(base64Decoded),
                                options.domain,
                                options.challenge
                              )
                            case _ => Validation.unit
                          })
                          verificationConfig <- ZIO.service[AppConfig].map(_.agent.verification)
                          _ <- ZIO.log(s"VerificationConfig: ${verificationConfig}")

                          // https://www.w3.org/TR/vc-data-model/#proofs-signatures-0
                          // A proof is typically attached to a verifiable presentation for authentication purposes
                          // and to a verifiable credential as a method of assertion.
                          httpLayer <- ZIO.service[HttpClient]
                          httpUrlResolver = new UriResolver {
                            override def resolve(uri: String): IO[GenericUriResolverError, String] = {
                              val res = HttpClient
                                .get(uri)
                                .map(x => x.bodyAsString)
                                .provideSomeLayer(ZLayer.succeed(httpLayer))
                              res.mapError(err => SchemaSpecificResolutionError("http", err))
                            }
                          }
                          genericUriResolver = GenericUriResolver(
                            Map(
                              "data" -> DataUrlResolver(),
                              "http" -> httpUrlResolver,
                              "https" -> httpUrlResolver
                            )
                          )
                          result <- JwtPresentation
                            .verify(
                              JWT(base64Decoded),
                              verificationConfig.toPresentationVerificationOptions()
                            )(didResolverService, genericUriResolver)(clock)
                            .mapError(error => PresentationError.UnexpectedError(error.mkString))
                        } yield result

                        presentationValidationResult

                      case any => ZIO.fail(NotImplemented)
                    }
                    _ <- credentialsValidationResult match
                      case l @ Failure(_, _) => ZIO.logError(s"CredentialsValidationResult: $l")
                      case l @ Success(_, _) => ZIO.logInfo(s"CredentialsValidationResult: $l")
                    service <- ZIO.service[PresentationService]
                    presReceivedToProcessedAspect = CustomMetricsAspect.endRecordingTime(
                      s"${record.id}_present_proof_flow_verifier_presentation_received_to_verification_success_or_failure_ms_gauge",
                      "present_proof_flow_verifier_presentation_received_to_verification_success_or_failure_ms_gauge"
                    )
                    _ <- credentialsValidationResult match {
                      case Success(log, value) =>
                        service
                          .markPresentationVerified(id)
                          .provideSomeLayer(ZLayer.succeed(walletAccessContext)) @@ presReceivedToProcessedAspect
                      case Failure(log, error) => {
                        for {
                          _ <- service
                            .markPresentationVerificationFailed(id)
                            .provideSomeLayer(ZLayer.succeed(walletAccessContext)) @@ presReceivedToProcessedAspect
                          didCommAgent <- buildDIDCommAgent(p.from).provideSomeLayer(
                            ZLayer.succeed(walletAccessContext)
                          )
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

                  } yield ()).mapError(e => (walletAccessContext, handlePresentationErrors(e)))
                } yield result
              verifierPresentationReceivedToProcessed
                @@ VerifierPresentationReceivedToProcessedSuccess.trackSuccess
                @@ VerifierPresentationReceivedToProcessedFailed.trackError
                @@ VerifierPresentationReceivedToProcessed
                @@ Metric
                  .gauge(
                    "present_proof_flow_verifier_presentation_received_to_verification_success_or_failure_flow_ms_gauge"
                  )
                  .trackDurationWith(_.toMetricsSeconds)
        case PresentationRecord(
              _,
              _,
              _,
              _,
              _,
              _,
              _,
              _,
              PresentationReceived,
              CredentialFormat.AnonCreds,
              None,
              _,
              _,
              _,
              _,
              _,
              _,
              _,
              _
            ) =>
          ZIO.fail(InvalidState("PresentationRecord in 'PresentationReceived' with no Presentation Request"))
        case PresentationRecord(
              _,
              _,
              _,
              _,
              _,
              _,
              _,
              _,
              PresentationReceived,
              CredentialFormat.AnonCreds,
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
          ZIO.fail(InvalidState("PresentationRecord in 'PresentationReceived' with no Presentation"))
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
              CredentialFormat.AnonCreds,
              Some(requestPresentation),
              _,
              Some(presentation),
              _,
              _,
              _,
              _,
              _,
              _
            ) => // Verifier
          ZIO.logDebug("PresentationRecord: PresentationReceived") *> ZIO.unit
          val verifierPresentationReceivedToProcessed =
            for {
              walletAccessContext <- buildWalletAccessContextLayer(presentation.to)
              presReceivedToProcessedAspect = CustomMetricsAspect.endRecordingTime(
                s"${record.id}_present_proof_flow_verifier_presentation_received_to_verification_success_or_failure_ms_gauge",
                "present_proof_flow_verifier_presentation_received_to_verification_success_or_failure_ms_gauge"
              )
              result <- (for {
                service <- ZIO.service[PresentationService]
                _ <- (service
                  .verifyAnoncredPresentation(presentation, requestPresentation, id)
                  .provideSomeLayer(ZLayer.succeed(walletAccessContext)) @@ presReceivedToProcessedAspect)
                  .flatMapError(e =>
                    for {
                      didCommAgent <- buildDIDCommAgent(presentation.from).provideSomeLayer(
                        ZLayer.succeed(walletAccessContext)
                      )
                      reportproblem = ReportProblem.build(
                        fromDID = presentation.to,
                        toDID = presentation.from,
                        pthid = presentation.thid.getOrElse(presentation.id),
                        code = ProblemCode("e.p.presentation-verification-failed"),
                        comment = Some(e.toString)
                      )
                      _ <- MessagingService
                        .send(reportproblem.toMessage)
                        .provideSomeLayer(didCommAgent)
                      _ <- ZIO.log(s"CredentialsValidationResult: ${e.toString}")
                    } yield ()
                    ZIO.succeed(e)
                  )
              } yield ()).mapError(e => (walletAccessContext, handlePresentationErrors(e)))
            } yield result
          verifierPresentationReceivedToProcessed
            @@ VerifierPresentationReceivedToProcessedSuccess.trackSuccess
            @@ VerifierPresentationReceivedToProcessedFailed.trackError
            @@ VerifierPresentationReceivedToProcessed
            @@ Metric
              .gauge(
                "present_proof_flow_verifier_presentation_received_to_verification_success_or_failure_flow_ms_gauge"
              )
              .trackDurationWith(_.toMetricsSeconds)
        case PresentationRecord(
              id,
              _,
              _,
              _,
              _,
              _,
              _,
              _,
              PresentationVerificationFailed,
              _,
              _,
              _,
              _,
              _,
              _,
              _,
              _,
              _,
              _
            ) =>
          ZIO.logDebug("PresentationRecord: PresentationVerificationFailed") *> ZIO.unit
        case PresentationRecord(id, _, _, _, _, _, _, _, PresentationAccepted, _, _, _, _, _, _, _, _, _, _) =>
          ZIO.logDebug("PresentationRecord: PresentationVerifiedAccepted") *> ZIO.unit
        case PresentationRecord(id, _, _, _, _, _, _, _, PresentationVerified, _, _, _, _, _, _, _, _, _, _) =>
          ZIO.logDebug("PresentationRecord: PresentationVerified") *> ZIO.unit
        case PresentationRecord(id, _, _, _, _, _, _, _, PresentationRejected, _, _, _, _, _, _, _, _, _, _) =>
          ZIO.logDebug("PresentationRecord: PresentationRejected") *> ZIO.unit
      }
    } yield ()

    aux
      .tapError(
        {
          case error: (WalletNotFoundError | BackgroundJobError) =>
            ZIO.logErrorCause(
              s"Present Proof - Error processing record: ${record.id}",
              Cause.fail(error)
            )
          case ((walletAccessContext, e)) =>
            for {
              presentationService <- ZIO.service[PresentationService]
              _ <- presentationService
                .reportProcessingFailure(record.id, Some(e.toString))
                .provideSomeLayer(
                  ZLayer.succeed(walletAccessContext)
                )
                .tapError(err =>
                  ZIO.logErrorCause(
                    s"Present Proof - failed to report processing failure: ${record.id}",
                    Cause.fail(err)
                  )
                )
            } yield ()

        }
      )
      .catchAll(e => ZIO.logErrorCause(s"Present Proof - Error processing record: ${record.id} ", Cause.fail(e)))
      .catchAllDefect(d => ZIO.logErrorCause(s"Present Proof - Defect processing record: ${record.id}", Cause.fail(d)))

  }

  private[this] def handlePresentationErrors: PartialFunction[
    Throwable | CredentialServiceError | PresentationError | BackgroundJobError,
    PresentationError | CredentialServiceError | BackgroundJobError
  ] = {
    case c: CredentialServiceError => c
    case p: PresentationError      => p
    case b: BackgroundJobError     => b
    case t: Throwable              => PresentationError.UnexpectedError(t.getMessage())
  }

  val syncDIDPublicationStateFromDlt =
    for {
      managedDidService <- ZIO.service[ManagedDIDService]
      _ <- managedDidService.syncManagedDIDState
      _ <- managedDidService.syncUnconfirmedUpdateOperations
    } yield ()

}
