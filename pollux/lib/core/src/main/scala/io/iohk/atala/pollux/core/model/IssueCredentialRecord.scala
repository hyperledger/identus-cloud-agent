package io.iohk.atala.pollux.core.model

import java.util.UUID

final case class IssueCredentialRecord(
    id: UUID,
    schemaId: String,
    subjectId: String,
    validityPeriod: Option[Double] = None,
    claims: Map[String, String],
    state: IssueCredentialRecord.State
)

object IssueCredentialRecord {

  enum State:    
    case OfferSent extends State
    case OfferReceived extends State
    case RequestSent extends State
    case RequestReceived extends State
    case CredentialIssued extends State
    case CredentialReceived extends State
}
