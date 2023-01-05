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

sealed trait UpdateManagedDIDAction

object UpdateManagedDIDAction {
  final case class AddKey(template: DIDPublicKeyTemplate) extends UpdateManagedDIDAction
  final case class RemoveKey(id: String) extends UpdateManagedDIDAction
  final case class AddService(service: Service) extends UpdateManagedDIDAction
  final case class RemoveService(id: String) extends UpdateManagedDIDAction
  final case class UpdateService(service: Service) extends UpdateManagedDIDAction
}
