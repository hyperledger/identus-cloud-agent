package io.iohk.atala.castor.core.model.did

import io.iohk.atala.castor.core.model.ProtoModelHelper
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.shared.models.HexStrings.*

import scala.util.Try

/** Represents a [Did] used in PRISM with prism-specific method and keys as [PrismDid]
  */
sealed trait PrismDID {
  override def toString: String = did.toString

  final def did: DID = DID(PrismDID.PRISM_METHOD, suffix)

  final def asCanonical: CanonicalPrismDID = CanonicalPrismDID(stateHash)

  def stateHash: HexString

  def suffix: DIDMethodSpecificId

}

object PrismDID {
  val PRISM_METHOD: DIDMethod = DIDMethod("prism")

  def buildCanonical(stateHash: Array[Byte]): CanonicalPrismDID = CanonicalPrismDID(HexString.fromByteArray(stateHash))

  def buildLongFormFromOperation(createOperation: PrismDIDOperation.Create): LongFormPrismDID = {
    import ProtoModelHelper.*
    val createDIDOperation = createOperation.toProto
    val atalaOperation = node_models.AtalaOperation(createDIDOperation)
    val encodedState = atalaOperation.toByteArray
    val encodedStateBase64 = Base64UrlString.fromByteArray(encodedState)
    val stateHash = HexString.fromByteArray(Sha256.compute(encodedState).getValue)
    LongFormPrismDID(stateHash, encodedStateBase64)
  }
}

final case class CanonicalPrismDID private[did] (stateHash: HexString) extends PrismDID {
  override val suffix: DIDMethodSpecificId = DIDMethodSpecificId.fromString(stateHash.toString).get
}

final case class LongFormPrismDID private[did] (stateHash: HexString, encodedState: Base64UrlString) extends PrismDID {
  override val suffix: DIDMethodSpecificId =
    DIDMethodSpecificId.fromString(s"${stateHash.toString}:${encodedState.toString}").get
}
