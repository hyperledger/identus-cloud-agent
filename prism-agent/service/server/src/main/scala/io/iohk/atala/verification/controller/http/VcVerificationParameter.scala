package io.iohk.atala.verification.controller.http

import sttp.tapir.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

import java.time.OffsetDateTime

sealed trait VcVerificationParameter

object VcVerificationParameter {
  given encoder: JsonEncoder[VcVerificationParameter] =
    DeriveJsonEncoder.gen[VcVerificationParameter]

  given decoder: JsonDecoder[VcVerificationParameter] =
    DeriveJsonDecoder.gen[VcVerificationParameter]

  given schema: Schema[VcVerificationParameter] = Schema.derived
}

case class DidParameter(aud: String) extends VcVerificationParameter

object DidParameter {
  given encoder: JsonEncoder[DidParameter] =
    DeriveJsonEncoder.gen[DidParameter]

  given decoder: JsonDecoder[DidParameter] =
    DeriveJsonDecoder.gen[DidParameter]

  given schema: Schema[DidParameter] = Schema.derived
}

case class DateTimeParameter(dateTime: OffsetDateTime) extends VcVerificationParameter

object DateTimeParameter {
  given encoder: JsonEncoder[DateTimeParameter] =
    DeriveJsonEncoder.gen[DateTimeParameter]

  given decoder: JsonDecoder[DateTimeParameter] =
    DeriveJsonDecoder.gen[DateTimeParameter]

  given schema: Schema[DateTimeParameter] = Schema.derived
}
