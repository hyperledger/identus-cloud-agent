package io.iohk.atala.issue.controller.http

/**
 * @param contents  for example: ''null''
*/
final case class IssueCredentialRecordPageAllOf (
  contents: Seq[IssueCredentialRecord]
)
