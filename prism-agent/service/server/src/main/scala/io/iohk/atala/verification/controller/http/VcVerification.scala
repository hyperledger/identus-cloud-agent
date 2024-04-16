package io.iohk.atala.verification.controller.http

import io.iohk.atala.pollux.core.service
import io.iohk.atala.pollux.core.service.verification.VcVerification as ServiceVcVerification
import sttp.tapir.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

enum VcVerification {
  case SignatureVerification
  case IssuerIdentification
  case ExpirationCheck
  case NotBeforeCheck
  case AudienceCheck
  case SubjectVerification
  case IntegrityOfClaims
  case ComplianceWithStandards
  case RevocationCheck
  case AlgorithmVerification
  case SchemaCheck
  case SemanticCheckOfClaims
}

object VcVerification {
  given encoder: JsonEncoder[VcVerification] =
    DeriveJsonEncoder.gen[VcVerification]

  given decoder: JsonDecoder[VcVerification] =
    DeriveJsonDecoder.gen[VcVerification]

  given schema: Schema[VcVerification] = Schema.derivedEnumeration.defaultStringBased

  def convert(verification: VcVerification): ServiceVcVerification = {
    verification match {
      case SignatureVerification   => ServiceVcVerification.SignatureVerification
      case IssuerIdentification    => ServiceVcVerification.IssuerIdentification
      case ExpirationCheck         => ServiceVcVerification.ExpirationCheck
      case NotBeforeCheck          => ServiceVcVerification.NotBeforeCheck
      case AudienceCheck           => ServiceVcVerification.AudienceCheck
      case SubjectVerification     => ServiceVcVerification.SubjectVerification
      case IntegrityOfClaims       => ServiceVcVerification.IntegrityOfClaims
      case ComplianceWithStandards => ServiceVcVerification.ComplianceWithStandards
      case RevocationCheck         => ServiceVcVerification.RevocationCheck
      case AlgorithmVerification   => ServiceVcVerification.AlgorithmVerification
      case SchemaCheck             => ServiceVcVerification.SchemaCheck
      case SemanticCheckOfClaims   => ServiceVcVerification.SemanticCheckOfClaims
    }
  }

  def toService(verification: ServiceVcVerification): VcVerification = {
    verification match {
      case ServiceVcVerification.SignatureVerification   => SignatureVerification
      case ServiceVcVerification.IssuerIdentification    => IssuerIdentification
      case ServiceVcVerification.ExpirationCheck         => ExpirationCheck
      case ServiceVcVerification.NotBeforeCheck          => NotBeforeCheck
      case ServiceVcVerification.AudienceCheck           => AudienceCheck
      case ServiceVcVerification.SubjectVerification     => SubjectVerification
      case ServiceVcVerification.IntegrityOfClaims       => IntegrityOfClaims
      case ServiceVcVerification.ComplianceWithStandards => ComplianceWithStandards
      case ServiceVcVerification.RevocationCheck         => RevocationCheck
      case ServiceVcVerification.AlgorithmVerification   => AlgorithmVerification
      case ServiceVcVerification.SchemaCheck             => SchemaCheck
      case ServiceVcVerification.SemanticCheckOfClaims   => SemanticCheckOfClaims
    }
  }
}
