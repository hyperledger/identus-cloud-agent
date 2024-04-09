package io.iohk.atala.pollux.core.service.verification

import io.iohk.atala.pollux.core.service.verification.VcVerificationFailureType.ERROR
import sttp.tapir.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

enum VcVerificationFailureType {
  case WARN extends VcVerificationFailureType
  case ERROR extends VcVerificationFailureType
}

enum VcVerification(
    val failureType: VcVerificationFailureType
) {
  case SignatureVerification extends VcVerification(ERROR)
  case IssuerIdentification extends VcVerification(ERROR)
  case ExpirationCheck extends VcVerification(ERROR)
  case NotBeforeCheck extends VcVerification(ERROR)
  case AudienceCheck extends VcVerification(ERROR)
  case SubjectVerification extends VcVerification(ERROR)
  case IntegrityOfClaims extends VcVerification(ERROR)
  case ComplianceWithStandards extends VcVerification(ERROR)
  case RevocationCheck extends VcVerification(ERROR)
  case AlgorithmVerification extends VcVerification(ERROR)
  case SchemaCheck extends VcVerification(ERROR)
  case SemanticCheckOfClaims extends VcVerification(ERROR)
}

object VcVerification {
  given encoder: JsonEncoder[VcVerification] =
    DeriveJsonEncoder.gen[VcVerification]

  given decoder: JsonDecoder[VcVerification] =
    DeriveJsonDecoder.gen[VcVerification]

  given schema: Schema[VcVerification] = Schema.derivedEnumeration.defaultStringBased
}
