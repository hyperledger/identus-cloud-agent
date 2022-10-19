package io.iohk.atala.castor.sql.repository

import doobie.Meta
import doobie.postgres.implicits.*
import io.iohk.atala.castor.sql.model.OperationType
import io.iohk.atala.shared.models.HexStrings.HexString

package object dao {

  private[sql] given Meta[OperationType] = pgEnumString[OperationType](
    "OPERATION_TYPE",
    OperationType.valueOf,
    _.toString
  )

  private[sql] given Meta[HexString] = Meta[String].timap[HexString](s =>
    HexString.fromString(s).getOrElse(throw IllegalArgumentException(s"unable to convert $s to hex-string"))
  )(_.toString)

}
