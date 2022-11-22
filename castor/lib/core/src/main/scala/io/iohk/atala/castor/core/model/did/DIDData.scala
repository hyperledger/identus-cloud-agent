package io.iohk.atala.castor.core.model.did

final case class DIDData(
    id: CanonicalPrismDID,
    publicKeys: Seq[PublicKey],
    services: Seq[Service],
    internalKeys: Seq[InternalPublicKey]
)
