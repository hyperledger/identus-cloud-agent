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
  ): URIO[DidOps & DIDResolver & HttpClient & ConnectionService & ManagedDIDService, Unit] = {
    import ProtocolState.*
    import Role.*

    val InviteeConnectionRequestMsgFailed =
      Metric
        .counterInt("connection_flow_invitee_connection_request_msg_failed_count")
        .fromConst(1)
        .tagged("connectionId", record.id.toString)

    val InviteeConnectionRequestMsgSuccess = Metric
      .counterInt("connection_flow_invitee_connection_request_msg_success_count")
      .fromConst(1)
      .tagged("connectionId", record.id.toString)

    val InviterConnectionResponseMsgFailed =
      Metric
        .counterInt("connection_flow_inviter_connection_response_msg_failed_count")
        .fromConst(1)
        .tagged("connectionId", record.id.toString)

    val InviterConnectionResponseMsgSuccess =
      Metric
        .counterInt("connection_flow_inviter_connection_response_msg_success_count")
        .fromConst(1)
        .tagged("connectionId", record.id.toString)

    val ProcessConnectionRecordInviteePendingSuccess =
      Metric
        .counterInt("connection_flow_invitee_process_connection_record_success_count")
        .fromConst(1)
        .tagged("connectionId", record.id.toString)

    val ProcessConnectionRecordInviteePendingFailed =
      Metric
        .counterInt("connection_flow_invitee_process_connection_record_failed_count")
        .fromConst(1)
        .tagged("connectionId", record.id.toString)

    val ProcessConnectionRecordInviteePendingTotal = Metric
      .counterInt("connection_flow_invitee_process_connection_record_total_count")
      .fromConst(1)
      .tagged("connectionId", record.id.toString)

    val ProcessConnectionRecordInviterPendingSuccess =
      Metric
        .counterInt("connection_flow_inviter_process_connection_record_success_count")
        .fromConst(1)
        .tagged("connectionId", record.id.toString)

    val ProcessConnectionRecordInviterPendingFailed =
      Metric
        .counterInt("connection_flow_inviter_process_connection_record_failed_count")
        .fromConst(1)
        .tagged("connectionId", record.id.toString)

    val ProcessConnectionRecordInviterPendingTotal = Metric
      .counterInt("connection_flow_inviter_process_connection_record_total_count")
      .fromConst(1)
      .tagged("connectionId", record.id.toString)

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
            .gauge("connection_flow_invitee_send_connection_request_ms")
            .tagged("connectionId", record.id.toString)
            .trackDurationWith(_.toNanos.toDouble)
          connectionService <- ZIO.service[ConnectionService]
          _ <- {
            if (resp.status >= 200 && resp.status < 300)
              connectionService.markConnectionRequestSent(id)
                @@ InviteeConnectionRequestMsgSuccess
                @@ CustomMetricsAspect.createGaugeAfter(s"${record.id}_invitee_pending_to_req_sent", "connection_flow_invitee_pending_to_req_sent_ms", Set(
                MetricLabel(
                  "connectionId", record.id.toString
                )
              ))
            else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp)) @@ InviteeConnectionRequestMsgFailed
          }
        } yield ()

        // inviteeProcessFlow // TODO decrease metaRetries if it has a error

        inviteeProcessFlow
          @@ ProcessConnectionRecordInviteePendingSuccess.trackSuccess
          @@ ProcessConnectionRecordInviteePendingFailed.trackError
          @@ ProcessConnectionRecordInviteePendingTotal
          @@ Metric
            .gauge("connection_flow_invitee_process_connection_record_ms")
            .tagged("connectionId", record.id.toString)
            .trackDurationWith(_.toNanos.toDouble)

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
            .gauge("connection_flow_inviter_send_connection_response_ms")
            .tagged("connectionId", record.id.toString)
            .trackDurationWith(_.toNanos.toDouble)
          connectionService <- ZIO.service[ConnectionService]
          _ <- {
            if (resp.status >= 200 && resp.status < 300)
              connectionService.markConnectionResponseSent(id) @@ InviterConnectionResponseMsgSuccess
            else ZIO.fail(ErrorResponseReceivedFromPeerAgent(resp)) @@ InviterConnectionResponseMsgFailed
          }
        } yield ()

        inviterProcessFlow
          @@ ProcessConnectionRecordInviterPendingSuccess.trackSuccess
          @@ ProcessConnectionRecordInviterPendingFailed.trackError
          @@ ProcessConnectionRecordInviterPendingTotal
          @@ Metric
            .gauge("connection_flow_inviter_process_connection_record_ms")
            .tagged("connectionId", record.id.toString)
            .trackDurationWith(_.toNanos.toDouble)

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
  ): ZIO[ManagedDIDService, KeyNotFoundError, ZLayer[Any, Nothing, DidAgent]] = {
    for {
      managedDidService <- ZIO.service[ManagedDIDService]
      peerDID <- managedDidService.getPeerDID(myDid)
      agent = AgentPeerService.makeLayer(peerDID)
    } yield agent
  }

}
