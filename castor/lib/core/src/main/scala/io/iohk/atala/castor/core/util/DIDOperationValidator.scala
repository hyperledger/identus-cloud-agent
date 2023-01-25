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
import io.iohk.atala.castor.core.util.Prelude.*
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

  // TODO: specification alignment
  // - service endpoint normalization
  // - service endpoints in create-operation must be non-empty
  def validate(operation: PrismDIDOperation): Either[DIDOperationError, Unit] = {
    for {
      _ <- validateMaxPublicKeysAccess(operation)
      _ <- validateMaxServiceAccess(operation)
      _ <- validateUniquePublicKeyId(operation)
      _ <- validateUniqueServiceId(operation)
      _ <- validateKeyIdIsUriFragment(operation)
      _ <- validateServiceIdIsUriFragment(operation)
      _ <- validateMasterKeyExists(operation)
      _ <- validateUpdateOperationAction(operation)
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
      case _: PrismDIDOperation.Create =>
        val ids = extractServiceIds(operation)
        if (ids.isUnique) Right(())
        else Left(DIDOperationError.InvalidArgument("id for services is not unique"))
      case _: PrismDIDOperation.Update => Right(())
    }
  }

  private def validateKeyIdIsUriFragment(operation: PrismDIDOperation): Either[DIDOperationError, Unit] = {
    val keyIds = extractKeyIds(operation)
    val invalidIds = keyIds.filterNot(isValidUriFragment)
    if (invalidIds.isEmpty) Right(())
    else
      Left(DIDOperationError.InvalidArgument(s"public key id is invalid: ${invalidIds.mkString("[", ", ", "]")}"))
  }

  private def validateServiceIdIsUriFragment(operation: PrismDIDOperation): Either[DIDOperationError, Unit] = {
    val serviceIds = extractServiceIds(operation)
    val invalidIds = serviceIds.filterNot(isValidUriFragment)
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

  private def validateUpdateOperationAction(operation: PrismDIDOperation): Either[DIDOperationError, Unit] = {
    // https://github.com/input-output-hk/atala-prism-building-blocks/blob/8eea08eb1a96f53ff546d720bd4750f86d3b8345/docs/method-spec/PRISM-method.md?plain=1#L419
    def isUpdateServiceActionNonEmpty(action: UpdateDIDAction): Boolean = action match {
      case UpdateDIDAction.UpdateService(_, None, Nil) => false
      case _                                           => true
    }

    operation match {
      case op: PrismDIDOperation.Update =>
        val isActionNonEmpty = op.actions.nonEmpty
        val isUpdateServiceActionValid = op.actions.forall(isUpdateServiceActionNonEmpty)

        if (!isActionNonEmpty)
          Left(DIDOperationError.InvalidArgument("update operation must contain at least 1 update action"))
        else if (!isUpdateServiceActionValid)
          Left(
            DIDOperationError.InvalidArgument(
              "update operation with UpdateServiceAction must not have both 'type' and 'serviceEndpoints' empty"
            )
          )
        else Right(())
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

  /** Checks if a string is a valid URI fragment according to <a
    * href="https://www.rfc-editor.org/rfc/rfc3986#section-3.5">RFC&nbsp;3986&nbsp;section-3.5</a>
    *
    * @param str
    * @return
    *   true if str is a valid URI fragment, otherwise false
    */
  private def isValidUriFragment(str: String): Boolean = {

    /*
     * Alphanumeric characters (A-Z, a-z, 0-9)
     * Some special characters: -._~!$&'()*+,;=:@
     * Percent-encoded characters, which are represented by the pattern %[0-9A-Fa-f]{2}
     */
    val uriFragmentRegex = "^([A-Za-z0-9\\-._~!$&'()*+,;=:@/?]|%[0-9A-Fa-f]{2})*$".r

    // In general, empty URI fragment is a valid fragment, but for our use-case it would be pointless
    str.nonEmpty && uriFragmentRegex.matches(str)
  }

}
