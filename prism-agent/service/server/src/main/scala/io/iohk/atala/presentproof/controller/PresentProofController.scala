package io.iohk.atala.presentproof.controller

import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.pollux.core.model.error.PresentationError
import io.iohk.atala.presentproof.controller.http.*
import zio.{IO, ZIO}

import scala.util.Try

trait PresentProofController {
  def requestPresentation(
      requestPresentationInput: RequestPresentationInput
  )(implicit
      rc: RequestContext
  ): IO[ErrorResponse, RequestPresentationOutput]

  def getAllPresentation(
      offset: Option[Int],
      limit: Option[Int],
      thid: Option[String]
  )(implicit
      rc: RequestContext
  ): IO[ErrorResponse, PresentationStatusPage]

  def getPresentation(id: String)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, PresentationStatus]

  def updatePresentation(id: String, requestPresentationAction: RequestPresentationAction)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, PresentationStatus]

}

object PresentProofController {
  def toHttpError(error: PresentationError): ErrorResponse =
    error match
      case PresentationError.RepositoryError(cause) =>
        ErrorResponse.internalServerError(title = "RepositoryError", detail = Some(cause.toString))
      case PresentationError.RecordIdNotFound(recordId) =>
        ErrorResponse.notFound(detail = Some(s"Record Id not found: $recordId"))
      case PresentationError.ThreadIdNotFound(thid) =>
        ErrorResponse.notFound(detail = Some(s"Thread Id not found: $thid"))
      case PresentationError.InvalidFlowStateError(msg) =>
        ErrorResponse.badRequest(title = "InvalidFlowState", detail = Some(msg))
      case PresentationError.UnexpectedError(msg) =>
        ErrorResponse.internalServerError(detail = Some(msg))
      case PresentationError.IssuedCredentialNotFoundError(_) =>
        ErrorResponse.internalServerError(detail = Some("Issued credential not found"))
      case PresentationError.PresentationDecodingError(_) =>
        ErrorResponse.internalServerError(detail = Some("Presentation decoding error"))
      case PresentationError.PresentationNotFoundError(_) =>
        ErrorResponse.notFound(detail = Some("Presentation no found"))
      case PresentationError.HolderBindingError(msg) =>
        ErrorResponse.internalServerError(detail = Some(s"Holder binding error: $msg"))

  def toDidCommID(str: String): ZIO[Any, ErrorResponse, io.iohk.atala.pollux.core.model.DidCommID] =
    ZIO
      .fromTry(Try(io.iohk.atala.pollux.core.model.DidCommID(str)))
      .mapError(e => ErrorResponse.badRequest(s"Error parsing string as DidCommID: ${e.getMessage}"))
}
