package io.iohk.atala.castor.core.model.did

import java.net.URL

sealed abstract class DIDStorage

object DIDStorage {
  final case class Cardano(ledgerName: String) extends DIDStorage
}
