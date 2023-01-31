package io.iohk.atala.castor.core.util

import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.castor.core.model.did.{
  InternalKeyPurpose,
  PrismDIDOperation,
  SignedPrismDIDOperation,
  UpdateDIDAction
}
import io.iohk.atala.castor.core.model.error.DIDOperationError
import io.iohk.atala.castor.core.util.DIDOperationValidator.Config
import io.iohk.atala.castor.core.util.Prelude.*
import zio.*

import java.net.URI
import scala.collection.immutable.ArraySeq

object DIDOperationValidator {
  final case class Config(publicKeyLimit: Int, serviceLimit: Int)

  object Config {
    val default: Config = Config(publicKeyLimit = 50, serviceLimit = 50)
  }

  def layer(config: Config = Config.default): ULayer[DIDOperationValidator] =
    ZLayer.succeed(DIDOperationValidator(config))
}

class DIDOperationValidator(config: Config) extends BaseOperationValidator {
  def validate(operation: PrismDIDOperation): Either[DIDOperationError, Unit] = {
    operation match {
      case op: PrismDIDOperation.Create     => CreateOperationValidator.validate(config)(op)
      case op: PrismDIDOperation.Update     => UpdateOperationValidator.validate(config)(op)
      case op: PrismDIDOperation.Deactivate => DeactivateOperationValidator.validate(config)(op)
    }
  }
}

private object CreateOperationValidator extends BaseOperationValidator {
  def validate(config: Config)(operation: PrismDIDOperation.Create): Either[DIDOperationError, Unit] = {
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

  private def validateMasterKeyExists(operation: PrismDIDOperation.Create): Either[DIDOperationError, Unit] = {
    val masterKeys = operation.internalKeys.filter(_.purpose == InternalKeyPurpose.Master)
    if (masterKeys.nonEmpty) Right(())
    else Left(DIDOperationError.InvalidArgument("create operation must contain at least 1 master key"))
  }

  private def validateServiceNonEmptyEndpoints(
      operation: PrismDIDOperation.Create
  ): Either[DIDOperationError, Unit] = {
    val serviceWithEmptyEndpoints = operation.services.filter(_.serviceEndpoint.isEmpty).map(_.id)
    if (serviceWithEmptyEndpoints.isEmpty) Right(())
    else
      Left(
        DIDOperationError.InvalidArgument(
          s"service must not have empty serviceEndpoint: ${serviceWithEmptyEndpoints.mkString("[", ", ", "]")}"
        )
      )
  }

  private def extractKeyIds(operation: PrismDIDOperation.Create): Seq[String] =
    operation.publicKeys.map(_.id) ++ operation.internalKeys.map(_.id)

  private def extractServiceIds(operation: PrismDIDOperation.Create): Seq[String] = operation.services.map(_.id)

  private def extractServiceEndpoint(operation: PrismDIDOperation.Create): Seq[(String, Seq[URI])] =
    operation.services.map(s => s.id -> s.serviceEndpoint)
}

private object UpdateOperationValidator extends BaseOperationValidator {
  def validate(config: Config)(operation: PrismDIDOperation.Update): Either[DIDOperationError, Unit] = {
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

  private def validateNonEmptyUpdateAction(operation: PrismDIDOperation.Update): Either[DIDOperationError, Unit] = {
    val isActionNonEmpty = operation.actions.nonEmpty
    if (isActionNonEmpty) Right(())
    else Left(DIDOperationError.InvalidArgument("update operation must contain at least 1 update action"))
  }

  private def validateUpdateServiceNonEmpty(operation: PrismDIDOperation.Update): Either[DIDOperationError, Unit] = {
    val isNonEmptyUpdateService = operation.actions.forall {
      case UpdateDIDAction.UpdateService(_, None, Nil) => false
      case _                                           => true
    }
    if (isNonEmptyUpdateService) Right(())
    else
      Left(
        DIDOperationError.InvalidArgument(
          "update operation with UpdateServiceAction must not have both 'type' and 'serviceEndpoints' empty"
        )
      )
  }

  private def validateAddServiceNonEmptyEndpoint(
      operation: PrismDIDOperation.Update
  ): Either[DIDOperationError, Unit] = {
    val serviceWithEmptyEndpoints = operation.actions
      .collect { case UpdateDIDAction.AddService(s) => s }
      .filter(_.serviceEndpoint.isEmpty)
      .map(_.id)
    if (serviceWithEmptyEndpoints.isEmpty) Right(())
    else
      Left(
        DIDOperationError.InvalidArgument(
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

  private def extractServiceEndpoint(operation: PrismDIDOperation.Update): Seq[(String, Seq[URI])] =
    operation.actions.collect {
      case UpdateDIDAction.AddService(service)             => service.id -> service.serviceEndpoint
      case UpdateDIDAction.UpdateService(id, _, endpoints) => id -> endpoints
    }
}

private object DeactivateOperationValidator extends BaseOperationValidator {
  def validate(config: Config)(operation: PrismDIDOperation.Deactivate): Either[DIDOperationError, Unit] =
    validatePreviousOperationHash(operation, _.previousOperationHash)
}

private trait BaseOperationValidator {

  type KeyIdExtractor[T] = T => Seq[String]
  type ServiceIdExtractor[T] = T => Seq[String]
  type ServiceEndpointExtractor[T] = T => Seq[(String, Seq[URI])]

  protected def validateMaxPublicKeysAccess[T <: PrismDIDOperation](
      config: Config
  )(operation: T, keyIdExtractor: KeyIdExtractor[T]): Either[DIDOperationError, Unit] = {
    val keyCount = keyIdExtractor(operation).length
    if (keyCount <= config.publicKeyLimit) Right(())
    else Left(DIDOperationError.TooManyDidPublicKeyAccess(config.publicKeyLimit, Some(keyCount)))
  }

  protected def validateMaxServiceAccess[T <: PrismDIDOperation](
      config: Config
  )(operation: T, serviceIdExtractor: ServiceIdExtractor[T]): Either[DIDOperationError, Unit] = {
    val serviceCount = serviceIdExtractor(operation).length
    if (serviceCount <= config.serviceLimit) Right(())
    else Left(DIDOperationError.TooManyDidServiceAccess(config.serviceLimit, Some(serviceCount)))
  }

  protected def validateUniquePublicKeyId[T <: PrismDIDOperation](
      operation: T,
      keyIdExtractor: KeyIdExtractor[T]
  ): Either[DIDOperationError, Unit] = {
    val ids = keyIdExtractor(operation)
    if (ids.isUnique) Right(())
    else Left(DIDOperationError.InvalidArgument("id for public-keys is not unique"))
  }

  protected def validateUniqueServiceId[T <: PrismDIDOperation](
      operation: T,
      serviceIdExtractor: ServiceIdExtractor[T]
  ): Either[DIDOperationError, Unit] = {
    val ids = serviceIdExtractor(operation)
    if (ids.isUnique) Right(())
    else Left(DIDOperationError.InvalidArgument("id for services is not unique"))
  }

  protected def validateKeyIdIsUriFragment[T <: PrismDIDOperation](
      operation: T,
      keyIdExtractor: KeyIdExtractor[T]
  ): Either[DIDOperationError, Unit] = {
    val ids = keyIdExtractor(operation)
    val invalidIds = ids.filterNot(isValidUriFragment)
    if (invalidIds.isEmpty) Right(())
    else
      Left(DIDOperationError.InvalidArgument(s"public key id is invalid: ${invalidIds.mkString("[", ", ", "]")}"))
  }

  protected def validateServiceIdIsUriFragment[T <: PrismDIDOperation](
      operation: T,
      serviceIdExtractor: ServiceIdExtractor[T]
  ): Either[DIDOperationError, Unit] = {
    val ids = serviceIdExtractor(operation)
    val invalidIds = ids.filterNot(isValidUriFragment)
    if (invalidIds.isEmpty) Right(())
    else
      Left(DIDOperationError.InvalidArgument(s"service id is invalid: ${invalidIds.mkString("[", ", ", "]")}"))
  }

  protected def validateServiceEndpointNormalized[T <: PrismDIDOperation](
      operation: T,
      endpointExtractor: ServiceEndpointExtractor[T]
  ): Either[DIDOperationError, Unit] = {
    val uris = endpointExtractor(operation).flatMap(_._2)
    val nonNormalizedUris = uris.filterNot(isUriNormalized)
    if (nonNormalizedUris.isEmpty) Right(())
    else
      Left(
        DIDOperationError.InvalidArgument(
          s"serviceEndpoint URIs must be normalized: ${nonNormalizedUris.mkString("[", ", ", "]")}"
        )
      )
  }

  protected def validatePreviousOperationHash[T <: PrismDIDOperation](
      operation: T,
      previousOperationHashExtractor: T => ArraySeq[Byte]
  ): Either[DIDOperationError, Unit] = {
    val previousOperationHash = previousOperationHashExtractor(operation)
    if (previousOperationHash.length == 32) Right(())
    else Left(DIDOperationError.InvalidArgument(s"previousOperationHash must have a size of 32 bytes"))
  }

  /** @return true if a given uri is normalized */
  protected def isUriNormalized(uri: URI): Boolean = {
    val normalized = uri.normalize()
    uri.toString == normalized.toString
  }

  /** Checks if a string is a valid URI fragment according to <a
    * href="https://www.rfc-editor.org/rfc/rfc3986#section-3.5">RFC&nbsp;3986&nbsp;section-3.5</a>
    *
    * @param str
    * @return
    *   true if str is a valid URI fragment, otherwise false
    */
  protected def isValidUriFragment(str: String): Boolean = {

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
