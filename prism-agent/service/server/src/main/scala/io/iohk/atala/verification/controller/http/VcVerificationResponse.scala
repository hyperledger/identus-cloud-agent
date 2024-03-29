package io.iohk.atala.verification.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.system.controller.http.HealthInfo.annotations
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

final case class VcVerificationResponses(
    @description(VcVerificationResponses.annotations.vcVerificationResponses.description)
    @encodedExample(VcVerificationResponses.annotations.vcVerificationResponses.example)
    credentialVerificationResponses: List[VcVerificationResponse]
)

final case class VcVerificationResponse(
    @description(VcVerificationResponses.annotations.credential.description)
    @encodedExample(VcVerificationResponses.annotations.credential.example)
    credential: String,
    @description(VcVerificationResponses.annotations.checks.description)
    @encodedExample(VcVerificationResponses.annotations.checks.example)
    checks: List[VcVerification],
    @description(VcVerificationResponses.annotations.successfulChecks.description)
    @encodedExample(VcVerificationResponses.annotations.successfulChecks.example)
    successfulChecks: List[VcVerification],
    @description(VcVerificationResponses.annotations.failedChecks.description)
    @encodedExample(VcVerificationResponses.annotations.failedChecks.example)
    failedChecks: List[VcVerification],
    @description(VcVerificationResponses.annotations.failedAsWarningChecks.description)
    @encodedExample(VcVerificationResponses.annotations.failedAsWarningChecks.example)
    failedAsWarningChecks: List[VcVerification]
)

object VcVerificationResponses {
  object annotations {
    object vcVerificationResponses
        extends Annotation[List[VcVerificationResponse]](
          description = "A list of VcVerificationResponse",
          example = List(
            VcVerificationResponse(
              "joxNbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
              checks = List(
                VcVerification.IssuerIdentification,
                VcVerification.RevocationCheck,
                VcVerification.SignatureVerification
              ),
              successfulChecks = List(
                VcVerification.SignatureVerification,
                VcVerification.RevocationCheck,
                VcVerification.IssuerIdentification
              ),
              failedChecks = List.empty,
              failedAsWarningChecks = List.empty
            ),
            VcVerificationResponse(
              "pXVCbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
              checks = List(
                VcVerification.IssuerIdentification,
                VcVerification.RevocationCheck,
                VcVerification.SignatureVerification
              ),
              successfulChecks = List(VcVerification.SignatureVerification),
              failedChecks = List(VcVerification.RevocationCheck),
              failedAsWarningChecks = List(VcVerification.IssuerIdentification)
            )
          )
        )

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

  given verificationEncoder: JsonEncoder[VcVerification] =
    DeriveJsonEncoder.gen[VcVerification]

  given verificationDecoder: JsonDecoder[VcVerification] =
    DeriveJsonDecoder.gen[VcVerification]

  given verificationSchema: Schema[VcVerification] = Schema.derived

  given credentialVerificationRequestEncoder: JsonEncoder[VcVerificationResponse] =
    DeriveJsonEncoder.gen[VcVerificationResponse]

  given credentialVerificationRequestDecoder: JsonDecoder[VcVerificationResponse] =
    DeriveJsonDecoder.gen[VcVerificationResponse]

  given credentialVerificationRequestSchema: Schema[VcVerificationResponse] = Schema.derived

  given credentialVerificationRequestsEncoder: JsonEncoder[VcVerificationResponses] =
    DeriveJsonEncoder.gen[VcVerificationResponses]

  given credentialVerificationRequestsDecoder: JsonDecoder[VcVerificationResponses] =
    DeriveJsonDecoder.gen[VcVerificationResponses]

  given credentialVerificationRequestsSchema: Schema[VcVerificationResponses] = Schema.derived
}
