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
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.utils.DurationOps.toMetricsSeconds
import io.iohk.atala.shared.utils.aspects.CustomMetricsAspect
import zio.*
import zio.metrics.*
import zio.prelude.Validation
import zio.prelude.ZValidation.*

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
    val presentProofDidComExchange = for {
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
    presentProofDidComExchange
  }

  private def counterMetric(key: String) = Metric
    .counterInt(key)
    .fromConst(1)

  private[this] def performIssueCredentialExchange(record: IssueCredentialRecord) = {
    import IssueCredentialRecord.*
    import IssueCredentialRecord.ProtocolState.*
    import IssueCredentialRecord.PublicationState.*

    val IssuerSendOfferMsgFailed = counterMetric(
      "issuance_flow_issuer_send_offer_msg_failed_counter"
    )
    val IssuerSendOfferMsgSucceed = counterMetric(
      "issuance_flow_issuer_send_offer_msg_succeed_counter"
    )

    val IssuerSendOfferSucceed = counterMetric(
      "issuance_flow_issuer_send_offer_flow_succeed_counter"
    )

    val IssuerSendOfferFailed = counterMetric(
      "issuance_flow_issuer_send_offer_flow_failed_counter"
    )

    val IssuerSendOfferAll = counterMetric(
      "issuance_flow_issuer_send_offer_flow_all_counter"
    )

    val HolderPendingToGeneratedSuccess = counterMetric(
      "issuance_flow_holder_req_pending_to_generated_flow_success_counter"
    )

    val HolderPendingToGeneratedFailed = counterMetric(
      "issuance_flow_holder_req_pending_to_generated_flow_failed_counter"
    )

    val HolderPendingToGeneratedAll = counterMetric(
      "issuance_flow_holder_req_pending_to_generated_flow_all_counter"
    )

    val HolderSendReqSucceed = counterMetric(
      "issuance_flow_holder_send_req_msg_succeed_counter"
    )

    val HolderSendReqFailed = counterMetric(
      "issuance_flow_holder_send_req_msg_failed_counter"
    )

    val HolderGeneratedToSentSucceed = counterMetric(
      "issuance_flow_holder_req_generated_to_sent_flow_success_counter"
    )
    val HolderGeneratedToSentFailed = counterMetric(
      "issuance_flow_holder_req_generated_to_sent_flow_failed_counter"
    )
    val HolderGeneratedToSentAll = counterMetric(
      "issuance_flow_holder_req_generated_to_sent_flow_all_counter"
    )

    val IssuerReceivedToPendingSuccess = counterMetric(
      "issuance_flow_issuer_cred_received_to_pending_flow_success_counter"
    )

    val IssuerReceivedToPendingFailed = counterMetric(
      "issuance_flow_issuer_cred_received_to_pending_flow_failed_counter"
    )

    val IssuerReceivedToPendingAll = counterMetric(
      "issuance_flow_issuer_cred_received_to_pending_flow_all_counter"
    )

    val IssuerPendingToGeneratedSuccess = counterMetric(
      "issuance_flow_issuer_cred_pending_to_generated_flow_success_counter"
    )
    val IssuerPendingToGeneratedFailed = counterMetric(
      "issuance_flow_issuer_cred_pending_to_generated_flow_failed_counter"
    )
    val IssuerPendingToGeneratedAll = counterMetric(
      "issuance_flow_issuer_cred_pending_to_generated_flow_all_counter"
    )

    val IssuerSendCredentialSuccess = counterMetric(
      "issuance_flow_issuer_send_cred_success_counter"
    )

    val IssuerSendCredentialFailed = counterMetric(
      "issuance_flow_issuer_send_cred_failed_counter"
    )

    val IssuerSendCredentialAll = counterMetric(
      "issuance_flow_issuer_send_cred_all_counter"
    )

    val IssuerSendCredentialMsgFailed = counterMetric(
      "issuance_flow_issuer_send_credential_msg_failed_counter"
    )
    val IssuerSendCredentialMsgSuccess = counterMetric(
      "issuance_flow_issuer_send_credential_msg_succeed_counter"
    )

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
          val sendOfferFlow = for {
            _ <- ZIO.log(s"IssueCredentialRecord: OfferPending (START)")
            didCommAgent <- buildDIDCommAgent(offer.from)
            resp <- MessagingService
              .send(offer.makeMessage)
              .provideSomeLayer(didCommAgent) @@ Metric
              .gauge("issuance_flow_issuer_send_offer_ms_gauge")
              .trackDurationWith(_.toMetricsSeconds)
            credentialService <- ZIO.service[CredentialService]
            _ <- {
              if (resp.status >= 200 && resp.status < 300)
                credentialService.markOfferSent(id) @@
                  IssuerSendOfferMsgSucceed @@
                  CustomMetricsAspect.endRecordingTime(
                    s"${record.id}_issuer_offer_pending_to_sent_ms_gauge",
                    "issuance_flow_issuer_offer_pending_to_sent_ms_gauge"
                  )
              else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp)) @@ IssuerSendOfferMsgFailed
            }
          } yield ()

          sendOfferFlow @@ IssuerSendOfferSucceed.trackSuccess
            @@ IssuerSendOfferFailed.trackError
            @@ IssuerSendOfferAll
            @@ Metric
              .gauge("issuance_flow_issuer_send_offer_flow_ms_gauge")
              .trackDurationWith(_.toMetricsSeconds)

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
          val holderPendingToGeneratedFlow = for {
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

          holderPendingToGeneratedFlow @@ HolderPendingToGeneratedSuccess.trackSuccess
            @@ HolderPendingToGeneratedFailed.trackError
            @@ HolderPendingToGeneratedAll
            @@ Metric
              .gauge("issuance_flow_holder_req_pending_to_generated_flow_ms_gauge")
              .trackDurationWith(_.toMetricsSeconds)

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
          val holderGeneratedToSentFlow = for {
            didCommAgent <- buildDIDCommAgent(request.from)
            resp <- MessagingService
              .send(request.makeMessage)
              .provideSomeLayer(didCommAgent) @@ Metric
              .gauge("issuance_flow_holder_send_request_ms_gauge")
              .trackDurationWith(_.toMetricsSeconds)
            credentialService <- ZIO.service[CredentialService]
            _ <- {
              if (resp.status >= 200 && resp.status < 300)
                credentialService.markRequestSent(id) @@ HolderSendReqSucceed
                  @@ CustomMetricsAspect.endRecordingTime(
                    s"${record.id}_issuance_flow_holder_req_generated_to_sent",
                    "issuance_flow_holder_req_generated_to_sent_ms_gauge"
                  )
              else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp)) @@ HolderSendReqFailed
            }
          } yield ()

          holderGeneratedToSentFlow @@ HolderGeneratedToSentSucceed.trackSuccess
            @@ HolderGeneratedToSentFailed.trackError
            @@ HolderGeneratedToSentAll
            @@ Metric
              .gauge("issuance_flow_holder_req_generated_to_sent_flow_ms_gauge")
              .trackDurationWith(_.toMetricsSeconds)

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
          val issuerReceivedToPendingFlow = for {
            credentialService <- ZIO.service[CredentialService]
            _ <- credentialService.acceptCredentialRequest(id)
          } yield ()

          issuerReceivedToPendingFlow @@ IssuerReceivedToPendingSuccess.trackSuccess
            @@ IssuerReceivedToPendingFailed.trackError
            @@ IssuerReceivedToPendingAll
            @@ Metric
              .gauge("issuance_flow_issuer_cred_received_to_pending_flow_ms_gauge")
              .trackDurationWith(_.toMetricsSeconds)

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
          // Generate the JWT Credential and store it in DB as an attachment to IssueCredentialData
          // Set ProtocolState to CredentialGenerated
          // Set PublicationState to PublicationPending
          val issuerPendingToGeneratedFlow = for {
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

          issuerPendingToGeneratedFlow @@ IssuerPendingToGeneratedSuccess.trackSuccess
            @@ IssuerPendingToGeneratedFailed.trackError
            @@ IssuerPendingToGeneratedAll
            @@ Metric
              .gauge("issuance_flow_issuer_cred_pending_to_generated_flow_ms_gauge")
              .trackDurationWith(_.toMetricsSeconds)

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
          val sendCredentialManualIssuanceFlow = for {
            didCommAgent <- buildDIDCommAgent(issue.from)
            resp <- MessagingService
              .send(issue.makeMessage)
              .provideSomeLayer(didCommAgent) @@ Metric
              .gauge("issuance_flow_issuer_send_credential_msg_ms_gauge")
              .trackDurationWith(_.toMetricsSeconds)
            credentialService <- ZIO.service[CredentialService]
            _ <- {
              if (resp.status >= 200 && resp.status < 300)
                credentialService.markCredentialSent(id) @@ IssuerSendCredentialMsgSuccess
              else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp)) @@ IssuerSendCredentialMsgFailed
            }
          } yield ()

          sendCredentialManualIssuanceFlow @@ IssuerSendCredentialSuccess.trackSuccess
            @@ IssuerSendCredentialFailed.trackError
            @@ IssuerSendCredentialAll @@ Metric
              .gauge("issuance_flow_issuer_send_cred_flow_ms_gauge")
              .trackDurationWith(_.toMetricsSeconds)

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
          val sendCredentialAutomaticIssuanceFlow = for {
            didCommAgent <- buildDIDCommAgent(issue.from)
            resp <- MessagingService.send(issue.makeMessage).provideSomeLayer(didCommAgent) @@ Metric
              .gauge("issuance_flow_issuer_send_credential_msg_ms_gauge")
              .trackDurationWith(_.toMetricsSeconds)
            credentialService <- ZIO.service[CredentialService]
            _ <- {
              if (resp.status >= 200 && resp.status < 300)
                credentialService.markCredentialSent(id) @@ IssuerSendCredentialMsgSuccess
              else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp)) @@ IssuerSendCredentialMsgFailed
            }
          } yield ()

          sendCredentialAutomaticIssuanceFlow @@ IssuerSendCredentialSuccess.trackSuccess
            @@ IssuerSendCredentialFailed.trackError
            @@ IssuerSendCredentialAll @@ Metric
              .gauge("issuance_flow_issuer_send_cred_flow_ms_gauge")
              .trackDurationWith(_.toMetricsSeconds)

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

  // TODO: Improvements needed here:
  // - Improve consistency in error handling (ATL-3210)
  private[this] def createJwtIssuer(
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
              val verifierReqPendingToSentFlow = for {
                _ <- ZIO.log(s"PresentationRecord: RequestPending (Send Massage)")
                didOps <- ZIO.service[DidOps]
                didCommAgent <- buildDIDCommAgent(record.from)
                resp <- MessagingService.send(record.makeMessage).provideSomeLayer(didCommAgent) @@ Metric
                  .gauge("present_proof_flow_verifier_send_presentation_request_msg_ms_gauge")
                  .trackDurationWith(_.toMetricsSeconds)
                service <- ZIO.service[PresentationService]
                _ <- {
                  if (resp.status >= 200 && resp.status < 300)
                    service.markRequestPresentationSent(
                      id
                    ) @@ VerifierSendPresentationRequestMsgSuccess @@ CustomMetricsAspect.endRecordingTime(
                      s"${record.id}_present_proof_flow_verifier_req_pending_to_sent_ms_gauge",
                      "present_proof_flow_verifier_req_pending_to_sent_ms_gauge"
                    )
                  else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp)) @@ VerifierSendPresentationRequestMsgFailed
                }
              } yield ()

              verifierReqPendingToSentFlow
                @@ VerifierReqPendingToSentSuccess.trackSuccess
                @@ VerifierReqPendingToSentFailed.trackError
                @@ VerifierReqPendingToSent
                @@ Metric
                  .gauge("present_proof_flow_verifier_req_pending_to_sent_flow_ms_gauge")
                  .trackDurationWith(_.toMetricsSeconds)

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
          val proverPresentationPendingToGeneratedFlow = for {
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
              presentation,
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
                didCommAgent <- buildDIDCommAgent(p.from)
                resp <- MessagingService
                  .send(p.makeMessage)
                  .provideSomeLayer(didCommAgent) @@ Metric
                  .gauge("present_proof_flow_prover_send_presentation_msg_ms_gauge")
                  .trackDurationWith(_.toMetricsSeconds)
                service <- ZIO.service[PresentationService]
                _ <- {
                  if (resp.status >= 200 && resp.status < 300)
                    service.markPresentationSent(id) @@ ProverSendPresentationMsgSuccess @@ CustomMetricsAspect
                      .endRecordingTime(
                        s"${record.id}_present_proof_flow_prover_presentation_generated_to_sent_ms_gauge",
                        "present_proof_flow_prover_presentation_generated_to_sent_ms_gauge"
                      )
                  else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp)) @@ ProverSendPresentationMsgFailed
                }
              } yield ()

              ProverPresentationGeneratedToSentFlow
                @@ ProverPresentationGeneratedToSentSuccess.trackSuccess
                @@ ProverPresentationGeneratedToSentFailed.trackError
                @@ ProverPresentationGeneratedToSent
                @@ Metric
                  .gauge("present_proof_flow_prover_presentation_generated_to_sent_flow_ms_gauge")
                  .trackDurationWith(_.toMetricsSeconds)

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
              val verifierPresentationReceivedToProcessed =
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
                        _ <- ZIO.fromEither(maybePresentationOptions.map {
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
                  presReceivedToProcessedAspect = CustomMetricsAspect.endRecordingTime(
                    s"${record.id}_present_proof_flow_verifier_presentation_received_to_verification_success_or_failure_ms_gauge",
                    "present_proof_flow_verifier_presentation_received_to_verification_success_or_failure_ms_gauge"
                  )
                  _ <- credentialsValidationResult match {
                    case Success(log, value) => service.markPresentationVerified(id) @@ presReceivedToProcessedAspect
                    case Failure(log, error) => {
                      for {
                        _ <- service.markPresentationVerificationFailed(id) @@ presReceivedToProcessedAspect
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
              verifierPresentationReceivedToProcessed
                @@ VerifierPresentationReceivedToProcessedSuccess.trackSuccess
                @@ VerifierPresentationReceivedToProcessedFailed.trackError
                @@ VerifierPresentationReceivedToProcessed
                @@ Metric
                  .gauge(
                    "present_proof_flow_verifier_presentation_received_to_verification_success_or_failure_flow_ms_gauge"
                  )
                  .trackDurationWith(_.toMetricsSeconds)

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
  ): ZIO[ManagedDIDService & WalletAccessContext, KeyNotFoundError, ZLayer[Any, Nothing, DidAgent]] = {
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
