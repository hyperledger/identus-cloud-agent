package io.iohk.atala.event.notification

sealed trait EventNotificationServiceError

object EventNotificationServiceError {
  case class EventSendingFailed(msg: String) extends EventNotificationServiceError
}
