package io.iohk.atala.issue.controller.http

/**
 * @param issuingDID The issuer DID of the verifiable credential object. for example: ''did:prism:issuerofverifiablecredentials''
 * @param connectionId The unique identifier of a DIDComm connection that already exists between the issuer and the holder, and that will be used to execute the issue credential protocol. for example: ''null''
*/
final case class CreateIssueCredentialRecordRequestAllOf (
  issuingDID: String,
  connectionId: String
)
