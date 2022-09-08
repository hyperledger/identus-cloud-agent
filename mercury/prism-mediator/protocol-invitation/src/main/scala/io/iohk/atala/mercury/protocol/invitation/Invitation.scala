package io.iohk.atala.mercury.protocol.invitation

import io.iohk.atala.mercury.model.PIURI

object Invitation {
  def `type`: PIURI = "https://atalaprism.io/mercury/out-of-band/1.0/invitation"

}

case class Invitation(
    id: String,
    `@type`: String,
    label: String,
    body: Body,
    handshake_protocols: Seq[String],
    service: Seq[Service] // FIXME service: Seq[ServiceType]
)
