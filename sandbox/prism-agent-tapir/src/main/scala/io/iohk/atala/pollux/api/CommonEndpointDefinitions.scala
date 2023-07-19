package io.iohk.atala.pollux.api

import io.iohk.atala.pollux.models.{BadRequest, FailureResponse, InternalServerError}
import sttp.model.StatusCode
import sttp.tapir.EndpointOutput.OneOf
import sttp.model.StatusCode
import sttp.tapir.EndpointOutput.OneOf
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.{oneOf, oneOfVariant}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

object CommonEndpointDefinitions {

  // This is an example of the common output definition for the endpoint that can be reused in the endpoint definition
  val httpErrors: OneOf[FailureResponse, FailureResponse] = oneOf[FailureResponse](
    oneOfVariant(StatusCode.InternalServerError, jsonBody[InternalServerError]),
    oneOfVariant(StatusCode.BadRequest, jsonBody[BadRequest])
  )
}
