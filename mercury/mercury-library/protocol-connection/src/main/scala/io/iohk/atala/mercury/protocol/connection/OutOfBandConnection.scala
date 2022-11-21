package io.iohk.atala.mercury.protocol.connection

import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation.Body

object OutOfBandConnection {

  def createInvitation(from: DidId): Invitation = {
    val body = Body("connect", "Start relationship", Seq("didcomm/v2"))
    Invitation(`type` = Invitation.`type`, from, body)
  }

}
