package io.iohk.atala.castor.sql.repository

import doobie.ConnectionIO
import doobie.implicits.*

import java.sql.SQLException

object Utils {
  def connectionIOSafe[T](
      connectionIO: ConnectionIO[T]
  ): ConnectionIO[Either[SQLException, T]] =
    connectionIO.attemptSql
}
