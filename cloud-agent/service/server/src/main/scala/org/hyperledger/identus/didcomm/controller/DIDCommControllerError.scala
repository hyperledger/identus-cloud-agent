package org.hyperledger.identus.didcomm.controller

import org.hyperledger.identus.mercury.model.DidId
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

  final case class RecipientNotFoundError()
      extends DIDCommControllerError(
        StatusCode.UnprocessableContent,
        "Recipient not found in the DIDComm Message"
      )

  final case class UnexpectedError(override val statusCode: StatusCode)
      extends DIDCommControllerError(
        statusCode,
        "An unexpected error occurred while processing your request"
      )

  final case class RequestBodyParsingError(cause: String)
      extends DIDCommControllerError(
        StatusCode.BadRequest,
        s"Unable to parse the request body as a valid DIDComm message: $cause"
      )

  final case class PeerDIDNotFoundError(did: DidId)
      extends DIDCommControllerError(
        StatusCode.UnprocessableContent,
        s"The Peer DID was not found in this agent: ${did.value}"
      )

  final case class PeerDIDKeyNotFoundError(did: DidId, keyId: String)
      extends DIDCommControllerError(
        StatusCode.UnprocessableContent,
        s"The Peer DID does not contain the required key: DID=$did, keyId=$keyId"
      )
}
