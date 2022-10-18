package io.iohk.atala.castor.core.model.did

import io.iohk.atala.castor.core.model.ProtoModelHelper
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.shared.models.HexStrings.*

enum PrismDIDVersion(val id: Int) {
  case V0 extends PrismDIDVersion(0)
  case V1 extends PrismDIDVersion(1)
}

/** Represents a [Did] used in PRISM with prism-specific method and keys as [PrismDid]
  */
sealed trait PrismDID {

  val version: PrismDIDVersion

  val versionSpecificId: String

  override def toString: String = did.toString

  def did: DID = DID(
    method = "prism",
    methodSpecificId = version match {
      case PrismDIDVersion.V0 => versionSpecificId
      case _                  => s"${version.id}:$versionSpecificId"
    }
  )

}

final case class PrismDIDV1 private[did] (network: String, suffix: HexString) extends PrismDID {

  override val version: PrismDIDVersion = PrismDIDVersion.V1

  override val versionSpecificId: String = s"$network:$suffix"

}

object PrismDIDV1 extends ProtoModelHelper {
  def fromCreateOperation(op: PublishedDIDOperation.Create): PrismDIDV1 =
    LongFormPrismDIDV1.fromCreateOperation(op).toCanonical
}

final case class LongFormPrismDIDV1 private[did] (network: String, suffix: HexString, encodedState: Base64UrlString)
    extends PrismDID {

  override val version: PrismDIDVersion = PrismDIDVersion.V1

  override val versionSpecificId: String = s"$network:$suffix:$encodedState"

  def toCanonical: PrismDIDV1 = PrismDIDV1(network, suffix)

}

object LongFormPrismDIDV1 extends ProtoModelHelper {
  def fromCreateOperation(op: PublishedDIDOperation.Create): LongFormPrismDIDV1 = {
    val createDIDProto = op.toProto
    val initialState = createDIDProto.toByteArray
    val suffix = HexString.fromByteArray(Sha256.compute(initialState).getValue)
    val encodedState = Base64UrlString.fromByteArray(initialState)
    val network = op.storage.ledgerName
    LongFormPrismDIDV1(network, suffix, encodedState)
  }
}
