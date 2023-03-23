package io.iohk.atala.castor.core.util

import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.castor.core.model.did.{
  InternalKeyPurpose,
  InternalPublicKey,
  PrismDIDOperation,
  SignedPrismDIDOperation,
  UpdateDIDAction
}
import io.iohk.atala.castor.core.model.error.OperationValidationError
import io.iohk.atala.castor.core.util.DIDOperationValidator.Config
import io.iohk.atala.castor.core.util.UriUtils
import io.iohk.atala.castor.core.util.Prelude.*
import zio.*

import io.lemonlabs.uri.Uri
import scala.collection.immutable.ArraySeq
import io.iohk.atala.castor.core.model.did.PublicKey

object DIDOperationValidator {
  final case class Config(publicKeyLimit: Int, serviceLimit: Int)

  object Config {
    val default: Config = Config(publicKeyLimit = 50, serviceLimit = 50)
  }

  def layer(config: Config = Config.default): ULayer[DIDOperationValidator] =
    ZLayer.succeed(DIDOperationValidator(config))
}

class DIDOperationValidator(config: Config) extends BaseOperationValidator {
  def validate(operation: PrismDIDOperation): Either[OperationValidationError, Unit] = {
    operation match {
      case op: PrismDIDOperation.Create     => CreateOperationValidator.validate(config)(op)
      case op: PrismDIDOperation.Update     => UpdateOperationValidator.validate(config)(op)
      case op: PrismDIDOperation.Deactivate => DeactivateOperationValidator.validate(config)(op)
    }
  }
}

private object CreateOperationValidator extends BaseOperationValidator {
  def validate(config: Config)(operation: PrismDIDOperation.Create): Either[OperationValidationError, Unit] = {
    for {
      _ <- validateMaxPublicKeysAccess(config)(operation, extractKeyIds)
      _ <- validateMaxServiceAccess(config)(operation, extractServiceIds)
      _ <- validateUniquePublicKeyId(operation, extractKeyIds)
      _ <- validateUniqueServiceId(operation, extractServiceIds)
      _ <- validateKeyIdIsUriFragment(operation, extractKeyIds)
      _ <- validateServiceIdIsUriFragment(operation, extractServiceIds)
      _ <- validateServiceEndpointNormalized(operation, extractServiceEndpoint)
      _ <- validateMasterKeyExists(operation)
      _ <- validateServiceNonEmptyEndpoints(operation)
    } yield ()
  }

  private def validateMasterKeyExists(operation: PrismDIDOperation.Create): Either[OperationValidationError, Unit] = {
    val masterKeys =
      operation.publicKeys.collect { case pk: InternalPublicKey => pk }.filter(_.purpose == InternalKeyPurpose.Master)
    if (masterKeys.nonEmpty) Right(())
    else Left(OperationValidationError.InvalidArgument("create operation must contain at least 1 master key"))
  }

  private def validateServiceNonEmptyEndpoints(
      operation: PrismDIDOperation.Create
  ): Either[OperationValidationError, Unit] = {
    val serviceWithEmptyEndpoints = operation.services.filter(_.serviceEndpoint.isEmpty).map(_.id)
    if (serviceWithEmptyEndpoints.isEmpty) Right(())
    else
      Left(
        OperationValidationError.InvalidArgument(
          s"service must not have empty serviceEndpoint: ${serviceWithEmptyEndpoints.mkString("[", ", ", "]")}"
        )
      )
  }

  private def extractKeyIds(operation: PrismDIDOperation.Create): Seq[String] =
    operation.publicKeys.map {
      case PublicKey(id, _, _)         => id
      case InternalPublicKey(id, _, _) => id
    }

  private def extractServiceIds(operation: PrismDIDOperation.Create): Seq[String] = operation.services.map(_.id)

  private def extractServiceEndpoint(operation: PrismDIDOperation.Create): Seq[(String, Seq[Uri])] =
    operation.services.map(s => s.id -> s.serviceEndpoint)
}

private object UpdateOperationValidator extends BaseOperationValidator {
  def validate(config: Config)(operation: PrismDIDOperation.Update): Either[OperationValidationError, Unit] = {
    for {
      _ <- validateMaxPublicKeysAccess(config)(operation, extractKeyIds)
      _ <- validateMaxServiceAccess(config)(operation, extractServiceIds)
      _ <- validateKeyIdIsUriFragment(operation, extractKeyIds)
      _ <- validateServiceIdIsUriFragment(operation, extractServiceIds)
      _ <- validateServiceEndpointNormalized(operation, extractServiceEndpoint)
      _ <- validatePreviousOperationHash(operation, _.previousOperationHash)
      _ <- validateNonEmptyUpdateAction(operation)
      _ <- validateUpdateServiceNonEmpty(operation)
      _ <- validateAddServiceNonEmptyEndpoint(operation)
    } yield ()
  }

  private def validateNonEmptyUpdateAction(
      operation: PrismDIDOperation.Update
  ): Either[OperationValidationError, Unit] = {
    val isActionNonEmpty = operation.actions.nonEmpty
    if (isActionNonEmpty) Right(())
    else Left(OperationValidationError.InvalidArgument("update operation must contain at least 1 update action"))
  }

  private def validateUpdateServiceNonEmpty(
      operation: PrismDIDOperation.Update
  ): Either[OperationValidationError, Unit] = {
    val isNonEmptyUpdateService = operation.actions.forall {
      case UpdateDIDAction.UpdateService(_, None, Nil) => false
      case _                                           => true
    }
    if (isNonEmptyUpdateService) Right(())
    else
      Left(
        OperationValidationError.InvalidArgument(
          "update operation with UpdateServiceAction must not have both 'type' and 'serviceEndpoints' empty"
        )
      )
  }

  private def validateAddServiceNonEmptyEndpoint(
      operation: PrismDIDOperation.Update
  ): Either[OperationValidationError, Unit] = {
    val serviceWithEmptyEndpoints = operation.actions
      .collect { case UpdateDIDAction.AddService(s) => s }
      .filter(_.serviceEndpoint.isEmpty)
      .map(_.id)
    if (serviceWithEmptyEndpoints.isEmpty) Right(())
    else
      Left(
        OperationValidationError.InvalidArgument(
          s"service must not have empty serviceEndpoint: ${serviceWithEmptyEndpoints.mkString("[", ", ", "]")}"
        )
      )
  }

  private def extractKeyIds(operation: PrismDIDOperation.Update): Seq[String] = operation.actions.flatMap {
    case UpdateDIDAction.AddKey(publicKey)         => Some(publicKey.id)
    case UpdateDIDAction.AddInternalKey(publicKey) => Some(publicKey.id)
    case UpdateDIDAction.RemoveKey(id)             => Some(id)
    case _: UpdateDIDAction.AddService             => None
    case _: UpdateDIDAction.RemoveService          => None
    case _: UpdateDIDAction.UpdateService          => None
  }

  private def extractServiceIds(operation: PrismDIDOperation.Update): Seq[String] = operation.actions.flatMap {
    case _: UpdateDIDAction.AddKey               => None
    case _: UpdateDIDAction.AddInternalKey       => None
    case _: UpdateDIDAction.RemoveKey            => None
    case UpdateDIDAction.AddService(service)     => Some(service.id)
    case UpdateDIDAction.RemoveService(id)       => Some(id)
    case UpdateDIDAction.UpdateService(id, _, _) => Some(id)
  }

  private def extractServiceEndpoint(operation: PrismDIDOperation.Update): Seq[(String, Seq[Uri])] =
    operation.actions.collect {
      case UpdateDIDAction.AddService(service)             => service.id -> service.serviceEndpoint
      case UpdateDIDAction.UpdateService(id, _, endpoints) => id -> endpoints
    }
}

private object DeactivateOperationValidator extends BaseOperationValidator {
  def validate(config: Config)(operation: PrismDIDOperation.Deactivate): Either[OperationValidationError, Unit] =
    validatePreviousOperationHash(operation, _.previousOperationHash)
}

private trait BaseOperationValidator {

  type KeyIdExtractor[T] = T => Seq[String]
  type ServiceIdExtractor[T] = T => Seq[String]
  type ServiceEndpointExtractor[T] = T => Seq[(String, Seq[Uri])]

  protected def validateMaxPublicKeysAccess[T <: PrismDIDOperation](
      config: Config
  )(operation: T, keyIdExtractor: KeyIdExtractor[T]): Either[OperationValidationError, Unit] = {
    val keyCount = keyIdExtractor(operation).length
    if (keyCount <= config.publicKeyLimit) Right(())
    else Left(OperationValidationError.TooManyDidPublicKeyAccess(config.publicKeyLimit, Some(keyCount)))
  }

  protected def validateMaxServiceAccess[T <: PrismDIDOperation](
      config: Config
  )(operation: T, serviceIdExtractor: ServiceIdExtractor[T]): Either[OperationValidationError, Unit] = {
    val serviceCount = serviceIdExtractor(operation).length
    if (serviceCount <= config.serviceLimit) Right(())
    else Left(OperationValidationError.TooManyDidServiceAccess(config.serviceLimit, Some(serviceCount)))
  }

  protected def validateUniquePublicKeyId[T <: PrismDIDOperation](
      operation: T,
      keyIdExtractor: KeyIdExtractor[T]
  ): Either[OperationValidationError, Unit] = {
    val ids = keyIdExtractor(operation)
    if (ids.isUnique) Right(())
    else Left(OperationValidationError.InvalidArgument("id for public-keys is not unique"))
  }

  protected def validateUniqueServiceId[T <: PrismDIDOperation](
      operation: T,
      serviceIdExtractor: ServiceIdExtractor[T]
  ): Either[OperationValidationError, Unit] = {
    val ids = serviceIdExtractor(operation)
    if (ids.isUnique) Right(())
    else Left(OperationValidationError.InvalidArgument("id for services is not unique"))
  }

  protected def validateKeyIdIsUriFragment[T <: PrismDIDOperation](
      operation: T,
      keyIdExtractor: KeyIdExtractor[T]
  ): Either[OperationValidationError, Unit] = {
    val ids = keyIdExtractor(operation)
    val invalidIds = ids.filterNot(UriUtils.isValidUriFragment)
    if (invalidIds.isEmpty) Right(())
    else
      Left(
        OperationValidationError.InvalidArgument(s"public key id is invalid: ${invalidIds.mkString("[", ", ", "]")}")
      )
  }

  protected def validateServiceIdIsUriFragment[T <: PrismDIDOperation](
      operation: T,
      serviceIdExtractor: ServiceIdExtractor[T]
  ): Either[OperationValidationError, Unit] = {
    val ids = serviceIdExtractor(operation)
    val invalidIds = ids.filterNot(UriUtils.isValidUriFragment)
    if (invalidIds.isEmpty) Right(())
    else
      Left(OperationValidationError.InvalidArgument(s"service id is invalid: ${invalidIds.mkString("[", ", ", "]")}"))
  }

  protected def validateServiceEndpointNormalized[T <: PrismDIDOperation](
      operation: T,
      endpointExtractor: ServiceEndpointExtractor[T]
  ): Either[OperationValidationError, Unit] = {
    val uris = endpointExtractor(operation).flatMap(_._2)
    val nonNormalizedUris = uris.filterNot(isUriNormalized)
    if (nonNormalizedUris.isEmpty) Right(())
    else
      Left(
        OperationValidationError.InvalidArgument(
          s"serviceEndpoint URIs must be normalized: ${nonNormalizedUris.mkString("[", ", ", "]")}"
        )
      )
  }

  protected def validatePreviousOperationHash[T <: PrismDIDOperation](
      operation: T,
      previousOperationHashExtractor: T => ArraySeq[Byte]
  ): Either[OperationValidationError, Unit] = {
    val previousOperationHash = previousOperationHashExtractor(operation)
    if (previousOperationHash.length == 32) Right(())
    else Left(OperationValidationError.InvalidArgument(s"previousOperationHash must have a size of 32 bytes"))
  }

  /** @return true if a given uri is normalized */
  protected def isUriNormalized(uri: Uri): Boolean = {
    val uriString = uri.toString
    UriUtils.normalizeUri(uriString).contains(uriString)
  }

}
