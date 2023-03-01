package io.iohk.atala.pollux.core.model.error

sealed trait CredentialRepositoryError extends Throwable

object CredentialRepositoryError {

  sealed trait SQLCredentialRepositoryError extends Throwable {
    def errorMessage: String
  }

  /** @param code
    *   PSQLException's code
    * @param errorMessage
    *   info abour the error code and error
    */
  def fromPSQLException(code: String, errorMessage: String): SQLCredentialRepositoryError =
    code.toIntOption.getOrElse(-1) match
      case 23505 /* PSQLState.UNIQUE_VIOLATION */ => UniqueConstraintViolation(errorMessage = errorMessage)
      case c                                      => SQLErrorCredentialRepositoryError(c, errorMessage)

  final case class UniqueConstraintViolation(errorMessage: String) extends SQLCredentialRepositoryError
  final case class SQLErrorCredentialRepositoryError(code: Int, errorMessage: String)
      extends SQLCredentialRepositoryError
}
