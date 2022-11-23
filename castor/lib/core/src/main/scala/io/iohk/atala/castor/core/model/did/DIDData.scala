package io.iohk.atala.castor.core.model.did

import io.iohk.atala.castor.core.model.did.w3c.DIDDocumentRepr

final case class DIDData(
    id: CanonicalPrismDID,
    publicKeys: Seq[PublicKey],
    services: Seq[Service],
    internalKeys: Seq[InternalPublicKey]
)
