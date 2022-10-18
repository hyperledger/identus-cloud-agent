package io.iohk.atala.mercury.protocol.outofbandlogin

import io.iohk.atala.mercury.model.PIURI
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.model.Message

/** Out-Of-Band Login Invitation
  * @see
  *   https://atalaprism.io/mercury/outofband-login/1.0/invitation
  */
final case class OutOfBandLoginInvitation(
    `type`: PIURI = "https://atalaprism.io/mercury/outofband-login/1.0/invitation",
    id: String = Utils.getNewMsgId,
    from: DidId
) {
  assert(`type` == "https://atalaprism.io/mercury/outofband-login/1.0/invitation")

  def makeMsg: Message = Message(piuri = `type`, id = id, from = Some(from), to = None)

  def reply(replier: DidId) = OutOfBandloginReply(
    from = replier,
    to = from,
    thid = id, // Thread identifier. Uniquely identifies the thread that the message belongs to
  )

}

/** Reply to Login Invitation
  * @see
  *   https://atalaprism.io/mercury/outofband-login/1.0/reply
  */
final case class OutOfBandloginReply(
    `type`: PIURI = "https://atalaprism.io/mercury/outofband-login/1.0/reply",
    id: String = Utils.getNewMsgId,
    from: DidId,
    to: DidId,
    thid: String,
    // replyTo: OutOfBandLoginInvitation
) {
  assert(`type` == "https://atalaprism.io/mercury/outofband-login/1.0/reply")

  def makeMsg: Message = Message(piuri = `type`, id = id, from = Some(from), to = Some(to), thid = Some(thid))
}
