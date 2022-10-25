package io.iohk.atala.agent.server.http.model

import akka.http.scaladsl.server.StandardRoute
import io.iohk.atala.agent.openapi.model.ErrorResponse
import io.iohk.atala.agent.walletapi.model.error.{CreateManagedDIDError, PublishManagedDIDError}
import io.iohk.atala.castor.core.model.error.DIDOperationError

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

}
