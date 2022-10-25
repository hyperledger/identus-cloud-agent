package io.iohk.atala.mercury.mediator

case class PublicKey(
    id: String,
    `type`: String,
    controller: String,
    publicKeyBase58: String
)
case class MediateRequest(
    id: String,
    `@type`: String,
    invitationId: String,
    publicKey: PublicKey
)
case class MediateResponse(
    id: String,
    `@type`: String,
    endpoint: String,
    routing_keys: Seq[String]
)
