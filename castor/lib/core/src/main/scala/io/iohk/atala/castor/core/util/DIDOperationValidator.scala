package io.iohk.atala.castor.core.util

import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.castor.core.model.did.{
  PrismDIDOperation,
  SignedPrismDIDOperation,
  UpdateDIDAction,
  InternalKeyPurpose
}
import io.iohk.atala.castor.core.model.error.DIDOperationError
import io.iohk.atala.castor.core.util.DIDOperationValidator.Config
import zio.*

import java.net.URI

object DIDOperationValidator {
  final case class Config(publicKeyLimit: Int, serviceLimit: Int)

  object Config {
    val default: Config = Config(publicKeyLimit = 50, serviceLimit = 50)
  }

  def layer(config: Config = Config.default): ULayer[DIDOperationValidator] =
    ZLayer.succeed(DIDOperationValidator(config))
}

class DIDOperationValidator(config: Config) {

  // For now we only use same regex as Node.
  // Prism DID spec might support full URI fragment syntax in the future.
  private val KEY_ID_RE = "^\\w+$".r
  private val SERVICE_ID_RE = "^\\w+$".r

  extension [T](xs: Seq[T]) {
    def isUnique: Boolean = xs.length == xs.distinct.length
  }

  def validate(operation: PrismDIDOperation): Either[DIDOperationError, Unit] = {
    for {
      _ <- validateMaxPublicKeysAccess(operation)
      _ <- validateMaxServiceAccess(operation)
      _ <- validateUniquePublicKeyId(operation)
      _ <- validateUniqueServiceId(operation)
      _ <- validateKeyIdRegex(operation)
      _ <- validateServiceIdRegex(operation)
      _ <- validateMasterKeyExists(operation)
      _ <- validateNonEmptyUpdateOperation(operation)
    } yield ()
  }

  private def validateMaxPublicKeysAccess(operation: PrismDIDOperation): Either[DIDOperationError, Unit] = {
    val keyCount = extractKeyIds(operation).length
    if (keyCount <= config.publicKeyLimit) Right(())
    else Left(DIDOperationError.TooManyDidPublicKeyAccess(config.publicKeyLimit, Some(keyCount)))
  }

  private def validateMaxServiceAccess(operation: PrismDIDOperation): Either[DIDOperationError, Unit] = {
    val serviceCount = extractServiceIds(operation).length
    if (serviceCount <= config.serviceLimit) Right(())
    else Left(DIDOperationError.TooManyDidServiceAccess(config.serviceLimit, Some(serviceCount)))
  }

  private def validateUniquePublicKeyId(operation: PrismDIDOperation): Either[DIDOperationError, Unit] = {
    operation match {
      case op: PrismDIDOperation.Create =>
        val ids = extractKeyIds(op)
        if (ids.isUnique) Right(())
        else Left(DIDOperationError.InvalidArgument("id for public-keys is not unique"))
      case _: PrismDIDOperation.Update => Right(())
    }
  }

  private def validateUniqueServiceId(operation: PrismDIDOperation): Either[DIDOperationError, Unit] = {
    operation match {
      case op: PrismDIDOperation.Create =>
        val ids = extractServiceIds(operation)
        if (ids.isUnique) Right(())
        else Left(DIDOperationError.InvalidArgument("id for services is not unique"))
      case _: PrismDIDOperation.Update => Right(())
    }
  }

  private def validateKeyIdRegex(operation: PrismDIDOperation): Either[DIDOperationError, Unit] = {
    val keyIds = extractKeyIds(operation)
    val invalidIds = keyIds.filterNot(id => KEY_ID_RE.pattern.matcher(id).matches())
    if (invalidIds.isEmpty) Right(())
    else
      Left(DIDOperationError.InvalidArgument(s"public key id is invalid: ${invalidIds.mkString("[", ", ", "]")}"))
  }

  private def validateServiceIdRegex(operation: PrismDIDOperation): Either[DIDOperationError, Unit] = {
    val serviceIds = extractServiceIds(operation)
    val invalidIds = serviceIds.filterNot(id => SERVICE_ID_RE.pattern.matcher(id).matches())
    if (invalidIds.isEmpty) Right(())
    else
      Left(DIDOperationError.InvalidArgument(s"service id is invalid: ${invalidIds.mkString("[", ", ", "]")}"))
  }

  private def validateMasterKeyExists(operation: PrismDIDOperation): Either[DIDOperationError, Unit] = {
    operation match {
      case op: PrismDIDOperation.Create =>
        val masterKeys = op.internalKeys.filter(_.purpose == InternalKeyPurpose.Master)
        if (masterKeys.nonEmpty) Right(())
        else Left(DIDOperationError.InvalidArgument("create operation must contain at least 1 master key"))
      case _ => Right(())
    }
  }

  private def validateNonEmptyUpdateOperation(operation: PrismDIDOperation): Either[DIDOperationError, Unit] = {
    operation match {
      case op: PrismDIDOperation.Update =>
        if (op.actions.nonEmpty) Right(())
        else Left(DIDOperationError.InvalidArgument("update operation must contain at least 1 update action"))
      case _ => Right(())
    }
  }

  private def extractKeyIds(operation: PrismDIDOperation): Seq[String] = {
    operation match {
      case op: PrismDIDOperation.Create => op.publicKeys.map(_.id) ++ op.internalKeys.map(_.id)
      case PrismDIDOperation.Update(_, _, actions) =>
        actions.flatMap {
          case UpdateDIDAction.AddKey(publicKey)         => Some(publicKey.id)
          case UpdateDIDAction.AddInternalKey(publicKey) => Some(publicKey.id)
          case UpdateDIDAction.RemoveKey(id)             => Some(id)
          case _: UpdateDIDAction.AddService             => None
          case _: UpdateDIDAction.RemoveService          => None
          case _: UpdateDIDAction.UpdateService          => None
        }
    }
  }

  private def extractServiceIds(operation: PrismDIDOperation): Seq[String] = {
    operation match {
      case op: PrismDIDOperation.Create => op.services.map(_.id)
      case PrismDIDOperation.Update(_, _, actions) =>
        actions.flatMap {
          case _: UpdateDIDAction.AddKey               => None
          case _: UpdateDIDAction.AddInternalKey       => None
          case _: UpdateDIDAction.RemoveKey            => None
          case UpdateDIDAction.AddService(service)     => Some(service.id)
          case UpdateDIDAction.RemoveService(id)       => Some(id)
          case UpdateDIDAction.UpdateService(id, _, _) => Some(id)
        }
    }
  }

}
