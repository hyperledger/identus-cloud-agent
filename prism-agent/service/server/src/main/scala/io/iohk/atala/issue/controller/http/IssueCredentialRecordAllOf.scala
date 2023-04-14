package io.iohk.atala.issue.controller.http

import java.time.OffsetDateTime

/**
 * @param recordId The unique identifier of the issue credential record. for example: ''null''
 * @param createdAt The date and time when the issue credential record was created. for example: ''null''
 * @param updatedAt The date and time when the issue credential record was last updated. for example: ''null''
 * @param role The role played by the Prism agent in the credential issuance flow. for example: ''null''
 * @param protocolState The current state of the issue credential protocol execution. for example: ''null''
 * @param jwtCredential The base64-encoded JWT verifiable credential that has been sent by the issuer. for example: ''null''
 * @param issuingDID Issuer DID of the verifiable credential object. for example: ''did:prism:issuerofverifiablecredentials''
*/
final case class IssueCredentialRecordAllOf (
  recordId: String,
  createdAt: OffsetDateTime,
  updatedAt: Option[OffsetDateTime] = None,
  role: String,
  protocolState: String,
  jwtCredential: Option[String] = None,
  issuingDID: Option[String] = None
)
