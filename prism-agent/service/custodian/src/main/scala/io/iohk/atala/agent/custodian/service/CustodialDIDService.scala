package io.iohk.atala.agent.custodian.service

import io.iohk.atala.agent.custodian.store.{DIDSecretStorage, InMemoryDIDSecretStorage}
import io.iohk.atala.castor.core.model.did.DID
import io.iohk.atala.shared.models.HexStrings.HexString
import zio.*

// TODO: implement
/** A wrapper around Castor's DIDService providing custodian capabilities */
final class CustodialDIDService(secretStorage: DIDSecretStorage) {

  def createAndStoreDID(): UIO[DID] = ???

}

object CustodialDIDService {
  def inMemoryStorage: ULayer[CustodialDIDService] =
    InMemoryDIDSecretStorage.layer >>> ZLayer.fromFunction(CustodialDIDService(_))
}
