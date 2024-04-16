package io.iohk.atala.verification.controller.http

import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.pollux.core.service
import io.iohk.atala.pollux.core.service.verification.VcVerification as ServiceVcVerification
import sttp.tapir.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
import zio.{IO, *}

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

  def convert(
      verification: VcVerification,
      maybeParameter: Option[VcVerificationParameter]
  ): IO[ErrorResponse, ServiceVcVerification] = {
    (verification, maybeParameter) match {
      case (SignatureVerification, None)                 => ZIO.succeed(ServiceVcVerification.SignatureVerification)
      case (IssuerIdentification, None)                  => ZIO.succeed(ServiceVcVerification.IssuerIdentification)
      case (ExpirationCheck, None)                       => ZIO.succeed(ServiceVcVerification.ExpirationCheck)
      case (NotBeforeCheck, None)                        => ZIO.succeed(ServiceVcVerification.NotBeforeCheck)
      case (AudienceCheck, Some(AudienceParameter(aud))) => ZIO.succeed(ServiceVcVerification.AudienceCheck(aud))
      case (SubjectVerification, None)                   => ZIO.succeed(ServiceVcVerification.SubjectVerification)
      case (IntegrityOfClaims, None)                     => ZIO.succeed(ServiceVcVerification.IntegrityOfClaims)
      case (ComplianceWithStandards, None)               => ZIO.succeed(ServiceVcVerification.ComplianceWithStandards)
      case (RevocationCheck, None)                       => ZIO.succeed(ServiceVcVerification.RevocationCheck)
      case (AlgorithmVerification, None)                 => ZIO.succeed(ServiceVcVerification.AlgorithmVerification)
      case (SchemaCheck, None)                           => ZIO.succeed(ServiceVcVerification.SchemaCheck)
      case (SemanticCheckOfClaims, None)                 => ZIO.succeed(ServiceVcVerification.SemanticCheckOfClaims)
      case _ =>
        ZIO.fail(
          ErrorResponse.badRequest(detail =
            Some(s"Unsupported Verification:$verification and Parameters:$maybeParameter")
          )
        )
    }
  }

  def toService(verification: ServiceVcVerification): VcVerification = {
    verification match {
      case ServiceVcVerification.SignatureVerification   => SignatureVerification
      case ServiceVcVerification.IssuerIdentification    => IssuerIdentification
      case ServiceVcVerification.ExpirationCheck         => ExpirationCheck
      case ServiceVcVerification.NotBeforeCheck          => NotBeforeCheck
      case ServiceVcVerification.AudienceCheck(_)        => AudienceCheck
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
