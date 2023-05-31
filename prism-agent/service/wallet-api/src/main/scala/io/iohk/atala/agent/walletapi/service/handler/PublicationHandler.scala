package io.iohk.atala.agent.walletapi.service.handler

import zio.*
import io.iohk.atala.agent.walletapi.model.ManagedDIDState
import io.iohk.atala.castor.core.model.did.PrismDIDOperation
import io.iohk.atala.agent.walletapi.model.error.CommonWalletStorageError
import io.iohk.atala.agent.walletapi.model.error.CommonCryptographyError
import io.iohk.atala.castor.core.model.did.SignedPrismDIDOperation
import io.iohk.atala.agent.walletapi.util.KeyResolver
import scala.collection.immutable.ArraySeq
import io.iohk.atala.castor.core.model.error.DIDOperationError
import io.iohk.atala.castor.core.model.did.ScheduleDIDOperationOutcome
import io.iohk.atala.castor.core.service.DIDService

class PublicationHandler(didService: DIDService, keyResolver: KeyResolver)(masterKeyId: String) {
  def signOperationWithMasterKey[E](state: ManagedDIDState, operation: PrismDIDOperation)(using
      c1: Conversion[CommonWalletStorageError, E],
      c2: Conversion[CommonCryptographyError, E]
  ): IO[E, SignedPrismDIDOperation] = {
    for {
      masterKeyPair <-
        keyResolver
          .getKey(state, masterKeyId)
          .mapError[E](CommonWalletStorageError.apply)
          .someOrElseZIO(
            ZIO.die(Exception("master-key must exists in the wallet for signing DID operation and submit to Node"))
          )
      signedOperation <- ZIO
        .fromTry(masterKeyPair.privateKey.sign(operation.toAtalaOperation.toByteArray))
        .mapError[E](CommonCryptographyError.apply)
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
