package io.iohk.atala.verification.controller.http

import io.iohk.atala.pollux.core.service.verification.{
  AudienceParameter as ServiceAudienceParameter,
  VcVerificationParameter as ServiceVcVerificationParameter
}
import sttp.tapir.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

sealed trait VcVerificationParameter

object VcVerificationParameter {
  given encoder: JsonEncoder[VcVerificationParameter] =
    DeriveJsonEncoder.gen[VcVerificationParameter]

  given decoder: JsonDecoder[VcVerificationParameter] =
    DeriveJsonDecoder.gen[VcVerificationParameter]

  given schema: Schema[VcVerificationParameter] = Schema.derived

  def convert(parameter: VcVerificationParameter): ServiceVcVerificationParameter = {
    parameter match
      case AudienceParameter(aud) => ServiceAudienceParameter(aud)
  }
}

case class AudienceParameter(aud: String) extends VcVerificationParameter
object AudienceParameter {
  given encoder: JsonEncoder[AudienceParameter] =
    DeriveJsonEncoder.gen[AudienceParameter]

  given decoder: JsonDecoder[AudienceParameter] =
    DeriveJsonDecoder.gen[AudienceParameter]

  given schema: Schema[AudienceParameter] = Schema.derived
}
