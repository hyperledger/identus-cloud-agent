package io.iohk.atala.issue.controller.http

/**
 * A request to accept a credential offer received from an issuer.
 *
 * @param subjectId The short-form subject Prism DID to which the verifiable credential should be issued. for example: ''did:prism:3bb0505d13fcb04d28a48234edb27b0d4e6d7e18a81e2c1abab58f3bbc21ce6f''
*/
final case class AcceptCredentialOfferRequest(
  subjectId: String
)
