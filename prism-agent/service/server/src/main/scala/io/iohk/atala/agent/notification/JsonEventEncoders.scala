package io.iohk.atala.agent.notification

import io.iohk.atala.agent.walletapi.model.ManagedDIDDetail
import io.iohk.atala.castor.controller.http.ManagedDID
import io.iohk.atala.castor.controller.http.ManagedDID.*
import io.iohk.atala.connect.controller.http.Connection
import io.iohk.atala.connect.controller.http.Connection.*
import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.event.notification.{Event, EventNotificationServiceError}
import io.iohk.atala.issue.controller.http.IssueCredentialRecord
import io.iohk.atala.issue.controller.http.IssueCredentialRecord.*
import io.iohk.atala.pollux.core.model.{
  IssueCredentialRecord as PolluxIssueCredentialRecord,
  PresentationRecord as PolluxPresentationRecord
}
import io.iohk.atala.presentproof.controller.http.PresentationStatus
import zio.*
import zio.json.*

object JsonEventEncoders {

  implicit val connectionRecordEncoder: JsonEncoder[ConnectionRecord] =
    Connection.encoder.contramap(implicitly[Conversion[ConnectionRecord, Connection]].convert)

  implicit val issueCredentialRecordEncoder: JsonEncoder[PolluxIssueCredentialRecord] =
    IssueCredentialRecord.encoder.contramap(
      implicitly[Conversion[PolluxIssueCredentialRecord, IssueCredentialRecord]].convert
    )

  implicit val presentationRecordEncoder: JsonEncoder[PolluxPresentationRecord] =
    PresentationStatus.encoder.contramap(implicitly[Conversion[PolluxPresentationRecord, PresentationStatus]].convert)

  implicit val managedDIDDetailEncoder: JsonEncoder[ManagedDIDDetail] =
    ManagedDID.encoder.contramap(implicitly[Conversion[ManagedDIDDetail, ManagedDID]].convert)

  implicit def eventEncoder[T](implicit jsonEncoder: JsonEncoder[T]): JsonEncoder[Event[T]] =
    DeriveJsonEncoder.gen[Event[T]]
}
