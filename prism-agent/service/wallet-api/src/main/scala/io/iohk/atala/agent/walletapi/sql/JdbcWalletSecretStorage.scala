package io.iohk.atala.agent.walletapi.sql

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import io.iohk.atala.agent.walletapi.model.WalletSeed
import io.iohk.atala.agent.walletapi.storage.WalletSecretStorage
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import java.time.Instant
import zio.*
import zio.interop.catz.*

class JdbcWalletSecretStorage(xa: Transactor[Task]) extends WalletSecretStorage {

  override def setWalletSeed(seed: WalletSeed): RIO[WalletAccessContext, Unit] = {
    val cxnIO = (now: Instant, walletId: WalletId) => sql"""
        | INSERT INTO public.wallet_seed(
        |   wallet_id,
        |   seed,
        |   created_at
        | ) VALUES (
        |   ${walletId},
        |   ${seed.toByteArray},
        |   ${now}
        | )
        """.stripMargin.update

    for {
      now <- Clock.instant
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      _ <- cxnIO(now, walletId).run.transact(xa)
    } yield ()
  }

  override def getWalletSeed: RIO[WalletAccessContext, Option[WalletSeed]] = {
    val cxnIO = (walletId: WalletId) =>
      sql"""
        | SELECT seed
        | FROM public.wallet_seed
        | WHERE wallet_id = $walletId
        """.stripMargin
        .query[Array[Byte]]
        .option

    ZIO
      .serviceWithZIO[WalletAccessContext](ctx => cxnIO(ctx.walletId).transact(xa))
      .map(_.map(WalletSeed.fromByteArray))
  }

}

object JdbcWalletSecretStorage {
  val layer: URLayer[Transactor[Task], WalletSecretStorage] =
    ZLayer.fromFunction(new JdbcWalletSecretStorage(_))
}
