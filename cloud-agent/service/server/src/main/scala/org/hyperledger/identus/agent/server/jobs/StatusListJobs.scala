package org.hyperledger.identus.agent.server.jobs

import org.hyperledger.identus.agent.server.config.AppConfig
import org.hyperledger.identus.castor.core.model.did.VerificationRelationship
import org.hyperledger.identus.mercury.*
import org.hyperledger.identus.mercury.protocol.revocationnotificaiton.RevocationNotification
import org.hyperledger.identus.pollux.core.service.{CredentialService, CredentialStatusListService}
import org.hyperledger.identus.pollux.vc.jwt.revocation.{VCStatusList2021, VCStatusList2021Error}
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*
import zio.metrics.Metric
import org.hyperledger.identus.shared.utils.DurationOps.toMetricsSeconds

object StatusListJobs extends BackgroundJobsHelper {

  val syncRevocationStatuses =
    for {
      credentialStatusListService <- ZIO.service[CredentialStatusListService]
      credentialService <- ZIO.service[CredentialService]
      credentialStatusListsWithCreds <- credentialStatusListService.getCredentialsAndItsStatuses
        .mapError(_.toThrowable) @@ Metric
        .gauge("revocation_status_list_sync_get_status_lists_w_creds_ms_gauge")
        .trackDurationWith(_.toMetricsSeconds)

      updatedVcStatusListsCredsEffects = credentialStatusListsWithCreds.map { statusListWithCreds =>
        val vcStatusListCredString = statusListWithCreds.statusListCredential
        val walletAccessContext = WalletAccessContext(statusListWithCreds.walletId)

        val effect = for {
          vcStatusListCredJson <- ZIO
            .fromEither(io.circe.parser.parse(vcStatusListCredString))
            .mapError(_.underlying)
          issuer <- createJwtIssuer(statusListWithCreds.issuer, VerificationRelationship.AssertionMethod)
          vcStatusListCred <- VCStatusList2021
            .decodeFromJson(vcStatusListCredJson, issuer)
            .mapError(x => new Throwable(x.msg))
          bitString <- vcStatusListCred.getBitString.mapError(x => new Throwable(x.msg))
          updateBitStringEffects = statusListWithCreds.credentials.map { cred =>
            if cred.isCanceled then {
              val sendMessageEffect = for {
                maybeIssueCredentialRecord <- credentialService
                  .getIssueCredentialRecord(cred.issueCredentialRecordId)
                  .mapError(_.toThrowable)
                issueCredentialRecord <- ZIO
                  .fromOption(maybeIssueCredentialRecord)
                  .mapError(_ =>
                    new Throwable(s"Issue credential record not found by id: ${cred.issueCredentialRecordId}")
                  )
                issueCredentialData <- ZIO
                  .fromOption(issueCredentialRecord.issueCredentialData)
                  .mapError(_ =>
                    new Throwable(
                      s"Issue credential data not found in issue credential record by id: ${cred.issueCredentialRecordId}"
                    )
                  )
                issueCredentialProtocolThreadId <- ZIO
                  .fromOption(issueCredentialData.thid)
                  .mapError(_ => new Throwable("thid not found in issue credential data"))
                revocationNotification = RevocationNotification.build(
                  issueCredentialData.from,
                  issueCredentialData.to,
                  issueCredentialProtocolThreadId = issueCredentialProtocolThreadId
                )
                didCommAgent <- buildDIDCommAgent(issueCredentialData.from)
                response <- MessagingService
                  .send(revocationNotification.makeMessage)
                  .provideSomeLayer(didCommAgent) @@ Metric
                  .gauge("revocation_status_list_sync_revocation_notification_ms_gauge")
                  .trackDurationWith(_.toMetricsSeconds)
              } yield response

              val updateBitStringEffect = bitString.setRevokedInPlace(cred.statusListIndex, true)

              val updateAndNotify = for {
                updated <- updateBitStringEffect.mapError(x => new Throwable(x.message))
                _ <-
                  if !cred.isProcessed then
                    sendMessageEffect.flatMap { resp =>
                      if (resp.status >= 200 && resp.status < 300)
                        ZIO.logInfo("successfully sent revocation notification message")
                      else ZIO.logError(s"failed to send revocation notification message")
                    }
                  else ZIO.unit
              } yield updated
              updateAndNotify.provideSomeLayer(ZLayer.succeed(walletAccessContext)) @@ Metric
                .gauge("revocation_status_list_sync_process_single_credential_ms_gauge")
                .trackDurationWith(_.toMetricsSeconds)
            } else ZIO.unit
          }
          _ <- ZIO
            .collectAll(updateBitStringEffects)

          unprocessedEntityIds = statusListWithCreds.credentials.collect {
            case x if !x.isProcessed && x.isCanceled => x.id
          }
          _ <- credentialStatusListService
            .markAsProcessedMany(unprocessedEntityIds)
            .mapError(_.toThrowable) @@ Metric
            .gauge("revocation_status_list_sync_mark_as_processed_many_ms_gauge")
            .trackDurationWith(_.toMetricsSeconds)

          updatedVcStatusListCred <- vcStatusListCred.updateBitString(bitString).mapError {
            case VCStatusList2021Error.EncodingError(msg: String) => new Throwable(msg)
            case VCStatusList2021Error.DecodingError(msg: String) => new Throwable(msg)
          }
          vcStatusListCredJsonString <- updatedVcStatusListCred.toJsonWithEmbeddedProof
            .map(_.spaces2)
          _ <- credentialStatusListService
            .updateStatusListCredential(statusListWithCreds.id, vcStatusListCredJsonString)
            .mapError(_.toThrowable)
        } yield ()

        effect.provideSomeLayer(ZLayer.succeed(walletAccessContext))

      }
      config <- ZIO.service[AppConfig]
      _ <- (ZIO
        .collectAll(updatedVcStatusListsCredsEffects) @@ Metric
        .gauge("revocation_status_list_sync_process_status_lists_w_creds_ms_gauge")
        .trackDurationWith(_.toMetricsSeconds))
        .withParallelism(config.pollux.syncRevocationStatusesBgJobProcessingParallelism)
    } yield ()
}
