package io.iohk.atala.mercury.protocol.didexchange.v1

import io.iohk.atala.mercury.model.{AttachmentDescriptor, PIURI}

final case class Thread(thid: String, pthid: String)

final case class DidExchangeRequest(
    `@id`: String,
    `@type`: PIURI,
    `~thread`: Thread,
    label: String,
    goal_code: String,
    goal: String,
    `did_doc~attach`: Option[AttachmentDescriptor]
)
