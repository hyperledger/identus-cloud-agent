package org.hyperledger.identus.castor.core.model

package object error {

  sealed trait DIDOperationError
  object DIDOperationError {
    final case class DLTProxyError(cause: Throwable) extends DIDOperationError
    final case class UnexpectedDLTResult(msg: String) extends DIDOperationError
    final case class ValidationError(cause: OperationValidationError) extends DIDOperationError
  }

  sealed trait DIDResolutionError

  object DIDResolutionError {
    final case class DLTProxyError(cause: Throwable) extends DIDResolutionError
    final case class UnexpectedDLTResult(msg: String) extends DIDResolutionError
    final case class ValidationError(cause: OperationValidationError) extends DIDResolutionError
  }

  sealed trait OperationValidationError

  object OperationValidationError {
    final case class TooManyDidPublicKeyAccess(limit: Int, access: Option[Int]) extends OperationValidationError
    final case class TooManyDidServiceAccess(limit: Int, access: Option[Int]) extends OperationValidationError
    final case class InvalidArgument(msg: String) extends OperationValidationError
    final case class InvalidPublicKeyData(ids: Seq[String]) extends OperationValidationError
    final case class InvalidMasterKeyType(ids: Seq[String]) extends OperationValidationError
  }

}
