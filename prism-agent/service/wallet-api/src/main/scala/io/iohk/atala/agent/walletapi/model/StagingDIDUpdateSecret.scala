package io.iohk.atala.agent.walletapi.model

import io.iohk.atala.castor.core.model.did.PublishedDIDOperation

final case class StagingDIDUpdateSecret(
    operation: PublishedDIDOperation.Update,
    updateCommitmentSecret: ECKeyPair,
    keyPairs: Map[String, ECKeyPair]
)
