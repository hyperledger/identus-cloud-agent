package io.iohk.atala.agent.custodian.service

import io.iohk.atala.agent.custodian.keystore.{DIDKeyStorage, InMemoryDIDKeyStorage}
import io.iohk.atala.castor.core.model.did.DID
import io.iohk.atala.shared.models.HexStrings.HexString
import zio.*

// TODO: implement
/** A wrapper around Castor's DIDService providing custodian capabilities */
final class CustodialDIDService(store: DIDKeyStorage) {

  def createAndStoreDID(publishToLedger: Boolean = false): UIO[DID] = ???

  def signWithDID(did: DID, keyId: String, bytes: HexString): UIO[HexString] = ???

}

object CustodialDIDService {
  def inMemoryStorage: ULayer[CustodialDIDService] =
    InMemoryDIDKeyStorage.layer >>> ZLayer.fromFunction(CustodialDIDService(_))
}
