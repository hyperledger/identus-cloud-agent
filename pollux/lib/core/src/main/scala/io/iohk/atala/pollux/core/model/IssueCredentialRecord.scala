package io.iohk.atala.pollux.core.model

import io.iohk.atala.castor.core.model.did.CanonicalPrismDID
import io.iohk.atala.mercury.protocol.issuecredential.{IssueCredential, OfferCredential, RequestCredential}
import io.iohk.atala.pollux.core.model.IssueCredentialRecord.*

import java.time.Instant

final case class IssueCredentialRecord(
    id: DidCommID,
    createdAt: Instant,
    updatedAt: Option[Instant],
    thid: DidCommID,
    schemaId: Option[String],
    role: Role,
    subjectId: Option[String],
    validityPeriod: Option[Double] = None,
    automaticIssuance: Option[Boolean],
    awaitConfirmation: Option[Boolean],
    protocolState: ProtocolState,
    publicationState: Option[PublicationState],
    offerCredentialData: Option[OfferCredential],
    requestCredentialData: Option[RequestCredential],
    issueCredentialData: Option[IssueCredential],
    issuedCredentialRaw: Option[String],
    issuingDID: Option[CanonicalPrismDID],
    metaRetries: Int,
    metaNextRetry: Option[Instant],
    metaLastFailure: Option[String],
)
final case class ValidIssuedCredentialRecord(
    id: DidCommID,
    issuedCredentialRaw: Option[String],
    subjectId: Option[String]
)

object IssueCredentialRecord {

  enum Role:
    case Issuer extends Role
    case Holder extends Role

  enum ProtocolState:
    // Issuer has created an offer in a database, but it has not been sent yet (in Issuer DB)
    case OfferPending extends ProtocolState
    // Issuer has sent an offer to a holder (in Issuer DB)
    case OfferSent extends ProtocolState
    // Holder has received an offer (In Holder DB)
    case OfferReceived extends ProtocolState

    // Holder has reviewed and approved the offer (in Holder DB)
    case RequestPending extends ProtocolState
    // Holder has generated the request that that includes the subjectID and proofs (in Holder DB)
    case RequestGenerated extends ProtocolState
    // Holder has sent a request to a an Issuer (in Holder DB)
    case RequestSent extends ProtocolState
    // Issuer has received a request from the holder (In Issuer DB)
    case RequestReceived extends ProtocolState

    // Holder declined the offer sent by Issuer (Holder DB) or Issuer declined the proposal sent by Holder (Issuer DB)
    case ProblemReportPending extends ProtocolState
    // Holder has sent problem report to Issuer (Holder DB) or Issuer has sent problem report to Holder (Issuer DB)
    case ProblemReportSent extends ProtocolState
    // Holder has received problem resport from Issuer (Holder DB) or Issuer has received problem report from Holder (Issuer DB)
    case ProblemReportReceived extends ProtocolState

    // Issuer has "accepted" a credential request received from a Holder (Issuer DB)
    case CredentialPending extends ProtocolState
    // Issuer has generated (signed) the credential and is now ready to send it to the Holder (Issuer DB)
    case CredentialGenerated extends ProtocolState
    // The credential has been sent to the holder (In Issuer DB)
    case CredentialSent extends ProtocolState
    // Holder has received the credential (In Holder DB)
    case CredentialReceived extends ProtocolState

  enum PublicationState:
    // The credential requires on-chain publication and should therefore be included in the next Merkle Tree computation/publication
    case PublicationPending extends PublicationState
    // The credential publication operation has been successfully sent to Iris and is pending publication
    case PublicationQueued extends PublicationState
    // The credential publication has been confirmed by Iris
    case Published extends PublicationState
}
