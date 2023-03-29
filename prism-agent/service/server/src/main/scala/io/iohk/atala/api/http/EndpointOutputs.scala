package io.iohk.atala.api.http
import sttp.model.StatusCode
import sttp.tapir.EndpointOutput.OneOfVariant
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.{oneOfVariantValueMatcher, *}

object EndpointOutputs {
  private def statusCodeMatcher(
      statusCode: StatusCode
  ): PartialFunction[Any, Boolean] = {
    case ErrorResponse(status, _, _, _, _) if status == statusCode.code => true
  }

  val basicFailures: EndpointOutput[ErrorResponse] =
    oneOf(
      oneOfVariantValueMatcher(
        StatusCode.BadRequest,
        jsonBody[ErrorResponse].description("Invalid request parameters")
      )(statusCodeMatcher(StatusCode.BadRequest)),
      oneOfVariantValueMatcher(
        StatusCode.InternalServerError,
        jsonBody[ErrorResponse].description("Internal server error")
      )(statusCodeMatcher(StatusCode.InternalServerError))
    )

  val basicFailuresAndNotFound = oneOf(
    oneOfVariantValueMatcher(
      StatusCode.NotFound,
      jsonBody[ErrorResponse].description("Resource could not be found")
    )(statusCodeMatcher(StatusCode.NotFound)),
    oneOfVariantValueMatcher(
      StatusCode.BadRequest,
      jsonBody[ErrorResponse].description("Invalid request parameters")
    )(statusCodeMatcher(StatusCode.BadRequest)),
    oneOfVariantValueMatcher(
      StatusCode.InternalServerError,
      jsonBody[ErrorResponse].description("Internal server error")
    )(statusCodeMatcher(StatusCode.InternalServerError))
  )
}
