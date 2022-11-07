package io.iohk.atala.castor.core.util

import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.castor.core.model.did.{DIDStatePatch, PublishedDIDOperation}
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
      _ <- validateVersionRef(operation)
    } yield ()
  }

  private def validateOperationCommitment(operation: PublishedDIDOperation): Either[DIDOperationError, Unit] = {
    operation match {
      case op: PublishedDIDOperation.Create =>
        if (isSha256Hex(op.updateCommitment) && isSha256Hex(op.recoveryCommitment)) Right(())
        else
          Left(
            DIDOperationError.InvalidArgument("operation updateCommitment or recoveryCommitment has unexpected length")
          )
      case op: PublishedDIDOperation.Update =>
        if (isSha256Hex(op.delta.updateCommitment)) Right(())
        else
          Left(
            DIDOperationError.InvalidArgument("operation updateCommitment has unexpected length")
          )
    }
  }

  private def validateMaxPublicKeysAccess(operation: PublishedDIDOperation): Either[DIDOperationError, Unit] = {
    val keyAccessCount = operation match {
      case op: PublishedDIDOperation.Create => op.document.publicKeys.length
      case op: PublishedDIDOperation.Update =>
        op.delta.patches.collect {
          case _: DIDStatePatch.AddPublicKey    => 1
          case _: DIDStatePatch.RemovePublicKey => 1
        }.sum
    }
    if (keyAccessCount <= config.publicKeyLimit) Right(())
    else Left(DIDOperationError.TooManyDidPublicKeyAccess(config.publicKeyLimit, Some(keyAccessCount)))
  }

  private def validateMaxServiceAccess(operation: PublishedDIDOperation): Either[DIDOperationError, Unit] = {
    val serviceAccessCount = operation match {
      case op: PublishedDIDOperation.Create => op.document.services.length
      case op: PublishedDIDOperation.Update =>
        op.delta.patches.collect {
          case _: DIDStatePatch.AddService    => 1
          case _: DIDStatePatch.RemoveService => 1
        }.sum
    }
    if (serviceAccessCount <= config.serviceLimit) Right(())
    else Left(DIDOperationError.TooManyDidServiceAccess(config.serviceLimit, Some(serviceAccessCount)))
  }

  private def validateUniquePublicKeyId(operation: PublishedDIDOperation): Either[DIDOperationError, Unit] = {
    operation match {
      case op: PublishedDIDOperation.Create =>
        val ids = op.document.publicKeys.map(_.id)
        if (ids.distinct.length == ids.length) Right(())
        else Left(DIDOperationError.InvalidArgument("id for public-keys is not unique"))
      case _: PublishedDIDOperation.Update => Right(())
    }
  }

  private def validateUniqueServiceId(operation: PublishedDIDOperation): Either[DIDOperationError, Unit] = {
    operation match {
      case op: PublishedDIDOperation.Create =>
        val ids = op.document.services.map(_.id)
        if (ids.distinct.length == ids.length) Right(())
        else Left(DIDOperationError.InvalidArgument("id for services is not unique"))
      case _: PublishedDIDOperation.Update => Right(())
    }
  }

  private def validateVersionRef(operation: PublishedDIDOperation): Either[DIDOperationError, Unit] = {
    operation match {
      case _: PublishedDIDOperation.Create => Right(())
      case op: PublishedDIDOperation.Update =>
        if (isSha256Hex(op.previousVersion)) Right(())
        else Left(DIDOperationError.InvalidArgument("previousVersion reference has unexpected length"))
    }
  }

  private def isSha256Hex(s: HexString): Boolean = s.toByteArray.length == 32

}
