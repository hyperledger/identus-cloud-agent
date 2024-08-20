package org.hyperledger.identus.mercury.protocol.presentproof

import org.hyperledger.identus.mercury.model.{AttachmentDescriptor, DidId}
import org.hyperledger.identus.mercury.protocol.invitation.v2.Invitation
object PresentProofInvitation {
  def makeInvitation(
      from: DidId,
      goalCode: Option[String],
      goal: Option[String],
      invitationId: String,
      requestPresentation: RequestPresentation
  ): Invitation = {
    val attachmentDescriptor = AttachmentDescriptor.buildJsonAttachment(payload = requestPresentation)
    Invitation(
      id = invitationId,
      from = from,
      body = Invitation.Body(
        goal_code = goalCode,
        goal = goal,
        Nil
      ),
      attachments = Some(Seq(attachmentDescriptor))
    )
  }

}
