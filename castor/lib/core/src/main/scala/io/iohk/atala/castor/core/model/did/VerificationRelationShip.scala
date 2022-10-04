package io.iohk.atala.castor.core.model.did

sealed trait VerificationRelationShip

object VerificationRelationShip {
  case object Authentication extends VerificationRelationShip

  case object AssertionMethod extends VerificationRelationShip

  case object KeyAgreement extends VerificationRelationShip

  case object CapabilityInvocation extends VerificationRelationShip
}
