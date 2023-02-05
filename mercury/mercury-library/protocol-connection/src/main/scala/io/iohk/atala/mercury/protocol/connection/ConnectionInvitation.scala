package io.iohk.atala.mercury.protocol.connection

import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation.Body

object ConnectionInvitation {

  /** Make a invitation to establish a trust connection between two peers */
  def makeConnectionInvitation(from: DidId): Invitation = {
    Invitation(
      from = from,
      body = Invitation.Body(
        goal_code = "io.atalaprism.connect",
        goal = s"Establish a trust connection between two peers using the protocol '${ConnectionRequest.`type`}'",
        Nil
      )
    )
  }

}
