package io.iohk.atala.castor.core.model.did

enum VerificationRelationship(val name: String) {
  case Authentication extends VerificationRelationship("authentication")
  case AssertionMethod extends VerificationRelationship("assertionMethod")
  case KeyAgreement extends VerificationRelationship("keyAgreement")
  case CapabilityInvocation extends VerificationRelationship("capabilityInvocation")
}
