package io.iohk.atala.agent.walletapi.sql

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.iohk.atala.agent.walletapi.storage.GenericSecret
import io.iohk.atala.agent.walletapi.storage.GenericSecretStorage
import io.iohk.atala.shared.db.ContextAwareTask
import io.iohk.atala.shared.db.Implicits.*
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*
import zio.json.ast.Json

import java.time.Instant

class JdbcGenericSecretStorage(xa: Transactor[ContextAwareTask]) extends GenericSecretStorage {

  override def set[K, V](key: K, secret: V)(implicit ev: GenericSecret[K, V]): RIO[WalletAccessContext, Unit] = {
    val keyPath = ev.keyPath(key)
    val payload = ev.encodeValue(secret)
    val cxnIO = (now: Instant) => sql"""
        | INSERT INTO public.generic_secret(
        |   key,
        |   payload,
        |   created_at,
        |   wallet_id
        | ) values (
        |   ${keyPath},
        |   ${payload},
        |   ${now},
        |   current_setting('app.current_wallet_id')::UUID
        | )
        """.stripMargin.update

    for {
      now <- Clock.instant
      _ <- cxnIO(now).run.transactWallet(xa)
    } yield ()
  }

  override def get[K, V](key: K)(implicit ev: GenericSecret[K, V]): RIO[WalletAccessContext, Option[V]] = {
    val keyPath = ev.keyPath(key)
    val cxnIO = sql"""
         | SELECT payload
         | FROM public.generic_secret
         | WHERE key = ${keyPath}
        """.stripMargin
      .query[Json]
      .option

    cxnIO
      .transactWallet(xa)
      .flatMap(_.fold(ZIO.none)(json => ZIO.fromTry(ev.decodeValue(json)).asSome))
  }

}

object JdbcGenericSecretStorage {
  val layer: URLayer[Transactor[ContextAwareTask], JdbcGenericSecretStorage] =
    ZLayer.fromFunction(new JdbcGenericSecretStorage(_))
}
