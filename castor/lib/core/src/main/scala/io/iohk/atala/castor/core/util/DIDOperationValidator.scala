package io.iohk.atala.castor.core.util

import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.castor.core.model.did.PublishedDIDOperation
import io.iohk.atala.castor.core.model.error.DIDOperationError
import io.iohk.atala.castor.core.util.DIDOperationValidator.Config
import zio.*

object DIDOperationValidator {
  final case class Config(publicKeyLimit: Int, serviceLimit: Int)

  def layer(config: Config): ULayer[DIDOperationValidator] = ZLayer.succeed(DIDOperationValidator(config))
}

class DIDOperationValidator(config: Config) {

  def validate(operation: PublishedDIDOperation): Either[DIDOperationError, Unit] = {
    for {
      _ <- validateOperationCommitment(operation)
      _ <- validateMaxPublicKeysAccess(operation)
      _ <- validateMaxServiceAccess(operation)
      _ <- validateUniquePublicKeyId(operation)
      _ <- validateUniqueServiceId(operation)
    } yield ()
  }

  private def validateOperationCommitment(operation: PublishedDIDOperation): Either[DIDOperationError, Unit] = {
    val isSha256Hex = (s: HexString) => s.toByteArray.length == 32
    operation match {
      case op: PublishedDIDOperation.Create =>
        if (isSha256Hex(op.updateCommitment) && isSha256Hex(op.recoveryCommitment)) Right(())
        else
          Left(
            DIDOperationError.InvalidArgument("operation updateCommitment or recoveryCommitment has unexpected length")
          )
    }
  }

  private def validateMaxPublicKeysAccess(operation: PublishedDIDOperation): Either[DIDOperationError, Unit] = {
    operation match {
      case op: PublishedDIDOperation.Create =>
        val keyCount = op.document.publicKeys.length
        if (keyCount <= config.publicKeyLimit) Right(())
        else Left(DIDOperationError.TooManyDidPublicKeyAccess(config.publicKeyLimit, Some(keyCount)))
    }
  }

  private def validateMaxServiceAccess(operation: PublishedDIDOperation): Either[DIDOperationError, Unit] = {
    operation match {
      case op: PublishedDIDOperation.Create =>
        val serviceCount = op.document.services.length
        if (serviceCount <= config.serviceLimit) Right(())
        else Left(DIDOperationError.TooManyDidServiceAccess(config.serviceLimit, Some(serviceCount)))
    }
  }

  private def validateUniquePublicKeyId(operation: PublishedDIDOperation): Either[DIDOperationError, Unit] = {
    operation match {
      case op: PublishedDIDOperation.Create =>
        val ids = op.document.publicKeys.map(_.id)
        if (ids.distinct.length == ids.length) Right(())
        else Left(DIDOperationError.InvalidArgument("id for public-keys is not unique"))
    }
  }

  private def validateUniqueServiceId(operation: PublishedDIDOperation): Either[DIDOperationError, Unit] = {
    operation match {
      case op: PublishedDIDOperation.Create =>
        val ids = op.document.services.map(_.id)
        if (ids.distinct.length == ids.length) Right(())
        else Left(DIDOperationError.InvalidArgument("id for services is not unique"))
    }
  }

}
