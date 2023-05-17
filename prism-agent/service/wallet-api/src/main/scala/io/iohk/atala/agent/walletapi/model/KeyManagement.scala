package io.iohk.atala.agent.walletapi.model

import io.iohk.atala.castor.core.model.did.VerificationRelationship
import io.iohk.atala.castor.core.model.did.InternalKeyPurpose
import io.iohk.atala.agent.walletapi.crypto.DerivationPath
import io.circe.Derivation
import io.iohk.atala.agent.walletapi.crypto.ECKeyPair

enum KeyManagementMode {
  case Random extends KeyManagementMode
  case HD extends KeyManagementMode
}

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
final case class ManagedDidHdKeyCounter(
    didIndex: Int,
    verificationRelationship: VerificationRelationshipCounter,
    internalKey: InternalKeyCounter
) {
  def next(keyUsage: VerificationRelationship | InternalKeyPurpose): ManagedDidHdKeyCounter = {
    keyUsage match {
      case i: VerificationRelationship => copy(verificationRelationship = verificationRelationship.next(i))
      case i: InternalKeyPurpose       => copy(internalKey = internalKey.next(i))
    }
  }

  def path(keyUsage: VerificationRelationship | InternalKeyPurpose): ManagedDidHdKeyPath = {
    val keyIndex = keyUsage match {
      case VerificationRelationship.AssertionMethod      => verificationRelationship.assertionMethod
      case VerificationRelationship.KeyAgreement         => verificationRelationship.keyAgreement
      case VerificationRelationship.CapabilityInvocation => verificationRelationship.capabilityInvocation
      case VerificationRelationship.CapabilityDelegation => verificationRelationship.capabilityDelegation
      case VerificationRelationship.Authentication       => verificationRelationship.authentication
      case InternalKeyPurpose.Master                     => internalKey.master
      case InternalKeyPurpose.Revocation                 => internalKey.revocation
    }
    ManagedDidHdKeyPath(didIndex, keyUsage, keyIndex)
  }
}

object ManagedDidHdKeyCounter {
  def zero(didIndex: Int): ManagedDidHdKeyCounter =
    ManagedDidHdKeyCounter(didIndex, VerificationRelationshipCounter.zero, InternalKeyCounter.zero)
}

final case class ManagedDidHdKeyPath(
    didIndex: Int,
    keyUsage: VerificationRelationship | InternalKeyPurpose,
    keyIndex: Int
) {
  def derivationPath: Seq[DerivationPath] =
    Seq(
      DerivationPath.Hardened(0x1d), // TODO: confirm the value of wallet purpose
      DerivationPath.Hardened(didIndex),
      DerivationPath.Hardened(keyUsageIndex),
      DerivationPath.Hardened(keyIndex)
    )

  def keyUsageIndex: Int = mapKeyUsageIndex(keyUsage)

  private def mapKeyUsageIndex(keyUsage: VerificationRelationship | InternalKeyPurpose): Int = {
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

private[walletapi] final case class CreateDIDRandKey(
    keyPairs: Map[String, ECKeyPair],
    internalKeyPairs: Map[String, ECKeyPair]
)

private[walletapi] final case class UpdateDIDRandKey(newKeyPairs: Map[String, ECKeyPair])

private[walletapi] final case class CreateDidHdKey(
    keyPaths: Map[String, ManagedDidHdKeyPath],
    internalKeyPaths: Map[String, ManagedDidHdKeyPath],
    counter: ManagedDidHdKeyCounter
)
