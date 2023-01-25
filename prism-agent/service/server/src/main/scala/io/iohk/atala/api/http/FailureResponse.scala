package io.iohk.atala.api.http

import sttp.model.StatusCode
import sttp.tapir.EndpointOutput.OneOf
import sttp.tapir.Schema
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}
import sttp.tapir.server.model.ValuedEndpointOutput

sealed trait FailureResponse

case class NotFound(msg: String) extends FailureResponse

object NotFound {
  given encoder: zio.json.JsonEncoder[NotFound] = DeriveJsonEncoder.gen[NotFound]
  given decoder: zio.json.JsonDecoder[NotFound] = DeriveJsonDecoder.gen[NotFound]
  given schema: Schema[NotFound] = Schema.derived
}

case class BadRequest(msg: String, errors: List[String] = List.empty) extends FailureResponse

object BadRequest {
  def failureResponseHandler(msg: String): ValuedEndpointOutput[_] =
    ValuedEndpointOutput(jsonBody[BadRequest], BadRequest(msg))
  given encoder: zio.json.JsonEncoder[BadRequest] = DeriveJsonEncoder.gen[BadRequest]
  given decoder: zio.json.JsonDecoder[BadRequest] = DeriveJsonDecoder.gen[BadRequest]
  given schema: Schema[BadRequest] = Schema.derived
}

case class InternalServerError(msg: String) extends FailureResponse

object InternalServerError {
  given encoder: zio.json.JsonEncoder[InternalServerError] = DeriveJsonEncoder.gen[InternalServerError]
  given decoder: zio.json.JsonDecoder[InternalServerError] = DeriveJsonDecoder.gen[InternalServerError]
  given schema: Schema[InternalServerError] = Schema.derived
}

//An RFC-7807 compliant data structure for reporting errors to the client
case class ErrorResponse(`type`: String, title: String, status: Int, instance: String, details: Option[String])
    extends FailureResponse

object ErrorResponse {
  given encoder: zio.json.JsonEncoder[ErrorResponse] = DeriveJsonEncoder.gen[ErrorResponse]
  given decoder: zio.json.JsonDecoder[ErrorResponse] = DeriveJsonDecoder.gen[ErrorResponse]
  given schema: Schema[ErrorResponse] = Schema.derived
}
