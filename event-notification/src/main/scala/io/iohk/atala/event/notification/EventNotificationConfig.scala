package io.iohk.atala.event.notification

import io.iohk.atala.shared.models.WalletId

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
