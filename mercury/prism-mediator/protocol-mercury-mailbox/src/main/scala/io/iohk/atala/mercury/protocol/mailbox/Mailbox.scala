package io.iohk.atala.mercury.protocol.mailbox

import io.iohk.atala.mercury.model.{DidId, Message, PIURI}
import io.iohk.atala.mercury.protocol.routing.ForwardBody

object Mailbox {
  def `type`: PIURI = "https://atalaprism.io/mercury/mailbox/1.0/ReadMessages"

  final case class ReadMessage(
      id: String = java.util.UUID.randomUUID.toString(),
      from: DidId,
      to: DidId,
      expires_time: Option[Long],
  ) {

    def asMessage = {
      Message(
        from = from,
        to = to,
        body = Map.empty[String, String],
        id = id,
        piuri = `type`
      )
    }
  }
}
