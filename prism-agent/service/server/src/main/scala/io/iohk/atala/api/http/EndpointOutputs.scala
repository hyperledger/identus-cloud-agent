package io.iohk.atala.api.http
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.EndpointOutput.OneOfVariant
import sttp.tapir.json.zio.jsonBody

object EndpointOutputs {

  val basicFailures: EndpointOutput[FailureResponse] = oneOf(
    oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[BadRequest].description("Bad request"))),
    oneOfVariant(
      statusCode(StatusCode.InternalServerError).and(jsonBody[InternalServerError].description("Internal server error"))
    )
  )

  val basicFailuresAndNotFound = oneOf(
    oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[NotFound].description("Entity not found"))),
    oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[BadRequest].description("Bad request"))),
    oneOfVariant(
      statusCode(StatusCode.InternalServerError).and(jsonBody[InternalServerError].description("Internal server error"))
    )
  )
}
