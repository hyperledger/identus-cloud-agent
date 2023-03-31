package io.iohk.atala.connect.controller.http

final case class CreateConnectionRequest(
    label: Option[String] = None
)
