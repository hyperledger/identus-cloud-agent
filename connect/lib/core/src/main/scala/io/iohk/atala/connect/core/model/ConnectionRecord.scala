package io.iohk.atala.connect.core.model

import io.iohk.atala.connect.core.model.ConnectionRecord.{ProtocolState, Role}

import java.util.UUID
import java.time.Instant

case class ConnectionRecord(
    id: UUID,
    createdAt: Instant,
    updatedAt: Option[Instant],
    thid: Option[UUID],
    label: Option[String],
    role: Role,
    protocolState: ProtocolState,
    invitation: Invitation,
    connectionRequest: Option[ConnectionRequest],
    connectionResponse: Option[ConnectionResponse]
)

object ConnectionRecord {
  enum Role:
    case Inviter extends Role
    case Invitee extends Role

  enum ProtocolState:
    // Inviter has created an Invitation in a database
    case InvitationGenerated extends ProtocolState

    // Invitee has received an offer (In Holder DB)
    case InvitationReceived extends ProtocolState

    //  Invitee has created a Connection Request to Inviter
    case ConnectionRequestPending extends ProtocolState

    // Invitee has sent a request to Inviter
    case ConnectionRequestSent extends ProtocolState

    // Inviter has received a request from Invitee
    case ConnectionRequestReceived extends ProtocolState

    // Inviter has created a Connection response for Invitee
    case ConnectionResponsePending extends ProtocolState

    // Inviter has sent Connection response for Invitee
    case ConnectionResponseSent extends ProtocolState

    // Invitee has received a ConnectionResponse from Inviter
    case ConnectionResponseReceived extends ProtocolState

    // TODO Ack to the connection response to finalise connection

    // TODO DID Rotation later

    // Holder declined the offer sent by Issuer (Holder DB) or Issuer declined the proposal sent by Holder (Issuer DB)
    case ProblemReportPending extends ProtocolState
    // Holder has sent problem report to Issuer (Holder DB) or Issuer has sent problem report to Holder (Issuer DB)
    case ProblemReportSent extends ProtocolState
    // Holder has received problem resport from Issuer (Holder DB) or Issuer has received problem report from Holder (Issuer DB)
    case ProblemReportReceived extends ProtocolState

}
