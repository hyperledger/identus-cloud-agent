package io.iohk.atala.mercury.protocol.coordinatemediation

import io.iohk.atala.mercury.model.PIURI

import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.semiauto._

final case class MediateRequest(
    id: String = java.util.UUID.randomUUID.toString(),
    `type`: PIURI = MediateRequest.`type`
) { assert(`type` == MediateRequest.`type`) }
object MediateRequest {
  def `type`: PIURI = "https://didcomm.org/coordinate-mediation/2.0/mediate-request"
  given Encoder[MediateRequest] = deriveEncoder[MediateRequest]
  given Decoder[MediateRequest] = deriveDecoder[MediateRequest]
}

final case class MediateDeny(id: String, `type`: PIURI) { assert(`type` == MediateDeny.`type`) }
object MediateDeny {
  def `type`: PIURI = "https://didcomm.org/coordinate-mediation/2.0/mediate-deny"
  given Encoder[MediateDeny] = deriveEncoder[MediateDeny]
  given Decoder[MediateDeny] = deriveDecoder[MediateDeny]
}

final case class MediateGrant(id: String, `type`: PIURI, body: MediateGrant.Body) {
  assert(`type` == MediateGrant.`type`)
}
object MediateGrant {
  def `type`: PIURI = "https://didcomm.org/coordinate-mediation/2.0/mediate-grant"
  given Encoder[MediateGrant] = deriveEncoder[MediateGrant]
  given Decoder[MediateGrant] = deriveDecoder[MediateGrant]

  /** @param routing_did
    *   DID of the mediator where forwarded messages should be sent. The recipient may use this DID as an enpoint as
    *   explained in Using a DID as an endpoint section of the specification.
    *
    * FIXME https://github.com/roots-id/didcomm-mediator/issues/17
    */
  final case class Body(routing_did: String) // Seq[String])
}

final case class KeylistUpdate(id: String, `type`: PIURI, body: KeylistUpdate.Body) {
  assert(`type` == KeylistUpdate.`type`)
}
object KeylistUpdate {
  def `type`: PIURI = "https://didcomm.org/coordinate-mediation/2.0/keylist-update"
  given Encoder[KeylistUpdate] = deriveEncoder[KeylistUpdate]
  given Decoder[KeylistUpdate] = deriveDecoder[KeylistUpdate]
  given Encoder[Update] = deriveEncoder[Update]
  given Decoder[Update] = deriveDecoder[Update]

  final case class Body(updates: Seq[Update])

  /** @param recipient_did
    *   DID subject of the update.
    * @param action
    *   one of add or remove.
    */
  final case class Update(recipient_did: String, action: Action)
  enum Action:
    case add extends Action
    case remove extends Action
}

final case class KeylistResponse(id: String, `type`: PIURI, body: KeylistResponse.Body) {
  assert(`type` == KeylistResponse.`type`)
}
object KeylistResponse {
  def `type`: PIURI = "https://didcomm.org/coordinate-mediation/2.0/keylist-update-response"
  given Encoder[KeylistResponse] = deriveEncoder[KeylistResponse]
  given Decoder[KeylistResponse] = deriveDecoder[KeylistResponse]
  given Encoder[Update] = deriveEncoder[Update]
  given Decoder[Update] = deriveDecoder[Update]

  final case class Body(updated: Seq[Update])

  /** @param recipient_did
    *   DID subject of the update.
    * @param action
    *   one of add or remove.
    * @param result
    *   one of client_error, server_error, no_change, success; describes the resulting state of the keylist update.
    */
  final case class Update(recipient_did: String, action: Action, result: Result)
  enum Action:
    case add extends Action
    case remove extends Action
  enum Result:
    case client_error extends Result
    case server_error extends Result
    case no_change extends Result
    case success extends Result
}

final case class KeylistQuery(id: String, `type`: PIURI, body: KeylistQuery.Body) {
  assert(`type` == KeylistQuery.`type`)
}
object KeylistQuery {
  def `type`: PIURI = "https://didcomm.org/coordinate-mediation/2.0/keylist-query"
  given Encoder[KeylistQuery] = deriveEncoder[KeylistQuery]
  given Decoder[KeylistQuery] = deriveDecoder[KeylistQuery]

  final case class Body(paginate: Option[Paginate] = None)
  final case class Paginate(limit: Int, offset: Int)
}

final case class Keylist(id: String, `type`: PIURI, body: Keylist.Body) {
  assert(`type` == Keylist.`type`)
}
object Keylist {
  def `type`: PIURI = "https://didcomm.org/coordinate-mediation/2.0/keylist"
  given Encoder[Keylist] = deriveEncoder[Keylist]
  given Decoder[Keylist] = deriveDecoder[Keylist]
  given Encoder[Key] = deriveEncoder[Key]
  given Decoder[Key] = deriveDecoder[Key]

  final case class Body(keys: Seq[Key], pagination: Option[Pagination])
  final case class Key(recipient_did: String)
  final case class Pagination(count: Int, offset: Int, remaining: Int)
}
