package io.iohk.atala.iris.core.repository

import zio.*

/** This component intended to run several combined repository operations in one database transaction. The idea to have
  * repositories traits instantiated with IOConnection and ZIO monads. Former to make possible to combine several
  * operations in one DB transaction, latter to run repository operations without additional hustle.
  */
trait DbRepositoryTransactor[F[_]] {
  def runAtomically[A](action: F[A]): Task[A]
}
