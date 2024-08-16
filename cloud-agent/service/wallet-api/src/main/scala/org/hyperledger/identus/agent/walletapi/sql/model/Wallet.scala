package org.hyperledger.identus.agent.walletapi.sql.model

import io.getquill.{SnakeCase, *}
import io.getquill.doobie.DoobieContext
import org.hyperledger.identus.agent.walletapi.model
import org.hyperledger.identus.shared.models.WalletId

import java.time.Instant
import java.util.UUID

final case class Wallet(
    id: UUID,
    name: String,
    createdAt: Instant,
    updatedAt: Instant,
    seedDigest: Array[Byte]
)

object Wallet {
  def from(wallet: model.Wallet, seedDigest: Array[Byte]): Wallet = {
    Wallet(
      id = wallet.id.toUUID,
      name = wallet.name,
      createdAt = wallet.createdAt,
      updatedAt = wallet.updatedAt,
      seedDigest = seedDigest
    )
  }

  def toModel(wallet: Wallet): model.Wallet = {
    model.Wallet(
      id = WalletId.fromUUID(wallet.id),
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
}
