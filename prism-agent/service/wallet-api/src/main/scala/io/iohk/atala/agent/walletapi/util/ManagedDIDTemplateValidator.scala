package io.iohk.atala.agent.walletapi.util

import io.iohk.atala.agent.walletapi.model.ManagedDIDTemplate
import io.iohk.atala.agent.walletapi.service.ManagedDIDService

object ManagedDIDTemplateValidator {

  def validate(template: ManagedDIDTemplate): Either[String, Unit] = {
    for {
      _ <- validateReservedKeyId(template)
      _ <- validateUniqueKeyId(template)
    } yield ()
  }

  private def validateReservedKeyId(template: ManagedDIDTemplate): Either[String, Unit] = {
    val keyIds = template.publicKeys.map(_.id)
    val reservedKeyIds = keyIds.filter(id => ManagedDIDService.reservedKeyIds.contains(id))
    if (reservedKeyIds.nonEmpty)
      Left(s"DID template cannot contain reserved key name: ${reservedKeyIds.mkString("[", ", ", "]")}")
    else Right(())
  }

  private def validateUniqueKeyId(template: ManagedDIDTemplate): Either[String, Unit] = {
    val keyIds = template.publicKeys.map(_.id)
    if (keyIds.distinct.length == keyIds.length) Right(())
    else Left("Public key for creating a DID id must be unique")
  }

}
