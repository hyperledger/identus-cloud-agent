package io.iohk.atala.castor.core.model.did

final case class DIDDocument(
    publicKeys: Seq[PublicKey],
    services: Seq[Service]
)
