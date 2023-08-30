package io.iohk.atala.agent.walletapi.model

import io.iohk.atala.shared.models.WalletId

import java.time.Instant

final case class Wallet(
    id: WalletId,
    name: String,
    createdAt: Instant,
    updatedAt: Instant
) {
  def withUpdatedAt(updatedAt: Instant): Wallet = copy(updatedAt = updatedAt)
}

object Wallet {
  def apply(name: String, walletId: WalletId): Wallet = {
    val now = Instant.now
    Wallet(
      id = walletId,
      name = name,
      createdAt = now,
      updatedAt = now,
    )
  }

  def apply(name: String): Wallet = apply(name, WalletId.random)
}
