package io.iohk.atala.event.notification

import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import zio.*

import java.net.URL
import java.time.Instant
import java.util.UUID

final case class EventNotificationConfig(
    id: UUID,
    walletId: WalletId,
    url: URL,
    customHeaders: Map[String, String],
    createdAt: Instant
)

object EventNotificationConfig {
  def apply(walletId: WalletId, url: URL, customHeaders: Map[String, String] = Map.empty): EventNotificationConfig =
    EventNotificationConfig(
      id = UUID.randomUUID(),
      walletId = walletId,
      url = url,
      customHeaders = customHeaders,
      createdAt = Instant.now
    )

  def applyWallet(url: URL, customHeaders: Map[String, String]): URIO[WalletAccessContext, EventNotificationConfig] =
    ZIO.serviceWith[WalletAccessContext](ctx => apply(ctx.walletId, url, customHeaders))

  def applyWallet(url: URL): URIO[WalletAccessContext, EventNotificationConfig] =
    ZIO.serviceWith[WalletAccessContext](ctx => apply(ctx.walletId, url, Map.empty))
}
