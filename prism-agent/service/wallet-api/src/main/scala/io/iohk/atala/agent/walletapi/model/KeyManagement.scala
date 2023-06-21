package io.iohk.atala.agent.walletapi.model

import io.iohk.atala.castor.core.model.did.VerificationRelationship
import io.iohk.atala.castor.core.model.did.InternalKeyPurpose
import io.iohk.atala.agent.walletapi.crypto.DerivationPath
import io.iohk.atala.agent.walletapi.crypto.ECKeyPair

enum KeyManagementMode {
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
final case class HdKeyIndexCounter(
    didIndex: Int,
    verificationRelationship: VerificationRelationshipCounter,
    internalKey: InternalKeyCounter
) {
  def next(keyUsage: VerificationRelationship | InternalKeyPurpose): HdKeyIndexCounter = {
    keyUsage match {
      case i: VerificationRelationship => copy(verificationRelationship = verificationRelationship.next(i))
      case i: InternalKeyPurpose       => copy(internalKey = internalKey.next(i))
    }
  }

  def path(keyUsage: VerificationRelationship | InternalKeyPurpose): ManagedDIDHdKeyPath = {
    val keyIndex = keyUsage match {
      case VerificationRelationship.AssertionMethod      => verificationRelationship.assertionMethod
      case VerificationRelationship.KeyAgreement         => verificationRelationship.keyAgreement
      case VerificationRelationship.CapabilityInvocation => verificationRelationship.capabilityInvocation
      case VerificationRelationship.CapabilityDelegation => verificationRelationship.capabilityDelegation
      case VerificationRelationship.Authentication       => verificationRelationship.authentication
      case InternalKeyPurpose.Master                     => internalKey.master
      case InternalKeyPurpose.Revocation                 => internalKey.revocation
    }
    ManagedDIDHdKeyPath(didIndex, keyUsage, keyIndex)
  }
}

object HdKeyIndexCounter {
  def zero(didIndex: Int): HdKeyIndexCounter =
    HdKeyIndexCounter(didIndex, VerificationRelationshipCounter.zero, InternalKeyCounter.zero)
}

final case class ManagedDIDHdKeyPath(
    didIndex: Int,
    keyUsage: VerificationRelationship | InternalKeyPurpose,
    keyIndex: Int
) {
  def derivationPath: Seq[DerivationPath] =
    Seq(
      DerivationPath.Hardened(0x1d),
      DerivationPath.Hardened(didIndex),
      DerivationPath.Hardened(keyUsageIndex),
      DerivationPath.Hardened(keyIndex)
    )

  def keyUsageIndex: Int = mapKeyUsageIndex(keyUsage)

  private def mapKeyUsageIndex(keyUsage: VerificationRelationship | InternalKeyPurpose): Int = {
    keyUsage match {
      case InternalKeyPurpose.Master                     => 1
      case VerificationRelationship.AssertionMethod      => 2
      case VerificationRelationship.KeyAgreement         => 3
      case VerificationRelationship.Authentication       => 4
      case InternalKeyPurpose.Revocation                 => 5
      case VerificationRelationship.CapabilityInvocation => 6
      case VerificationRelationship.CapabilityDelegation => 7
    }
  }
}

private[walletapi] final case class CreateDIDRandKey(
    keyPairs: Map[String, ECKeyPair],
    internalKeyPairs: Map[String, ECKeyPair]
)

private[walletapi] final case class UpdateDIDRandKey(newKeyPairs: Map[String, ECKeyPair])

private[walletapi] final case class CreateDIDHdKey(
    keyPaths: Map[String, ManagedDIDHdKeyPath],
    internalKeyPaths: Map[String, ManagedDIDHdKeyPath],
)

private[walletapi] final case class UpdateDIDHdKey(
    newKeyPaths: Map[String, ManagedDIDHdKeyPath],
    counter: HdKeyIndexCounter
)
