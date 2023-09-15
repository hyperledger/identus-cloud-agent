package io.iohk.atala.agent.server.jobs

import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.server.jobs.BackgroundJobError.ErrorResponseReceivedFromPeerAgent
import io.iohk.atala.agent.walletapi.model.error.DIDSecretStorageError
import io.iohk.atala.agent.walletapi.model.error.DIDSecretStorageError.KeyNotFoundError
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.ConnectionRecord.*
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.connect.core.service.ConnectionService
import io.iohk.atala.mercury.*
import io.iohk.atala.mercury.model.*
import io.iohk.atala.mercury.model.error.*
import io.iohk.atala.resolvers.DIDResolver
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.utils.DurationOps.toMetricsSeconds
import io.iohk.atala.shared.utils.aspects.CustomMetricsAspect
import zio.*
import zio.metrics.*

object ConnectBackgroundJobs {

  val didCommExchanges = {
    for {
      connectionService <- ZIO.service[ConnectionService]
      config <- ZIO.service[AppConfig]
      records <- connectionService
        .getConnectionRecordsByStates(
          ignoreWithZeroRetries = true,
          limit = config.connect.connectBgJobRecordsLimit,
          ConnectionRecord.ProtocolState.ConnectionRequestPending,
          ConnectionRecord.ProtocolState.ConnectionResponsePending
        )
        .mapError(err => Throwable(s"Error occurred while getting connection records: $err"))
      _ <- ZIO.foreachPar(records)(performExchange).withParallelism(config.connect.connectBgJobProcessingParallelism)
    } yield ()
  }

  private[this] def performExchange(
      record: ConnectionRecord
  ): URIO[
    DidOps & DIDResolver & HttpClient & ConnectionService & ManagedDIDService & WalletAccessContext & AppConfig,
    Unit
  ] = {
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

          didCommAgent <- buildDIDCommAgent(request.from)
          resp <- MessagingService.send(request.makeMessage).provideSomeLayer(didCommAgent) @@ Metric
            .gauge("connection_flow_invitee_send_connection_request_ms_gauge")
            .trackDurationWith(_.toMetricsSeconds)
          connectionService <- ZIO.service[ConnectionService]
          _ <- {
            if (resp.status >= 200 && resp.status < 300)
              connectionService.markConnectionRequestSent(id)
                @@ InviteeConnectionRequestMsgSuccess
                @@ CustomMetricsAspect.endRecordingTime(
                  s"${record.id}_invitee_pending_to_req_sent",
                  "connection_flow_invitee_pending_to_req_sent_ms_gauge"
                )
            else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp)) @@ InviteeConnectionRequestMsgFailed
          }
        } yield ()

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
          didCommAgent <- buildDIDCommAgent(response.from)
          resp <- MessagingService.send(response.makeMessage).provideSomeLayer(didCommAgent) @@ Metric
            .gauge("connection_flow_inviter_send_connection_response_ms_gauge")
            .trackDurationWith(_.toMetricsSeconds)
          connectionService <- ZIO.service[ConnectionService]
          _ <- {
            if (resp.status >= 200 && resp.status < 300)
              connectionService.markConnectionResponseSent(id)
                @@ InviterConnectionResponseMsgSuccess
                @@ CustomMetricsAspect.endRecordingTime(
                  s"${record.id}_inviter_pending_to_res_sent",
                  "connection_flow_inviter_pending_to_res_sent_ms_gauge"
                )
            else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp)) @@ InviterConnectionResponseMsgFailed
          }
        } yield ()
        inviterProcessFlow
          @@ InviterProcessConnectionRecordPendingSuccess.trackSuccess
          @@ InviterProcessConnectionRecordPendingFailed.trackError
          @@ InviterProcessConnectionRecordPendingTotal
          @@ Metric
            .gauge("connection_flow_inviter_process_connection_record_ms_gauge")
            .trackDurationWith(_.toMetricsSeconds)
      case _ => ZIO.unit
    }

    exchange
      .tapError(e =>
        for {
          connectService <- ZIO.service[ConnectionService]
          _ <- connectService
            .reportProcessingFailure(record.id, Some(e.toString))
            .tapError(err =>
              ZIO.logErrorCause(
                s"Connect - failed to report processing failure: ${record.id}",
                Cause.fail(err)
              )
            )
        } yield ()
      )
      .catchAll(e => ZIO.logErrorCause(s"Connect - Error processing record: ${record.id} ", Cause.fail(e)))
      .catchAllDefect(d => ZIO.logErrorCause(s"Connect - Defect processing record: ${record.id}", Cause.fail(d)))
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

}
