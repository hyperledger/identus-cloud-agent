package org.hyperledger.identus.verification.controller.http

import org.hyperledger.identus.api.http.Annotation
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

final case class VcVerificationResponse(
    @description(VcVerificationResponse.annotations.credential.description)
    @encodedExample(VcVerificationResponse.annotations.credential.example)
    credential: String,
    @description(VcVerificationResponse.annotations.vcVerificationResults.description)
    @encodedExample(VcVerificationResponse.annotations.vcVerificationResults.example)
    result: List[VcVerificationResult],
)

object VcVerificationResponse {

  object annotations {

    object credential
        extends Annotation[String](
          description = "Encoded Verifiable Credential to verify",
          example =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        )

    object vcVerificationResults
        extends Annotation[List[VcVerificationResult]](
          description = "The list executed Verifications",
          example = List(
            VcVerificationResult(VcVerification.SignatureVerification, true),
            VcVerificationResult(VcVerification.IssuerIdentification, true),
            VcVerificationResult(VcVerification.ExpirationCheck, true),
            VcVerificationResult(VcVerification.NotBeforeCheck, true),
            VcVerificationResult(VcVerification.AudienceCheck, true),
            VcVerificationResult(VcVerification.SubjectVerification, true),
            VcVerificationResult(VcVerification.IntegrityOfClaims, true),
            VcVerificationResult(VcVerification.ComplianceWithStandards, true),
            VcVerificationResult(VcVerification.RevocationCheck, true),
            VcVerificationResult(VcVerification.AlgorithmVerification, true),
            VcVerificationResult(VcVerification.SchemaCheck, true),
            VcVerificationResult(VcVerification.SemanticCheckOfClaims, true),
          )
        )
  }

  given credentialVerificationRequestEncoder: JsonEncoder[VcVerificationResponse] =
    DeriveJsonEncoder.gen[VcVerificationResponse]

  given credentialVerificationRequestDecoder: JsonDecoder[VcVerificationResponse] =
    DeriveJsonDecoder.gen[VcVerificationResponse]

  given credentialVerificationRequestSchema: Schema[VcVerificationResponse] = Schema.derived

}
