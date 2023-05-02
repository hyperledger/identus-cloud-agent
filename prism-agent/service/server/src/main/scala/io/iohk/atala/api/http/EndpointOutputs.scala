package io.iohk.atala.api.http
import sttp.model.StatusCode
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.{oneOfVariantValueMatcher, *}
import sttp.tapir.EndpointOutput.OneOfVariant

object EndpointOutputs {
  private def statusCodeMatcher(
      statusCode: StatusCode
  ): PartialFunction[Any, Boolean] = {
    case ErrorResponse(status, _, _, _, _) if status == statusCode.code => true
  }

  def basicFailuresWith(extraFailures: OneOfVariant[ErrorResponse]*) = {
    oneOf(
      Failure.badRequest,
      (Seq(Failure.internalServerError) ++ extraFailures): _*
    )
  }

  val basicFailures: EndpointOutput[ErrorResponse] = basicFailuresWith()

  val basicFailuresAndNotFound = basicFailuresWith(Failure.notFound)

  object Failure {
    val badRequest = oneOfVariantValueMatcher(
      StatusCode.BadRequest,
      jsonBody[ErrorResponse].description("Invalid request parameters")
    )(statusCodeMatcher(StatusCode.BadRequest))

    val internalServerError = oneOfVariantValueMatcher(
      StatusCode.InternalServerError,
      jsonBody[ErrorResponse].description("Internal server error")
    )(statusCodeMatcher(StatusCode.InternalServerError))

    val notFound = oneOfVariantValueMatcher(
      StatusCode.NotFound,
      jsonBody[ErrorResponse].description("Resource could not be found")
    )(statusCodeMatcher(StatusCode.NotFound))

    val unprocessableEntity = oneOfVariantValueMatcher(
      StatusCode.UnprocessableEntity,
      jsonBody[ErrorResponse].description("Unable to process the request")
    )(statusCodeMatcher(StatusCode.UnprocessableEntity))
  }

}
