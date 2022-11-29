package io.iohk.atala.castor.core.model

package object error {

  sealed trait DIDOperationError
  object DIDOperationError {
    final case class DLTProxyError(cause: Throwable) extends DIDOperationError
    final case class UnexpectedDLTResult(msg: String) extends DIDOperationError
    final case class TooManyDidPublicKeyAccess(limit: Int, access: Option[Int]) extends DIDOperationError
    final case class TooManyDidServiceAccess(limit: Int, access: Option[Int]) extends DIDOperationError
    final case class InvalidArgument(msg: String) extends DIDOperationError
  }

  // errors are based on https://www.w3.org/TR/did-spec-registries/#error
  sealed trait DIDResolutionError

  object DIDResolutionError {
    final case class DLTProxyError(cause: Throwable) extends DIDResolutionError
    final case class UnexpectedDLTResult(msg: String) extends DIDResolutionError
    final case class InvalidEncodedState(msg: String) extends DIDResolutionError
  }

}
