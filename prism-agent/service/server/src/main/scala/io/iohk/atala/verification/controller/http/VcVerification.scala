package io.iohk.atala.verification.controller.http

import sttp.tapir.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

enum VcVerification {
  case SignatureVerification extends VcVerification
  case IssuerIdentification extends VcVerification
  case ExpirationCheck extends VcVerification
  case NotBeforeCheck extends VcVerification
  case AudienceCheck extends VcVerification
  case SubjectVerification extends VcVerification
  case IntegrityOfClaims extends VcVerification
  case ComplianceWithStandards extends VcVerification
  case RevocationCheck extends VcVerification
  case AlgorithmVerification extends VcVerification
  case SchemaCheck extends VcVerification
  case SemanticCheckOfClaims extends VcVerification
}

object VcVerification {
  given encoder: JsonEncoder[VcVerification] =
    DeriveJsonEncoder.gen[VcVerification]

  given decoder: JsonDecoder[VcVerification] =
    DeriveJsonDecoder.gen[VcVerification]

  given schema: Schema[VcVerification] = Schema.derivedEnumeration.defaultStringBased

}
