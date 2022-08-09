package io.iohk.atala.outofband

import io.circe.syntax.EncoderOps

import java.util.Base64
case class CreateInvitation(goal: String, goal_code: String)

case class Invitation(
    id: String,
    `@type`: String,
    label: String,
    body: Body,
    handshake_protocols: Seq[String],
    service: Seq[Service] // FIXME service: Seq[ServiceType]
)

case class CreateInvitationResponse(alias: String, invitation: Invitation, invitationUrl: String)

case class Body(goal: String, goal_code: String, accept: Seq[String])

sealed trait ServiceType

case class Service(
    id: String,
    serviceEndpoint: String,
    `type`: String,
    recipientKeys: Seq[String],
    routingKeys: Seq[String]
) extends ServiceType

case class Did(did: String) extends ServiceType

object CreateInvitationResponse {
  import io.circe.generic.auto._
  val accepts = Seq("didcomm/v2")

  def apply(goal: String, goal_code: String): CreateInvitationResponse = {
    val body = Body(goal, goal_code, accepts)
    val service = Service(
      id = "did:prism:PR6vs6GEZ8rHaVgjg2WodM#did-communication",
      serviceEndpoint = "http://localhost:8080/create-connection",
      `type` = "did-communication",
      recipientKeys = Seq("did:prism:PR6vs6GEZ8rHaVgjg2WodM"),
      routingKeys = Seq("did:prism:PR6vs6GEZ8rHaVgjg2WodM")
    )
    val invitation = Invitation(
      id = "f3375429-b116-4224-b55f-563d7ef461f1",
      `@type` = "https://didcomm.org/out-of-band/2.0/invitation",
      label = "Mediator Invitation",
      body = body,
      handshake_protocols = Seq("https://didcomm.org/didexchange/1.0"),
      service = Seq(service)
    )

    val encodedString = Base64.getUrlEncoder.encodeToString(invitation.asJson.noSpaces.getBytes)
    val invitationUrl = s"http://localhost:8080/invitation?_oob=$encodedString"
    CreateInvitationResponse(alias = "Mediator", invitation, invitationUrl)
  }
}
