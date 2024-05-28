package org.hyperledger.identus.didcomm.controller

import org.hyperledger.identus.shared.models.{Failure, StatusCode}

sealed trait DIDCommControllerError(
    val statusCode: StatusCode,
    val userFacingMessage: String
) extends Failure {
  override val namespace = "DIDCommControllerError"
}

object DIDCommControllerError {
  final case class InvalidContentType(maybeContentType: Option[String])
      extends DIDCommControllerError(
        StatusCode.BadRequest,
        maybeContentType match
          case Some(value) => s"The 'content-type' request header value is invalid: $value"
          case None        => s"The 'content-type' request header is undefined"
      )

  final case class UnexpectedError(override val statusCode: StatusCode)
      extends DIDCommControllerError(
        statusCode,
        "An unexpected error occurred while processing your request"
      )
}
