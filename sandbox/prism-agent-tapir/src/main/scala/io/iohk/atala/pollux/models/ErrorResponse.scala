package io.iohk.atala.pollux.models

import sttp.model.StatusCode
import sttp.tapir.EndpointOutput.OneOf
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.{oneOf, oneOfVariant}
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
case class ErrorResponse(`type`: String, title: String, status: Int, instance: String, details: Option[String])
    extends FailureResponse

object ErrorResponse {
  given encoder: zio.json.JsonEncoder[ErrorResponse] = DeriveJsonEncoder.gen[ErrorResponse]
  given decoder: zio.json.JsonDecoder[ErrorResponse] = DeriveJsonDecoder.gen[ErrorResponse]
}

case class UnauthorizedResponse(message: String) extends FailureResponse

object UnauthorizedResponse {
  given encoder: zio.json.JsonEncoder[UnauthorizedResponse] = DeriveJsonEncoder.gen[UnauthorizedResponse]
  given decoder: zio.json.JsonDecoder[UnauthorizedResponse] = DeriveJsonDecoder.gen[UnauthorizedResponse]
}

case class UnknownResponse(message: String) extends FailureResponse

object UnknownResponse {
  given encoder: zio.json.JsonEncoder[UnknownResponse] = DeriveJsonEncoder.gen[UnknownResponse]
  given decoder: zio.json.JsonDecoder[UnknownResponse] = DeriveJsonDecoder.gen[UnknownResponse]
}

//    ErrorResponse:
//      type: object
//      description: An RFC-7807 compliant data structure for reporting errors to the client
//      required:
//        - type
//        - title
//        - status
//        - instance
//      properties:
//        type:
//          type: string
//          description: A URI reference that identifies the problem type.
//          example: https://example.org/doc/#model-MalformedEmail
//        title:
//          type: string
//          example: "Malformed email"
//          description: |-
//            A short, human-readable summary of the problem type. It does not
//            change from occurrence to occurrence of the problem.
//        status:
//          type: integer
//          format: int32
//          example: 400
//          description: |-
//            The HTTP status code for this occurrence of the problem.
//        detail:
//          type: string
//          description: |-
//            A human-readable explanation specific to this occurrence of the problem.
//          example: "The received '{}à!è@!.b}' email does not conform to the email format"
//        instance:
//          type: string
//          example: "/problems/d914e"
//          description: |-
//            A URI reference that identifies the specific occurrence of the problem.
//            It may or may not yield further information if dereferenced.
