package io.iohk.atala.castor.core.model

package object error {

  sealed trait DIDOperationError
  object DIDOperationError {
    final case class DLTProxyError(cause: Throwable) extends DIDOperationError
    final case class InvalidArgument(msg: String) extends DIDOperationError
    final case class TooManyDidPublicKeyAccess(limit: Int, access: Option[Int]) extends DIDOperationError
    final case class TooManyDidServiceAccess(limit: Int, access: Option[Int]) extends DIDOperationError
  }

}
