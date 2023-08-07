package io.iohk.atala.shared.db

import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*
import zio.interop.catz.*

trait ContextAware
type ContextfulTask[T] = Task[T] with ContextAware

object Implicits {

  extension [A](ma: ConnectionIO[A]) {
    def transactAny(xa: Transactor[ContextfulTask]): Task[A] = {
      ma.transact(xa.asInstanceOf[Transactor[Task]])
    }

    def transactWallet(xa: Transactor[ContextfulTask]): RIO[WalletAccessContext, A] = {
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
