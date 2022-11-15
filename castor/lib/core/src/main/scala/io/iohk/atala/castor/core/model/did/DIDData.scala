package io.iohk.atala.castor.core.model.did

final case class DIDData(
    publicKeys: Seq[PublicKey],
    services: Seq[Service],
    internalKeys: Seq[InternalPublicKey]
)
