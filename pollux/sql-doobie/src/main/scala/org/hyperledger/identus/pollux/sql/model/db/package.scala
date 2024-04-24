package org.hyperledger.identus.pollux.sql.model

import io.getquill.MappedEncoding
import java.util.UUID
import io.iohk.atala.shared.models.WalletId

package object db {

  given MappedEncoding[WalletId, UUID] = MappedEncoding(_.toUUID)
  given MappedEncoding[UUID, WalletId] = MappedEncoding(WalletId.fromUUID)

}
