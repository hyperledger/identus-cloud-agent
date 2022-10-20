package io.iohk.atala.api.http

import sttp.model.StatusCode
import sttp.tapir.EndpointOutput.OneOf
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

sealed trait FailureResponse

case class NotFoundResponse(msg: String) extends FailureResponse

object NotFoundResponse {
  given encoder: zio.json.JsonEncoder[NotFoundResponse] = DeriveJsonEncoder.gen[NotFoundResponse]
  given decoder: zio.json.JsonDecoder[NotFoundResponse] = DeriveJsonDecoder.gen[NotFoundResponse]
}

case class BadRequest(msg: String, errors: List[String] = List.empty) extends FailureResponse

object BadRequest {
  given encoder: zio.json.JsonEncoder[BadRequest] = DeriveJsonEncoder.gen[BadRequest]
  given decoder: zio.json.JsonDecoder[BadRequest] = DeriveJsonDecoder.gen[BadRequest]
}

case class InternalServerError(msg: String) extends FailureResponse

object InternalServerError {
  given encoder: zio.json.JsonEncoder[InternalServerError] = DeriveJsonEncoder.gen[InternalServerError]
  given decoder: zio.json.JsonDecoder[InternalServerError] = DeriveJsonDecoder.gen[InternalServerError]
}

//An RFC-7807 compliant data structure for reporting errors to the client
case class ErrorResponse(`type`: String, title: String, status: Int, instance: String, details: Option[String]) extends FailureResponse

object ErrorResponse {
  given encoder: zio.json.JsonEncoder[ErrorResponse] = DeriveJsonEncoder.gen[ErrorResponse]
  given decoder: zio.json.JsonDecoder[ErrorResponse] = DeriveJsonDecoder.gen[ErrorResponse]
}