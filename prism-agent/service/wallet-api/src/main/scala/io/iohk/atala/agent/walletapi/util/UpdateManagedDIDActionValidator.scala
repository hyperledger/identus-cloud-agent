package io.iohk.atala.agent.walletapi.util

import io.iohk.atala.agent.walletapi.model.UpdateManagedDIDAction
import io.iohk.atala.agent.walletapi.service.ManagedDIDService

object UpdateManagedDIDActionValidator {

  def validate(actions: Seq[UpdateManagedDIDAction]): Either[String, Unit] = validateReservedKeyId(actions)

  private def validateReservedKeyId(actions: Seq[UpdateManagedDIDAction]): Either[String, Unit] = {
    val keyIds = actions.flatMap {
      case UpdateManagedDIDAction.AddKey(template) => Some(template.id)
      case UpdateManagedDIDAction.RemoveKey(id)    => Some(id)
      case UpdateManagedDIDAction.AddService(_)    => None
      case UpdateManagedDIDAction.RemoveService(_) => None
      case UpdateManagedDIDAction.UpdateService(_) => None
    }
    val reservedKeyIds = keyIds.filter(id => ManagedDIDService.reservedKeyIds.contains(id))
    if (reservedKeyIds.nonEmpty)
      Left(s"DID update actions cannot contain reserved key name: ${reservedKeyIds.mkString("[", ", ", "]")}")
    else Right(())
  }

}
