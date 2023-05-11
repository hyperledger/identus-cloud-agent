package io.iohk.atala.agent.walletapi.model

import io.iohk.atala.castor.core.model.did.VerificationRelationship
import io.iohk.atala.castor.core.model.did.InternalKeyPurpose
import io.iohk.atala.agent.walletapi.crypto.DerivationPath
import io.circe.Derivation

final case class VerificationRelationshipCounter(
    authentication: Int,
    assertionMethod: Int,
    keyAgreement: Int,
    capabilityInvocation: Int,
    capabilityDelegation: Int,
) {
  def next(keyUsage: VerificationRelationship): VerificationRelationshipCounter = {
    keyUsage match {
      case VerificationRelationship.Authentication       => copy(authentication = authentication + 1)
      case VerificationRelationship.AssertionMethod      => copy(assertionMethod = assertionMethod + 1)
      case VerificationRelationship.KeyAgreement         => copy(keyAgreement = keyAgreement + 1)
      case VerificationRelationship.CapabilityInvocation => copy(capabilityInvocation = capabilityInvocation + 1)
      case VerificationRelationship.CapabilityDelegation => copy(capabilityDelegation = capabilityDelegation + 1)
    }
  }
}

object VerificationRelationshipCounter {
  def zero: VerificationRelationshipCounter = VerificationRelationshipCounter(0, 0, 0, 0, 0)
}

final case class InternalKeyCounter(
    master: Int,
    revocation: Int
) {
  def next(keyUsage: InternalKeyPurpose): InternalKeyCounter = {
    keyUsage match {
      case InternalKeyPurpose.Master     => copy(master = master + 1)
      case InternalKeyPurpose.Revocation => copy(revocation = revocation + 1)
    }
  }
}

object InternalKeyCounter {
  def zero: InternalKeyCounter = InternalKeyCounter(0, 0)
}

/** Key counter of a single DID */
final case class HDKeyCounter(
    didIndex: Int,
    verificationRelationship: VerificationRelationshipCounter,
    internalKey: InternalKeyCounter
) {
  def next(keyUsage: VerificationRelationship | InternalKeyPurpose): HDKeyCounter = {
    keyUsage match {
      case i: VerificationRelationship => copy(verificationRelationship = verificationRelationship.next(i))
      case i: InternalKeyPurpose       => copy(internalKey = internalKey.next(i))
    }
  }

  def pathOf(keyUsage: VerificationRelationship | InternalKeyPurpose): Seq[DerivationPath] = {
    val usageIndex = keyUsagePath(keyUsage)
    val keyIndex = keyUsage match {
      case InternalKeyPurpose.Master                     => internalKey.master
      case InternalKeyPurpose.Revocation                 => internalKey.revocation
      case VerificationRelationship.AssertionMethod      => verificationRelationship.assertionMethod
      case VerificationRelationship.Authentication       => verificationRelationship.authentication
      case VerificationRelationship.CapabilityDelegation => verificationRelationship.capabilityDelegation
      case VerificationRelationship.CapabilityInvocation => verificationRelationship.capabilityInvocation
      case VerificationRelationship.KeyAgreement         => verificationRelationship.keyAgreement
    }

    Seq(
      DerivationPath.Hardened(0x1d), // TODO: confirm the value of wallet purpose
      DerivationPath.Hardened(didIndex),
      DerivationPath.Hardened(usageIndex),
      DerivationPath.Hardened(keyIndex)
    )
  }

  private def keyUsagePath(keyUsage: VerificationRelationship | InternalKeyPurpose): Int = {
    keyUsage match {
      case InternalKeyPurpose.Master                     => 0
      case VerificationRelationship.AssertionMethod      => 1
      case VerificationRelationship.KeyAgreement         => 2
      case VerificationRelationship.Authentication       => 3
      case InternalKeyPurpose.Revocation                 => 4
      case VerificationRelationship.CapabilityInvocation => 5
      case VerificationRelationship.CapabilityDelegation => 6
    }
  }
}

object HDKeyCounter {
  def zero(didIndex: Int): HDKeyCounter =
    HDKeyCounter(didIndex, VerificationRelationshipCounter.zero, InternalKeyCounter.zero)
}
