package io.iohk.atala.castor.core.model.did

sealed trait VerificationRelationship

object VerificationRelationship {
  case object Authentication extends VerificationRelationship

  case object AssertionMethod extends VerificationRelationship

  case object KeyAgreement extends VerificationRelationship

  case object CapabilityInvocation extends VerificationRelationship
}
