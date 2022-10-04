package io.iohk.atala.castor.core.model.did

import java.net.URL

sealed abstract class DIDStorage

object DIDStorage {
  final case class Cardano(ledgerName: String) extends DIDStorage
  final case class SecondaryStorage(uri: URL) extends DIDStorage
}
