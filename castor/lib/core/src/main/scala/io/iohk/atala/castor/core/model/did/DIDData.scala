package io.iohk.atala.castor.core.model.did

import scala.collection.immutable.ArraySeq

final case class DIDData(
    id: CanonicalPrismDID,
    publicKeys: Seq[PublicKey],
    services: Seq[Service],
    internalKeys: Seq[InternalPublicKey],
    context: Seq[String]
)

final case class DIDMetadata(
    lastOperationHash: ArraySeq[Byte],
    deactivated: Boolean
)
