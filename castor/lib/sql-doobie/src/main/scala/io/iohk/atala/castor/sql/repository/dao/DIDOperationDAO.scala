package io.iohk.atala.castor.sql.repository.dao

import doobie.ConnectionIO
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.iohk.atala.castor.sql.model.ConfirmedPublishedDIDOperationRow
import io.iohk.atala.shared.models.HexStrings.HexString

import java.time.Instant

private[sql] object DIDOperationDAO {

  def getConfirmedPublishedDIDOperation(didSuffix: String): ConnectionIO[Seq[ConfirmedPublishedDIDOperationRow]] = {
    sql"""
       | SELECT
       |   o.ledger_name,
       |   o.did_suffix,
       |   o.operation_type,
       |   o.operation_content,
       |   o.anchored_at,
       |   o.block_number,
       |   o.block_index
       | FROM public.confirmed_published_did_operations AS o
       | WHERE o.did_suffix = ${didSuffix.toString}
       """.stripMargin
      .query[ConfirmedPublishedDIDOperationRow]
      .to[Seq]
  }

}
