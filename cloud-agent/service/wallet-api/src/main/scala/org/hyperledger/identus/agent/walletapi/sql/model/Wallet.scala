package org.hyperledger.identus.agent.walletapi.sql.model

import io.getquill.{SnakeCase, *}
import io.getquill.doobie.DoobieContext
import org.hyperledger.identus.agent.walletapi.model
import org.hyperledger.identus.shared.models.WalletId

import java.time.Instant
import java.util.UUID

final case class Wallet(
    walletId: UUID,
    name: String,
    createdAt: Instant,
    updatedAt: Instant,
    seedDigest: Array[Byte]
)

object Wallet {
  def from(wallet: model.Wallet, seedDigest: Array[Byte]): Wallet = {
    Wallet(
      walletId = wallet.id.toUUID,
      name = wallet.name,
      createdAt = wallet.createdAt,
      updatedAt = wallet.updatedAt,
      seedDigest = seedDigest
    )
  }

  extension (wallet: Wallet) {
    def toModel: model.Wallet =
      model.Wallet(
        id = WalletId.fromUUID(wallet.walletId),
        name = wallet.name,
        createdAt = wallet.createdAt,
        updatedAt = wallet.updatedAt
      )
  }
}

object WalletSql extends DoobieContext.Postgres(SnakeCase) {
  def insert(wallet: Wallet) = run {
    quote(
      query[Wallet]
        .insertValue(lift(wallet))
    ).returning(w => w)
  }

  def findByIds(walletIds: Seq[WalletId]) = run {
    quote(query[Wallet].filter(p => liftQuery(walletIds.map(_.toUUID)).contains(p.walletId)))
  }

  def findBySeed(seedDigest: Array[Byte]) = run {
    quote(query[Wallet].filter(_.seedDigest == lift(seedDigest)).take(1))
  }
}
