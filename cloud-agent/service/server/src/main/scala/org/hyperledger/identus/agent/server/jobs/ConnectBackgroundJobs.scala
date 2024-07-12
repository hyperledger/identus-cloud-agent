package org.hyperledger.identus.agent.server.jobs

import org.hyperledger.identus.agent.server.config.AppConfig
import org.hyperledger.identus.agent.server.jobs.BackgroundJobError.ErrorResponseReceivedFromPeerAgent
import org.hyperledger.identus.agent.walletapi.service.ManagedDIDService
import org.hyperledger.identus.agent.walletapi.storage.DIDNonSecretStorage
import org.hyperledger.identus.connect.core.model.{ConnectionRecord, WalletIdAndRecordId}
import org.hyperledger.identus.connect.core.model.ConnectionRecord.*
import org.hyperledger.identus.connect.core.service.ConnectionService
import org.hyperledger.identus.mercury.*
import org.hyperledger.identus.messaging.{ByteArrayWrapper, Message, Producer}
import org.hyperledger.identus.resolvers.DIDResolver
import org.hyperledger.identus.shared.models.{WalletAccessContext, WalletId}
import org.hyperledger.identus.shared.utils.aspects.CustomMetricsAspect
import org.hyperledger.identus.shared.utils.DurationOps.toMetricsSeconds
import zio.*
import zio.metrics.*

import java.time.Instant
import java.util.UUID

object ConnectBackgroundJobs extends BackgroundJobsHelper {

  private val CONNECT_TOPIC = "connect"
  private val CONNECT_RETRY_TOPIC = "connect-retry"
  private val CONNECT_RETRY_BACKOFF = 5.seconds

  def handleMessage(message: Message[UUID, WalletIdAndRecordId]): URIO[
    DidOps & DIDResolver & HttpClient & ConnectionService & ManagedDIDService & DIDNonSecretStorage & AppConfig &
      Producer[UUID, WalletIdAndRecordId],
    Unit
  ] = {
    for {
      _ <- ZIO.logInfo(s"!!! Handling recordId: ${message.value} via Kafka queue")
      connectionService <- ZIO.service[ConnectionService]
      walletAccessContext = WalletAccessContext(WalletId.fromUUID(message.value.walletId))
      record <- connectionService
        .findRecordById(message.value.recordId)
        .provideSome(ZLayer.succeed(walletAccessContext))
        .someOrElseZIO(ZIO.dieMessage("Record Not Found"))
      _ <- performExchange(record)
        .tapSomeError { case (walletAccessContext, errorResponse) =>
          for {
            connectService <- ZIO.service[ConnectionService]
            _ <- connectService
              .reportProcessingFailure(record.id, Some(errorResponse))
              .provideSomeLayer(ZLayer.succeed(walletAccessContext))
            _ <- ZIO.when(record.metaRetries > 0) {
              for {
                messageProducer <- ZIO.service[Producer[UUID, WalletIdAndRecordId]]
                _ <- messageProducer.produce(
                  CONNECT_RETRY_TOPIC,
                  message.key,
                  message.value
                )
              } yield ()
            }
          } yield ()
        }
        .catchAll { e => ZIO.logErrorCause(s"Connect - Error processing record: ${record.id} ", Cause.fail(e)) }
        .catchAllDefect(d => ZIO.logErrorCause(s"Connect - Defect processing record: ${record.id}", Cause.fail(d)))
    } yield ()
  }

  def handleRetry(
      message: Message[ByteArrayWrapper, ByteArrayWrapper]
  ): URIO[Producer[ByteArrayWrapper, ByteArrayWrapper], Unit] = {
    for {
      retryProducer <- ZIO.service[Producer[ByteArrayWrapper, ByteArrayWrapper]]
      _ <- ZIO.logInfo("Posting message to connect topic in 10 sec")
      millisSpentInQueue = Instant.now().toEpochMilli - message.timestamp
      sleepDelay = CONNECT_RETRY_BACKOFF.toMillis - millisSpentInQueue
      _ <- ZIO.when(sleepDelay > 0)(ZIO.sleep(Duration.fromMillis(sleepDelay)))
      _ <- retryProducer
        .produce(CONNECT_TOPIC, message.key, message.value)
        .catchAll(e => ZIO.logErrorCause(s"Error sending message to the $CONNECT_RETRY_TOPIC topic", Cause.fail(e)))
    } yield ()
  }

  private def performExchange(
      record: ConnectionRecord
  ) = {
    import ProtocolState.*
    import Role.*

    def counterMetric(key: String) = Metric
      .counterInt(key)
      .fromConst(1)

    val InviteeConnectionRequestMsgFailed = counterMetric(
      "connection_flow_invitee_connection_request_msg_failed_counter"
    )
    val InviteeConnectionRequestMsgSuccess = counterMetric(
      "connection_flow_invitee_connection_request_msg_success_counter"
    )
    val InviterConnectionResponseMsgFailed = counterMetric(
      "connection_flow_inviter_connection_response_msg_failed_counter"
    )
    val InviterConnectionResponseMsgSuccess = counterMetric(
      "connection_flow_inviter_connection_response_msg_success_counter"
    )
    val InviteeProcessConnectionRecordPendingSuccess = counterMetric(
      "connection_flow_invitee_process_connection_record_success_counter"
    )
    val InviteeProcessConnectionRecordPendingFailed = counterMetric(
      "connection_flow_invitee_process_connection_record_failed_counter"
    )
    val InviteeProcessConnectionRecordPendingTotal = counterMetric(
      "connection_flow_invitee_process_connection_record_all_counter"
    )
    val InviterProcessConnectionRecordPendingSuccess = counterMetric(
      "connection_flow_inviter_process_connection_record_success_counter"
    )
    val InviterProcessConnectionRecordPendingFailed = counterMetric(
      "connection_flow_inviter_process_connection_record_failed_counter"
    )
    val InviterProcessConnectionRecordPendingTotal = counterMetric(
      "connection_flow_inviter_process_connection_record_all_counter"
    )

    val exchange = record match {
      case ConnectionRecord(
            id,
            _,
            _,
            _,
            _,
            _,
            _,
            Invitee,
            ConnectionRequestPending,
            _,
            Some(request),
            _,
            metaRetries,
            _,
            _
          ) if metaRetries > 0 =>
        val inviteeProcessFlow = for {
          walletAccessContext <- buildWalletAccessContextLayer(request.from)
          result <- (for {
            didCommAgent <- buildDIDCommAgent(request.from).provideSomeLayer(ZLayer.succeed(walletAccessContext))
            resp <- MessagingService.send(request.makeMessage).provideSomeLayer(didCommAgent) @@ Metric
              .gauge("connection_flow_invitee_send_connection_request_ms_gauge")
              .trackDurationWith(_.toMetricsSeconds)
            connectionService <- ZIO.service[ConnectionService]
            _ <- {
              if (resp.status >= 200 && resp.status < 300)
                connectionService.markConnectionRequestSent(id).provideSomeLayer(ZLayer.succeed(walletAccessContext))
                  @@ InviteeConnectionRequestMsgSuccess
                  @@ CustomMetricsAspect.endRecordingTime(
                    s"${record.id}_invitee_pending_to_req_sent",
                    "connection_flow_invitee_pending_to_req_sent_ms_gauge"
                  )
              else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp)) @@ InviteeConnectionRequestMsgFailed
            }
          } yield ()).mapError(e => (walletAccessContext, e))
        } yield result

        // inviteeProcessFlow // TODO decrease metaRetries if it has a error

        inviteeProcessFlow
          @@ InviteeProcessConnectionRecordPendingSuccess.trackSuccess
          @@ InviteeProcessConnectionRecordPendingFailed.trackError
          @@ InviteeProcessConnectionRecordPendingTotal
          @@ Metric
            .gauge("connection_flow_invitee_process_connection_record_ms_gauge")
            .trackDurationWith(_.toMetricsSeconds)

      case ConnectionRecord(
            id,
            _,
            _,
            _,
            _,
            _,
            _,
            Inviter,
            ConnectionResponsePending,
            _,
            _,
            Some(response),
            metaRetries,
            _,
            _
          ) if metaRetries > 0 =>
        val inviterProcessFlow = for {
          walletAccessContext <- buildWalletAccessContextLayer(response.from)
          result <- (for {
            didCommAgent <- buildDIDCommAgent(response.from).provideSomeLayer(ZLayer.succeed(walletAccessContext))
            resp <- MessagingService.send(response.makeMessage).provideSomeLayer(didCommAgent) @@ Metric
              .gauge("connection_flow_inviter_send_connection_response_ms_gauge")
              .trackDurationWith(_.toMetricsSeconds)
            connectionService <- ZIO.service[ConnectionService]
            _ <- {
              if (resp.status >= 200 && resp.status < 300)
                connectionService.markConnectionResponseSent(id).provideSomeLayer(ZLayer.succeed(walletAccessContext))
                  @@ InviterConnectionResponseMsgSuccess
                  @@ CustomMetricsAspect.endRecordingTime(
                    s"${record.id}_inviter_pending_to_res_sent",
                    "connection_flow_inviter_pending_to_res_sent_ms_gauge"
                  )
              else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp)) @@ InviterConnectionResponseMsgFailed
            }
          } yield ()).mapError(e => (walletAccessContext, e))
        } yield result

        inviterProcessFlow
          @@ InviterProcessConnectionRecordPendingSuccess.trackSuccess
          @@ InviterProcessConnectionRecordPendingFailed.trackError
          @@ InviterProcessConnectionRecordPendingTotal
          @@ Metric
            .gauge("connection_flow_inviter_process_connection_record_ms_gauge")
            .trackDurationWith(_.toMetricsSeconds)
      case r => ZIO.logWarning(s"Invalid candidate record received for processing: $r") *> ZIO.unit
    }

    exchange
  }

}
