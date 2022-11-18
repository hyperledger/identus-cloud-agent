package io.iohk.atala.connect.core.model

case class ConnectionRecord(
    id: UUID,
    thid: UUID,
    schemaId: Option[String],
    role: Role,
    subjectId: String,
    validityPeriod: Option[Double] = None,
    claims: Map[String, String],
    protocolState: ProtocolState,
    publicationState: Option[PublicationState],
    offerCredentialData: Option[OfferCredential],
    requestCredentialData: Option[RequestCredential],
    issueCredentialData: Option[IssueCredential]
)

object ConnectionRecord {
  enum Role:
    case Inviter extends Role
    case Invitee extends Role

  enum ProtocolState:
    // Inviter has created an Invitation in a database
    case InvitationSent extends ProtocolState

    // Invitee has received an offer (In Holder DB)
    case InvitationReceived extends ProtocolState

    //  Ask
    case RequestPending extends ProtocolState

    // Invitee has sent a request to Inviter
    case ConnectionRequestSent extends ProtocolState
    // Inviter has received a request from Invitee
    case ConnectionRequestReceived extends ProtocolState

    // Inviter has received a request from the Invitee
    case ConnectionResponseSent extends ProtocolState
    // Invitee has received a ConnectionResponse from Inviter
    case ConnectionResponseReceived extends ProtocolState

    // Holder declined the offer sent by Issuer (Holder DB) or Issuer declined the proposal sent by Holder (Issuer DB)
    case ProblemReportPending extends ProtocolState
    // Holder has sent problem report to Issuer (Holder DB) or Issuer has sent problem report to Holder (Issuer DB)
    case ProblemReportSent extends ProtocolState
    // Holder has received problem resport from Issuer (Holder DB) or Issuer has received problem report from Holder (Issuer DB)
    case ProblemReportReceived extends ProtocolState

}
