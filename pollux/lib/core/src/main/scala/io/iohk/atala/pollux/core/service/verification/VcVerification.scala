package io.iohk.atala.pollux.core.service.verification

sealed trait VcVerification

object VcVerification {
  case object SignatureVerification extends VcVerification

  case object IssuerIdentification extends VcVerification

  case object ExpirationCheck extends VcVerification

  case object NotBeforeCheck extends VcVerification

  case class AudienceCheck(aud: String) extends VcVerification

  case object SubjectVerification extends VcVerification

  case object IntegrityOfClaims extends VcVerification

  case object ComplianceWithStandards extends VcVerification

  case object RevocationCheck extends VcVerification

  case object AlgorithmVerification extends VcVerification

  case object SchemaCheck extends VcVerification

  case object SemanticCheckOfClaims extends VcVerification
}
