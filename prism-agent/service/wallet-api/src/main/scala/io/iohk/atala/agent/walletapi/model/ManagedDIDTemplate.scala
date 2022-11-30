package io.iohk.atala.agent.walletapi.model

import io.iohk.atala.castor.core.model.did.{Service, VerificationRelationship}

final case class ManagedDIDTemplate(
    publicKeys: Seq[DIDPublicKeyTemplate],
    services: Seq[Service]
)

final case class DIDPublicKeyTemplate(
    id: String,
    purpose: VerificationRelationship
)
