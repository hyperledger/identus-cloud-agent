package io.iohk.atala.verification.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.pollux.core.service.verification.VcVerification
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

final case class VcVerificationResponse(
    @description(VcVerificationResponse.annotations.credential.description)
    @encodedExample(VcVerificationResponse.annotations.credential.example)
    credential: String,
    @description(VcVerificationResponse.annotations.checks.description)
    @encodedExample(VcVerificationResponse.annotations.checks.example)
    checks: List[VcVerification],
    @description(VcVerificationResponse.annotations.successfulChecks.description)
    @encodedExample(VcVerificationResponse.annotations.successfulChecks.example)
    successfulChecks: List[VcVerification],
    @description(VcVerificationResponse.annotations.failedChecks.description)
    @encodedExample(VcVerificationResponse.annotations.failedChecks.example)
    failedChecks: List[VcVerification],
    @description(VcVerificationResponse.annotations.failedAsWarningChecks.description)
    @encodedExample(VcVerificationResponse.annotations.failedAsWarningChecks.example)
    failedAsWarningChecks: List[VcVerification]
)

object VcVerificationResponse {

  object annotations {

    object credential
        extends Annotation[String](
          description = "Encoded Verifiable Credential to verify",
          example =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        )

    object checks
        extends Annotation[List[VcVerification]](
          description = "The list executed Verifications",
          example = List(
            VcVerification.SignatureVerification,
            VcVerification.IssuerIdentification,
            VcVerification.ExpirationCheck,
            VcVerification.NotBeforeCheck,
            VcVerification.AudienceCheck,
            VcVerification.SubjectVerification,
            VcVerification.IntegrityOfClaims,
            VcVerification.ComplianceWithStandards,
            VcVerification.RevocationCheck,
            VcVerification.AlgorithmVerification,
            VcVerification.SchemaCheck,
            VcVerification.SemanticCheckOfClaims,
          )
        )

    object successfulChecks
        extends Annotation[List[VcVerification]](
          description = "The list of successful Verifications",
          example = List(
            VcVerification.SignatureVerification,
            VcVerification.IssuerIdentification,
            VcVerification.ExpirationCheck,
            VcVerification.NotBeforeCheck,
            VcVerification.AudienceCheck,
            VcVerification.SubjectVerification,
            VcVerification.IntegrityOfClaims,
            VcVerification.ComplianceWithStandards,
            VcVerification.RevocationCheck,
            VcVerification.AlgorithmVerification,
            VcVerification.SchemaCheck,
            VcVerification.SemanticCheckOfClaims,
          )
        )

    object failedChecks
        extends Annotation[List[VcVerification]](
          description = "The list of failed Verifications.",
          example = List(
            VcVerification.SignatureVerification,
            VcVerification.IssuerIdentification,
            VcVerification.ExpirationCheck,
            VcVerification.NotBeforeCheck,
            VcVerification.AudienceCheck,
            VcVerification.SubjectVerification,
            VcVerification.IntegrityOfClaims,
            VcVerification.ComplianceWithStandards,
            VcVerification.RevocationCheck,
            VcVerification.AlgorithmVerification,
            VcVerification.SchemaCheck,
            VcVerification.SemanticCheckOfClaims,
          )
        )

    object failedAsWarningChecks
        extends Annotation[List[VcVerification]](
          description = "The list of failed Verifications as warning",
          example = List(
            VcVerification.SignatureVerification,
            VcVerification.IssuerIdentification,
            VcVerification.ExpirationCheck,
            VcVerification.NotBeforeCheck,
            VcVerification.AudienceCheck,
            VcVerification.SubjectVerification,
            VcVerification.IntegrityOfClaims,
            VcVerification.ComplianceWithStandards,
            VcVerification.RevocationCheck,
            VcVerification.AlgorithmVerification,
            VcVerification.SchemaCheck,
            VcVerification.SemanticCheckOfClaims,
          )
        )
  }

  given credentialVerificationRequestEncoder: JsonEncoder[VcVerificationResponse] =
    DeriveJsonEncoder.gen[VcVerificationResponse]

  given credentialVerificationRequestDecoder: JsonDecoder[VcVerificationResponse] =
    DeriveJsonDecoder.gen[VcVerificationResponse]

  given credentialVerificationRequestSchema: Schema[VcVerificationResponse] = Schema.derived

}
