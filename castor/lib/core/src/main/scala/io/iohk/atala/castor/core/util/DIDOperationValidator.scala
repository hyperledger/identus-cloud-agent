package io.iohk.atala.castor.core.util

import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.castor.core.model.did.{PrismDIDOperation, SignedPrismDIDOperation}
import io.iohk.atala.castor.core.model.error.DIDOperationError
import io.iohk.atala.castor.core.util.DIDOperationValidator.Config
import zio.*

object DIDOperationValidator {
  final case class Config(publicKeyLimit: Int)

  object Config {
    val default: Config = Config(publicKeyLimit = 50)
  }

  def layer(config: Config = Config.default): ULayer[DIDOperationValidator] =
    ZLayer.succeed(DIDOperationValidator(config))
}

class DIDOperationValidator(config: Config) {

  private val KEY_ID_RE = "^\\w+$".r

  def validate(operation: PrismDIDOperation): Either[DIDOperationError, Unit] = {
    for {
      _ <- validateMaxPublicKeysAccess(operation)
      _ <- validateMaxServiceAccess(operation)
      _ <- validateUniquePublicKeyId(operation)
      _ <- validateUniqueServiceId(operation)
      _ <- validateKeyIdRegex(operation)
    } yield ()
  }

  private def validateMaxPublicKeysAccess(operation: PrismDIDOperation): Either[DIDOperationError, Unit] = {
    operation match {
      case op: PrismDIDOperation.Create =>
        val keyCount = op.publicKeys.length + op.internalKeys.length
        if (keyCount <= config.publicKeyLimit) Right(())
        else Left(DIDOperationError.TooManyDidPublicKeyAccess(config.publicKeyLimit, Some(keyCount)))
    }
  }

  // TODO: bring back when service endpoint is added in Node
  private def validateMaxServiceAccess(operation: PrismDIDOperation): Either[DIDOperationError, Unit] = {
//    operation match {
//      case op: PrismDIDOperation.Create =>
//        val serviceCount = op.document.services.length
//        if (serviceCount <= config.serviceLimit) Right(())
//        else Left(DIDOperationError.TooManyDidServiceAccess(config.serviceLimit, Some(serviceCount)))
//    }
    Right(())
  }

  private def validateUniquePublicKeyId(operation: PrismDIDOperation): Either[DIDOperationError, Unit] = {
    operation match {
      case op: PrismDIDOperation.Create =>
        val ids = op.publicKeys.map(_.id) ++ op.internalKeys.map(_.id)
        if (ids.distinct.length == ids.length) Right(())
        else Left(DIDOperationError.InvalidArgument("id for public-keys is not unique"))
    }
  }

  // TODO: bring back when service endpoint is added in Node
  private def validateUniqueServiceId(operation: PrismDIDOperation): Either[DIDOperationError, Unit] = {
//    operation match {
//      case op: PrismDIDOperation.Create =>
//        val ids = op.document.services.map(_.id)
//        if (ids.distinct.length == ids.length) Right(())
//        else Left(DIDOperationError.InvalidArgument("id for services is not unique"))
//    }
    Right(())
  }

  private def validateKeyIdRegex(operation: PrismDIDOperation): Either[DIDOperationError, Unit] = {
    operation match {
      case PrismDIDOperation.Create(publicKeys, internalKeys) =>
        val ids = publicKeys.map(_.id) ++ internalKeys.map(_.id)
        val invalidIds = ids.filterNot(id => KEY_ID_RE.pattern.matcher(id).matches())
        if (invalidIds.isEmpty) Right(())
        else
          Left(DIDOperationError.InvalidArgument(s"public key id is invalid: ${invalidIds.mkString("[", ", ", "]")}"))
    }
  }

}
