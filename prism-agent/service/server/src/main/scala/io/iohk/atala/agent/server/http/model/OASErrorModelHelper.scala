package io.iohk.atala.agent.server.http.model

import akka.http.scaladsl.server.StandardRoute
import io.iohk.atala.agent.openapi.model.ErrorResponse
import io.iohk.atala.agent.walletapi.model.error.{CreateManagedDIDError, PublishManagedDIDError}
import io.iohk.atala.castor.core.model.did.w3c.DIDResolutionErrorRepr
import io.iohk.atala.castor.core.model.error.{DIDOperationError, DIDResolutionError}

import java.util.UUID
import io.iohk.atala.pollux.core.model.error.IssueCredentialError
import io.iohk.atala.connect.core.model.error.ConnectionError

trait ToErrorResponse[E] {
  def toErrorResponse(e: E): ErrorResponse
}

// TODO: properly define error representation for error models
trait OASErrorModelHelper {

  extension [E](e: HttpServiceError[E]) {
    def toOAS(using te: ToErrorResponse[E]): ErrorResponse = {
      e match
        case HttpServiceError.InvalidPayload(msg) =>
          ErrorResponse(
            `type` = "error-type",
            title = "error-title",
            status = 422,
            detail = Some(msg),
            instance = "error-instance"
          )
        case HttpServiceError.DomainError(cause) => te.toErrorResponse(cause)
    }
  }

  given ToErrorResponse[DIDOperationError] with {
    override def toErrorResponse(e: DIDOperationError): ErrorResponse = {
      ErrorResponse(
        `type` = "error-type",
        title = "error-title",
        status = 500,
        detail = Some(e.toString),
        instance = "error-instance"
      )
    }
  }

  given ToErrorResponse[PublishManagedDIDError] with {
    override def toErrorResponse(e: PublishManagedDIDError): ErrorResponse = {
      ErrorResponse(
        `type` = "error-type",
        title = "error-title",
        status = 500,
        detail = Some(e.toString),
        instance = "error-instance"
      )
    }
  }

  given ToErrorResponse[CreateManagedDIDError] with {
    override def toErrorResponse(e: CreateManagedDIDError): ErrorResponse = {
      ErrorResponse(
        `type` = "error-type",
        title = "error-title",
        status = 500,
        detail = Some(e.toString),
        instance = "error-instance"
      )
    }
  }

  given ToErrorResponse[DIDResolutionErrorRepr] with {
    override def toErrorResponse(e: DIDResolutionErrorRepr): ErrorResponse = {
      import DIDResolutionErrorRepr.*
      val status = e match {
        case InvalidDID                 => 422
        case InvalidDIDUrl              => 422
        case NotFound                   => 404
        case RepresentationNotSupported => 422
        case InternalError              => 500
        case InvalidPublicKeyLength     => 422
        case InvalidPublicKeyType       => 422
        case UnsupportedPublicKeyType   => 422
      }
      ErrorResponse(
        `type` = "error-type",
        title = e.value,
        status = status,
        detail = Some(e.toString),
        instance = "error-instance"
      )
    }
  }

  given ToErrorResponse[IssueCredentialError] with {
    def toErrorResponse(error: IssueCredentialError): ErrorResponse = {
      ErrorResponse(
        `type` = "error-type",
        title = "error-title",
        status = 500,
        detail = Some(error.toString),
        instance = "error-instance"
      )
    }
  }

  given ToErrorResponse[ConnectionError] with {
    def toErrorResponse(error: ConnectionError): ErrorResponse = {
      ErrorResponse(
        `type` = "error-type",
        title = "error-title",
        status = 500,
        detail = Some(error.toString),
        instance = "error-instance"
      )
    }
  }

  def notFoundErrorResponse(detail: Option[String] = None) = ErrorResponse(
    `type` = "not-found",
    title = "Resource not found",
    status = 404,
    detail = detail,
    instance = UUID.randomUUID().toString
  )

}
