package io.iohk.atala.issue.controller.http

/**
 * @param self The reference to the connection collection itself. for example: ''https://atala-prism-products.io/dids''
 * @param kind The type of object returned. In this case a `Collection`. for example: ''Collection''
 * @param pageOf Page number within the context of paginated response. for example: ''null''
 * @param next URL of the next page (if available) for example: ''null''
 * @param previous URL of the previous page (if available) for example: ''null''
 * @param contents  for example: ''null''
*/
final case class IssueCredentialRecordPage (
  self: String,
  kind: String,
  pageOf: String,
  next: Option[String] = None,
  previous: Option[String] = None,
  contents: Seq[IssueCredentialRecord]
)
