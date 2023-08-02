package io.iohk.atala.agent.notification

sealed trait WebhookPublisherError

object WebhookPublisherError {
  case class InvalidWebhookURL(msg: String) extends WebhookPublisherError
  case class UnexpectedError(msg: String) extends WebhookPublisherError
}
