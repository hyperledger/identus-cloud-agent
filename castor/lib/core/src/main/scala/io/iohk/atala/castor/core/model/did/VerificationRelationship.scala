package io.iohk.atala.castor.core.model.did

enum VerificationRelationship(val name: String) {
  case Authentication extends VerificationRelationship("authentication")
  case AssertionMethod extends VerificationRelationship("assertionMethod")
  case KeyAgreement extends VerificationRelationship("keyAgreement")
  case CapabilityInvocation extends VerificationRelationship("capabilityInvocation")
}

object VerificationRelationship {

  private val lookup = VerificationRelationship.values.map(i => i.name -> i).toMap

  def parseString(s: String): Option[VerificationRelationship] = lookup.get(s)

}
