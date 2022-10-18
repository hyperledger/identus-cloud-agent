package io.iohk.atala.agent.custodian.model

import io.iohk.atala.castor.core.model.did.{Service, VerificationRelationship}

final case class ManagedDIDTemplate(
    storage: String,
    publicKeys: Seq[DIDPublicKeyTemplate],
    services: Seq[Service]
)

final case class DIDPublicKeyTemplate(
    id: String,
    purpose: VerificationRelationship
)
