package io.iohk.atala.castor.core.model.did

import io.iohk.atala.castor.core.model.HexStrings

sealed trait PublishedDIDOperation

object PublishedDIDOperation {
  final case class Create(
      updateCommitment: HexStrings.HexString,
      recoveryCommitment: HexStrings.HexString,
      storage: DIDStorage.Cardano,
      document: DIDDocument
  ) extends PublishedDIDOperation
}
