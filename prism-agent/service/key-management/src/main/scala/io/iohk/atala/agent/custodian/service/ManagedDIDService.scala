package io.iohk.atala.agent.custodian.service

import io.iohk.atala.agent.custodian.crypto.KeyGeneratorWrapper
import io.iohk.atala.agent.custodian.model.ManagedDIDTemplate
import io.iohk.atala.agent.custodian.model.error.CreateManagedDIDError
import io.iohk.atala.agent.custodian.storage.{DIDSecretStorage, InMemoryDIDSecretStorage}
import io.iohk.atala.castor.core.model.did.{DID, EllipticCurve, PrismDID, PublishedDIDOperation}
import zio.*

/** A wrapper around Castor's DIDService providing key-management capability. Analogous to the secretAPI in
  * indy-wallet-sdk.
  */
final class ManagedDIDService private[custodian] (secretStorage: DIDSecretStorage) {

  def createAndStoreDID(didTemplate: ManagedDIDTemplate): IO[CreateManagedDIDError, PrismDID] = {
    for {
      _ <- ZIO.logInfo("creating and storing custodial DID")
      keys <- ZIO
        .foreach(didTemplate.publicKeys.sortBy(_.id))(template =>
          ZIO
            .fromTry(KeyGeneratorWrapper.generateECKeyPair(EllipticCurve.SECP256K1))
            .map(template -> _)
        )
        .mapError(CreateManagedDIDError.KeyGenerationError.apply)
      _ <- ZIO.die(RuntimeException("DIE!!!"))
    } yield ???
  }

}

object ManagedDIDService {
  def inMemoryStorage: ULayer[ManagedDIDService] =
    InMemoryDIDSecretStorage.layer >>> ZLayer.fromFunction(ManagedDIDService(_))
}
