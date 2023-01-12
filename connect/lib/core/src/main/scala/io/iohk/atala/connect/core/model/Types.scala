package io.iohk.atala.connect.core.model

type DidId = String
type ConnectionRequest = { def thid: Option[String] }
type ConnectionResponse = {
  def thid: Option[String]
  def id: String
}
type Invitation = Any
