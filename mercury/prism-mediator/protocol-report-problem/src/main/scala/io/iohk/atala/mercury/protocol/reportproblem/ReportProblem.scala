package io.iohk.atala.mercury.protocol.reportproblem

import io.iohk.atala.mercury.model.Message

/** ReportProblem
  *
  * Example:
  * @see
  *   https://github.com/hyperledger/aries-rfcs/tree/main/features/0035-report-problem#the-problem-report-message-type
  *
  * {{{
  * {
  *   "@type"            : "https://didcomm.org/report-problem/1.0/problem-report",
  *   "@id"              : "an identifier that can be used to discuss this error message",
  *   "~thread"          : "info about the threading context in which the error occurred (if any)",
  *   "description"      : { "en": "localized message", "code": "symbolic-name-for-error" },
  *   "problem_items"    : [ {"<item descrip>": "value"} ],
  *   "who_retries"      : "enum: you | me | both | none",
  *   "fix_hint"         : { "en": "localized error-instance-specific hint of how to fix issue"},
  *   "impact"           : "enum: message | thread | connection",
  *   "where"            : "enum: you | me | other - enum: cloud | edge | wire | agency | ..",
  *   "noticed_time"     : "<time>",
  *   "tracking_uri"     : "",
  *   "escalation_uri"   : ""
  * }
  * }}}
  */
final case class ReportProblem(
    // `@type`: String,
    `@id`: Option[String] = None,
    `~thread`: Option[String] = None,
    description: Description,
    problem_items: Option[Map[ItemDescrip, String]] = None, // key/value - TODO does `value` here a String?
    who_retries: Option[String] = None,
    fix_hint: Option[String] = None,
    impact: Option[Impact] = None,
    where: Option[Where] = None,
    noticed_time: Option[ISO8601UTC] = None,
    tracking_uri: Option[URI] = None,
    escalation_ur: Option[URI] = None,
) {
  // assert(`@type` == "https://didcomm.org/report-problem/1.0/problem-report") // this is something for the parser TODO
  def `@type` = "https://didcomm.org/report-problem/1.0/problem-report"
}

object ReportProblem {
  def toMessage(obj: ReportProblem): Message = ??? // TODO FIXME
  def fromMessage(msg: Message): ReportProblem = ??? // TODO FIXME
}

final case class Description( // TODO this will be +- a Map
    en: Option[String] = None,
    code: String
)

type ItemDescrip = String

enum WhoRetries {
  case you extends WhoRetries
  case me extends WhoRetries
  case both extends WhoRetries
  case none extends WhoRetries
}

enum Impact {
  case message extends Impact
  case thread extends Impact
  case connection extends Impact
}

type Where = String
// enum Where {
//   case you extends Where
//   case me extends Where
//   case other extends Where // FIXME enum: cloud | edge | wire | agency | ..
// }

type ISO8601UTC = String
type URI = String
