package io.iohk.atala.pollux.core.model

import java.util.UUID

final case class IssueCredentialRecord(
    id: UUID,
    credentialId: UUID, // Id of the credential that will be created from this record
    schemaId: String,
    //role: IssueCredentialRecord.Role,
    subjectId: String,
    validityPeriod: Option[Double] = None,
    claims: Map[String, String],
    state: IssueCredentialRecord.State
)

object IssueCredentialRecord {

  enum Role:
    case Issuer extends Role
    case Holder extends Role

  enum State:
    // Issuer has created an offer in a database, but it has not been sent yet (in Issuer DB)
    case OfferPending extends State
    // Issuer has sent an offer to a holder (in Issuer DB)
    case OfferSent extends State
    // Holder has received an offer (In Holder DB)
    case OfferReceived extends State

    // Holder has reviewed and approved the offer (in Holder DB)
    case RequestPending extends State
    // Holder has sent a request to a an Issuer (in Holder DB)
    case RequestSent extends State
    // Issuer has received a request from the holder (In Issuer DB)
    case RequestReceived extends State

    // Holder declined the offer sent by Issuer (Holder DB) or Issuer declined the proposal sent by Holder (Issuer DB)
    case ProblemReportPending extends State
    // Holder has sent problem report to Issuer (Holder DB) or Issuer has sent problem report to Holder (Issuer DB)
    case ProblemReportSent extends State
    // Holder has received problem resport from Issuer (Holder DB) or Issuer has received problem report from Holder (Issuer DB)
    case ProblemReportReceived extends State

    // Issuer has "accepted" a credential request received from a Holder (Issuer DB)
    case CredentialPending extends State
    // The Issuer has issued (signed) a credential and sent it to Iris. Iris has not confirmed that is has been published on DLT yet (In Issuer DB)
    case CredentialPublishQueued extends State
    // The credential has been sent to the holder (In Issuer DB)
    case CredentialSent extends State
    // Iris has notified the Issuer that a credential that it has queued before has been published on DLT (In Issuer DB)
    case CredentialPublished extends State
    // Holder has received the credential (In Holder DB)
    case CredentialReceived extends State
}
