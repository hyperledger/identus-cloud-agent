package io.iohk.atala.mercury.protocol.routing

import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.model.EncryptedMessage

/** Example
  * {{{
  *     "type": "https://didcomm.org/routing/2.0/forward",
  *     "id": "abc123xyz456",
  *     "to": ["did:example:mediator"],
  *     "expires_time": 1516385931,
  *     "body":{
  *         "next": "did:foo:1234abcd"
  *     },
  *     "attachments": [
  *         // The payload(s) to be forwarded
  *     ]
  * }}}
  *
  * @param next
  *   (REQUIRED) The identifier of the party to send the attached message to.
  * @param attachments
  *   (REQUIRED) The DIDComm message(s) to send to the party indicated in the next body attribute. This content should
  *   be encrypted for the next recipient.
  */
final case class ForwardMessage(
    id: String,
    to: Set[DidId], // The mediator
    expires_time: Option[Long],
    body: ForwardBody,
    attachments: Seq[ForwardAttachment],
) {
  def `type` = "https://didcomm.org/routing/2.0/forward"
}

/** @param next
  *   typically a DID can also be (TODO) a key for the last hop of a route!
  */
case class ForwardBody(
    next: DidId
)
type ForwardAttachment = EncryptedMessage

/** Rewrapping is not implemented
  *
  * @see
  *   https://identity.foundation/didcomm-messaging/spec/#routing-protocol-20
  */
object Rewrapping {}
