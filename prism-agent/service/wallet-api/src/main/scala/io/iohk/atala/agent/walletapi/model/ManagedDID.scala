package io.iohk.atala.agent.walletapi.model

import io.iohk.atala.castor.core.model.did.{PrismDID, Service, VerificationRelationship}

final case class ManagedDIDCreateTemplate(
    storage: String,
    publicKeys: Seq[DIDPublicKeyTemplate],
    services: Seq[Service]
)

final case class DIDPublicKeyTemplate(
    id: String,
    purpose: VerificationRelationship
)

final case class ManagedDIDUpdateTemplate(
    did: PrismDID,
    patches: Seq[ManagedDIDUpdatePatch]
)

sealed trait ManagedDIDUpdatePatch

object ManagedDIDUpdatePatch {
  final case class AddPublicKey(template: DIDPublicKeyTemplate) extends ManagedDIDUpdatePatch
  final case class RemovePublicKey(id: String) extends ManagedDIDUpdatePatch
  final case class AddService(service: Service) extends ManagedDIDUpdatePatch
  final case class RemoveService(id: String) extends ManagedDIDUpdatePatch
  final case class RotateKey(id: String) extends ManagedDIDUpdatePatch
}
