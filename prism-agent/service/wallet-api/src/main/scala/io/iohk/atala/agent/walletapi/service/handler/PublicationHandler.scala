package io.iohk.atala.agent.walletapi.service.handler

import io.iohk.atala.agent.walletapi.model.ManagedDIDState
import io.iohk.atala.agent.walletapi.model.error.CommonCryptographyError
import io.iohk.atala.agent.walletapi.model.error.CommonWalletStorageError
import io.iohk.atala.agent.walletapi.util.KeyResolver
import io.iohk.atala.castor.core.model.did.PrismDIDOperation
import io.iohk.atala.castor.core.model.did.ScheduleDIDOperationOutcome
import io.iohk.atala.castor.core.model.did.SignedPrismDIDOperation
import io.iohk.atala.castor.core.model.error.DIDOperationError
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.shared.crypto.Secp256k1KeyPair
import io.iohk.atala.shared.models.WalletAccessContext
import scala.collection.immutable.ArraySeq
import scala.language.implicitConversions
import zio.*

class PublicationHandler(didService: DIDService, keyResolver: KeyResolver)(masterKeyId: String) {
  def signOperationWithMasterKey[E](state: ManagedDIDState, operation: PrismDIDOperation)(using
      c1: Conversion[CommonWalletStorageError, E],
      c2: Conversion[CommonCryptographyError, E]
  ): ZIO[WalletAccessContext, E, SignedPrismDIDOperation] = {
    for {
      masterKeyPair <-
        keyResolver
          .getKey(state.did, masterKeyId)
          .someOrFail(Exception("master-key must exists in the wallet for signing DID operation and submit to Node"))
          .collect(Exception("master-key must be secp256k1 key")) { case keyPair: Secp256k1KeyPair => keyPair }
          .orDie
      signedOperation <- ZIO
        .succeed(masterKeyPair.privateKey.sign(operation.toAtalaOperation.toByteArray))
        .map(signature =>
          SignedPrismDIDOperation(
            operation = operation,
            signature = ArraySeq.from(signature),
            signedWithKey = masterKeyId
          )
        )
    } yield signedOperation
  }

  def submitSignedOperation[E](
      signedOperation: SignedPrismDIDOperation
  )(using c1: Conversion[DIDOperationError, E]): IO[E, ScheduleDIDOperationOutcome] =
    didService.scheduleOperation(signedOperation).mapError[E](e => e)
}
