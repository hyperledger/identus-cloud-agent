package org.hyperledger.identus.verification.controller.http

import sttp.tapir.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

import java.time.OffsetDateTime

sealed trait VcVerificationParameter(val parameterType: String)

object VcVerificationParameter {
  given encoder: JsonEncoder[VcVerificationParameter] = DidParameter.encoder
    .orElseEither(DateTimeParameter.encoder)
    .contramap[VcVerificationParameter] {
      case did: DidParameter           => Left(did)
      case dateTime: DateTimeParameter => Right(dateTime)
    }

  given decoder: JsonDecoder[VcVerificationParameter] = DidParameter.decoder
    .orElseEither(DateTimeParameter.decoder)
    .map[VcVerificationParameter] {
      case Left(did)       => did
      case Right(dateTime) => dateTime
    }

  given schema: Schema[VcVerificationParameter] =
    Schema
      .oneOfUsingField[VcVerificationParameter, String](a => a.parameterType, t => t)(
        ("DidParameter", DidParameter.schema),
        ("DateTimeParameter", DateTimeParameter.schema)
      )

}

case class DidParameter(did: String) extends VcVerificationParameter("DidParameter")

object DidParameter {
  given encoder: JsonEncoder[DidParameter] =
    DeriveJsonEncoder.gen[DidParameter]

  given decoder: JsonDecoder[DidParameter] =
    DeriveJsonDecoder.gen[DidParameter]

  given schema: Schema[DidParameter] = Schema.derived
}

case class DateTimeParameter(dateTime: OffsetDateTime) extends VcVerificationParameter("DateTimeParameter")

object DateTimeParameter {
  given encoder: JsonEncoder[DateTimeParameter] =
    DeriveJsonEncoder.gen[DateTimeParameter]

  given decoder: JsonDecoder[DateTimeParameter] =
    DeriveJsonDecoder.gen[DateTimeParameter]

  given schema: Schema[DateTimeParameter] = Schema.derived
}
