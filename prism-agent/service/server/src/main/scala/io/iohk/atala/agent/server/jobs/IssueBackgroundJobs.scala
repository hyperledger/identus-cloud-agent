package io.iohk.atala.agent.server.jobs

import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.server.jobs.BackgroundJobError.ErrorResponseReceivedFromPeerAgent
import io.iohk.atala.castor.core.model.did.*
import io.iohk.atala.mercury.*
import io.iohk.atala.mercury.protocol.issuecredential.*
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import io.iohk.atala.pollux.core.service.CredentialService
import io.iohk.atala.shared.utils.DurationOps.toMetricsSeconds
import io.iohk.atala.shared.utils.aspects.CustomMetricsAspect
import zio.*
import zio.metrics.*
import io.iohk.atala.agent.walletapi.model.error.DIDSecretStorageError.WalletNotFoundError

object IssueBackgroundJobs extends BackgroundJobsHelper {

  val issueCredentialDidCommExchanges = {
    for {
      credentialService <- ZIO.service[CredentialService]
      config <- ZIO.service[AppConfig]
      records <- credentialService
        .getIssueCredentialRecordsByStatesForAllWallets(
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

  private def counterMetric(key: String) = Metric
    .counterInt(key)
    .fromConst(1)

  private[this] def performIssueCredentialExchange(record: IssueCredentialRecord) = {
    import IssueCredentialRecord.*
    import IssueCredentialRecord.ProtocolState.*

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
              _,
              _,
              Role.Issuer,
              _,
              _,
              _,
              OfferPending,
              Some(offer),
              _,
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
            walletAccessContext <- buildWalletAccessContextLayer(offer.from)
            result <- (for {
              didCommAgent <- buildDIDCommAgent(offer.from).provideSomeLayer(ZLayer.succeed(walletAccessContext))
              resp <- MessagingService
                .send(offer.makeMessage)
                .provideSomeLayer(didCommAgent) @@ Metric
                .gauge("issuance_flow_issuer_send_offer_ms_gauge")
                .trackDurationWith(_.toMetricsSeconds)
              credentialService <- ZIO.service[CredentialService]
              _ <- {
                if (resp.status >= 200 && resp.status < 300)
                  credentialService.markOfferSent(id).provideSomeLayer(ZLayer.succeed(walletAccessContext)) @@
                    IssuerSendOfferMsgSucceed @@
                    CustomMetricsAspect.endRecordingTime(
                      s"${record.id}_issuer_offer_pending_to_sent_ms_gauge",
                      "issuance_flow_issuer_offer_pending_to_sent_ms_gauge"
                    )
                else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp)) @@ IssuerSendOfferMsgFailed
              }
            } yield ()).mapError(e => (walletAccessContext, e))
          } yield result

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
              _,
              CredentialFormat.JWT,
              Role.Holder,
              Some(subjectId),
              _,
              _,
              RequestPending,
              Some(offer),
              None,
              _,
              _,
              _,
              _,
              _,
              _,
              _
            ) =>
          val holderPendingToGeneratedFlow = for {
            walletAccessContext <- buildWalletAccessContextLayer(offer.to)
            result <- (for {
              credentialService <- ZIO.service[CredentialService]
              _ <- credentialService
                .generateJWTCredentialRequest(id)
                .provideSomeLayer(ZLayer.succeed(walletAccessContext))
            } yield ()).mapError(e => (walletAccessContext, handleCredentialErrors(e)))
          } yield result

          holderPendingToGeneratedFlow @@ HolderPendingToGeneratedSuccess.trackSuccess
            @@ HolderPendingToGeneratedFailed.trackError
            @@ HolderPendingToGeneratedAll
            @@ Metric
              .gauge("issuance_flow_holder_req_pending_to_generated_flow_ms_gauge")
              .trackDurationWith(_.toMetricsSeconds)

        case IssueCredentialRecord(
              id,
              _,
              _,
              _,
              _,
              _,
              CredentialFormat.AnonCreds,
              Role.Holder,
              None,
              _,
              _,
              RequestPending,
              Some(offer),
              None,
              _,
              _,
              _,
              _,
              _,
              _,
              _
            ) =>
          val holderPendingToGeneratedFlow = for {
            walletAccessContext <- buildWalletAccessContextLayer(offer.to)
            result <- (for {
              credentialService <- ZIO.service[CredentialService]
              _ <- credentialService
                .generateAnonCredsCredentialRequest(id)
                .provideSomeLayer(ZLayer.succeed(walletAccessContext))
            } yield ()).mapError(e => (walletAccessContext, handleCredentialErrors(e)))
          } yield result

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
              _,
              _,
              Role.Holder,
              _,
              _,
              _,
              RequestGenerated,
              _,
              Some(request),
              _,
              _,
              _,
              _,
              _,
              _,
              _
            ) =>
          val holderGeneratedToSentFlow = for {
            walletAccessContext <- buildWalletAccessContextLayer(request.from)
            result <- (for {
              didCommAgent <- buildDIDCommAgent(request.from).provideSomeLayer(ZLayer.succeed(walletAccessContext))
              resp <- MessagingService
                .send(request.makeMessage)
                .provideSomeLayer(didCommAgent) @@ Metric
                .gauge("issuance_flow_holder_send_request_ms_gauge")
                .trackDurationWith(_.toMetricsSeconds)
              credentialService <- ZIO.service[CredentialService]
              _ <- {
                if (resp.status >= 200 && resp.status < 300)
                  credentialService
                    .markRequestSent(id)
                    .provideSomeLayer(ZLayer.succeed(walletAccessContext)) @@ HolderSendReqSucceed
                    @@ CustomMetricsAspect.endRecordingTime(
                      s"${record.id}_issuance_flow_holder_req_generated_to_sent",
                      "issuance_flow_holder_req_generated_to_sent_ms_gauge"
                    )
                else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp)) @@ HolderSendReqFailed
              }
            } yield ()).mapError(e => (walletAccessContext, e))
          } yield result

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
              _,
              _,
              Role.Issuer,
              _,
              _,
              Some(true),
              RequestReceived,
              _,
              Some(request),
              _,
              _,
              _,
              _,
              _,
              _,
              _,
            ) =>
          val issuerReceivedToPendingFlow = for {
            walletAccessContext <- buildWalletAccessContextLayer(request.to)
            result <- (for {
              credentialService <- ZIO.service[CredentialService]
              _ <- credentialService.acceptCredentialRequest(id).provideSomeLayer(ZLayer.succeed(walletAccessContext))
            } yield ()).mapError(e => (walletAccessContext, e))
          } yield result

          issuerReceivedToPendingFlow @@ IssuerReceivedToPendingSuccess.trackSuccess
            @@ IssuerReceivedToPendingFailed.trackError
            @@ IssuerReceivedToPendingAll
            @@ Metric
              .gauge("issuance_flow_issuer_cred_received_to_pending_flow_ms_gauge")
              .trackDurationWith(_.toMetricsSeconds)

        // Credential is pending, can be generated by Issuer
        case IssueCredentialRecord(
              id,
              _,
              _,
              _,
              _,
              _,
              CredentialFormat.JWT,
              Role.Issuer,
              _,
              _,
              _,
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
          // TODO Move all logic to service
          val issuerPendingToGeneratedFlow = for {
            walletAccessContext <- buildWalletAccessContextLayer(issue.from)
            result <- (for {
              credentialService <- ZIO.service[CredentialService]
              _ <- credentialService.generateJWTCredential(id).provideSomeLayer(ZLayer.succeed(walletAccessContext))
            } yield ()).mapError(e => (walletAccessContext, e))
          } yield result

          issuerPendingToGeneratedFlow @@ IssuerPendingToGeneratedSuccess.trackSuccess
            @@ IssuerPendingToGeneratedFailed.trackError
            @@ IssuerPendingToGeneratedAll
            @@ Metric
              .gauge("issuance_flow_issuer_cred_pending_to_generated_flow_ms_gauge")
              .trackDurationWith(_.toMetricsSeconds)

        case IssueCredentialRecord(
              id,
              _,
              _,
              _,
              _,
              _,
              CredentialFormat.AnonCreds,
              Role.Issuer,
              _,
              _,
              _,
              CredentialPending,
              _,
              _,
              _,
              Some(issue),
              _,
              _,
              _,
              _,
              _,
            ) =>
          val issuerPendingToGeneratedFlow = for {
            walletAccessContext <- buildWalletAccessContextLayer(issue.from)
            result <- (for {
              credentialService <- ZIO.service[CredentialService]
              _ <- credentialService
                .generateAnonCredsCredential(id)
                .provideSomeLayer(ZLayer.succeed(walletAccessContext))
            } yield ()).mapError(e => (walletAccessContext, e))
          } yield result

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
              _,
              _,
              Role.Issuer,
              _,
              _,
              _,
              CredentialGenerated,
              _,
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
            walletAccessContext <- buildWalletAccessContextLayer(issue.from)
            result <- (for {
              didCommAgent <- buildDIDCommAgent(issue.from).provideSomeLayer(ZLayer.succeed(walletAccessContext))
              resp <- MessagingService
                .send(issue.makeMessage)
                .provideSomeLayer(didCommAgent) @@ Metric
                .gauge("issuance_flow_issuer_send_credential_msg_ms_gauge")
                .trackDurationWith(_.toMetricsSeconds)
              credentialService <- ZIO.service[CredentialService]
              _ <- {
                if (resp.status >= 200 && resp.status < 300)
                  credentialService
                    .markCredentialSent(id)
                    .provideSomeLayer(ZLayer.succeed(walletAccessContext)) @@ IssuerSendCredentialMsgSuccess
                else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp)) @@ IssuerSendCredentialMsgFailed
              }
            } yield ()).mapError(e => (walletAccessContext, e))
          } yield result

          sendCredentialManualIssuanceFlow @@ IssuerSendCredentialSuccess.trackSuccess
            @@ IssuerSendCredentialFailed.trackError
            @@ IssuerSendCredentialAll @@ Metric
              .gauge("issuance_flow_issuer_send_cred_flow_ms_gauge")
              .trackDurationWith(_.toMetricsSeconds)

        case _: IssueCredentialRecord => ZIO.unit
      }
    } yield ()

    aux
      .tapError(
        {
          case walletNotFound: WalletNotFoundError =>
            ZIO.logErrorCause(
              s"Issue Credential- Error processing record: ${record.id}",
              Cause.fail(walletNotFound)
            )
          case ((walletAccessContext, e)) =>
            for {
              credentialService <- ZIO.service[CredentialService]
              _ <- credentialService
                .reportProcessingFailure(record.id, Some(e.toString))
                .provideSomeLayer(ZLayer.succeed(walletAccessContext))
                .tapError(err =>
                  ZIO.logErrorCause(
                    s"Issue Credential - failed to report processing failure: ${record.id}",
                    Cause.fail(err)
                  )
                )
            } yield ()

        }
      )
      .catchAll(e => ZIO.logErrorCause(s"Issue Credential - Error processing record: ${record.id} ", Cause.fail(e)))
      .catchAllDefect(d =>
        ZIO.logErrorCause(s"Issue Credential - Defect processing record: ${record.id}", Cause.fail(d))
      )

  }

  private[this] def handleCredentialErrors
      : PartialFunction[Throwable | CredentialServiceError, CredentialServiceError] = {
    case e: CredentialServiceError => e
    case t: Throwable              => CredentialServiceError.UnexpectedError(t.getMessage())
  }

}
