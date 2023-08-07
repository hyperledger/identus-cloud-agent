package io.iohk.atala.shared.db

import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*
import zio.interop.catz.*

trait ContextRef[T]

trait WalletAware
type WalletTask[T] = Task[T] with WalletAware

object Implicits {

  extension [A](ma: ConnectionIO[A]) {
    // TODO: find a better name
    def globalTransact(xa: Transactor[WalletTask]): Task[A] = {
      ma.transact(xa.asInstanceOf[Transactor[Task]])
    }

    def walletTransact(xa: Transactor[WalletTask]): RIO[WalletAccessContext, A] = {
      def walletCxnIO(ctx: WalletAccessContext) =
        for {
          // TODO: set parameter here
          result <- ma
        } yield result

      for {
        ctx <- ZIO.service[WalletAccessContext]
        _ <- ZIO.debug {
          s"""
          | ##### WalletAccesContext #####
          | ${ctx.toString()}
          """.stripMargin
        }
        result <- walletCxnIO(ctx).transact(xa.asInstanceOf[Transactor[Task]])
      } yield result
    }

  }

}
