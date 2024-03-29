package io.iohk.atala.verification.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.system.controller.http.HealthInfo.annotations
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

final case class VcVerificationRequests(
    @description(VcVerificationRequests.annotations.vcVerificationRequests.description)
    @encodedExample(VcVerificationRequests.annotations.vcVerificationRequests.example)
    credentialVerificationRequests: List[VcVerificationRequest]
)

final case class VcVerificationRequest(
    @description(VcVerificationRequests.annotations.credential.description)
    @encodedExample(VcVerificationRequests.annotations.credential.example)
    credential: String,
    @description(VcVerificationRequests.annotations.vcVerification.description)
    @encodedExample(VcVerificationRequests.annotations.vcVerification.example)
    verifications: List[VcVerification]
)

enum VcVerification(val str: String) {
  case SignatureVerification extends VcVerification("SignatureVerification")
  case IssuerIdentification extends VcVerification("IssuerIdentification")
  case ExpirationCheck extends VcVerification("ExpirationCheck")
  case NotBeforeCheck extends VcVerification("NotBeforeCheck")
  case AudienceCheck extends VcVerification("AudienceCheck")
  case SubjectVerification extends VcVerification("SubjectVerification")
  case IntegrityOfClaims extends VcVerification("IntegrityOfClaims")
  case ComplianceWithStandards extends VcVerification("ComplianceWithStandards")
  case RevocationCheck extends VcVerification("RevocationCheck")
  case AlgorithmVerification extends VcVerification("AlgorithmVerification")
  case SchemaCheck extends VcVerification("SchemaCheck")
  case SemanticCheckOfClaims extends VcVerification("SemanticCheckOfClaims")
}

object VcVerificationRequests {
  object annotations {

    object credential
        extends Annotation[String](
          description = "Encoded Verifiable Credential to verify",
          example =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        )
    object vcVerification
        extends Annotation[List[VcVerification]](
          description = "The list of Verifications to verify. All verifications run if Verifications left empty",
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
    object vcVerificationRequests
        extends Annotation[List[VcVerificationRequest]](
          description = "A list of VcVerificationRequest",
          example = List(
            VcVerificationRequest(
              "joxNbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
              List.empty
            ),
            VcVerificationRequest(
              "3ODkbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
              List(VcVerification.IssuerIdentification)
            ),
            VcVerificationRequest(
              "pXVCbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
              List(VcVerification.RevocationCheck)
            )
          )
        )

  }

  given verificationEncoder: JsonEncoder[VcVerification] =
    DeriveJsonEncoder.gen[VcVerification]

  given verificationDecoder: JsonDecoder[VcVerification] =
    DeriveJsonDecoder.gen[VcVerification]

  given verificationSchema: Schema[VcVerification] = Schema.derived

  given credentialVerificationRequestEncoder: JsonEncoder[VcVerificationRequest] =
    DeriveJsonEncoder.gen[VcVerificationRequest]

  given credentialVerificationRequestDecoder: JsonDecoder[VcVerificationRequest] =
    DeriveJsonDecoder.gen[VcVerificationRequest]

  given credentialVerificationRequestSchema: Schema[VcVerificationRequest] = Schema.derived

  given credentialVerificationRequestsEncoder: JsonEncoder[VcVerificationRequests] =
    DeriveJsonEncoder.gen[VcVerificationRequests]

  given credentialVerificationRequestsDecoder: JsonDecoder[VcVerificationRequests] =
    DeriveJsonDecoder.gen[VcVerificationRequests]

  given credentialVerificationRequestsSchema: Schema[VcVerificationRequests] = Schema.derived
}
