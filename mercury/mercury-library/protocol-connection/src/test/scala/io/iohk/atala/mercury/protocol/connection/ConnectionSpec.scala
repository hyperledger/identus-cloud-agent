package io.iohk.atala.mercury.protocol.connection

import io.circe._
import io.circe.parser._
import io.circe.syntax._
import zio.*
import munit.*
import io.iohk.atala.mercury.model.*

/** protocolConnection/testOnly io.iohk.atala.mercury.protocol.connection.CoordinateMediationSpec */
class CoordinateMediationSpec extends ZSuite {

  val connectionRequest = """{
    |  "id" : "46430433-d872-4d4c-8376-0fa72cf124c2",
    |  "thid" : "544659a7-a8e3-4101-a918-6afef6ad7bbb",
    |  "typ" : "application/didcomm-plain+json",
    |  "piuri" : "https://atalaprism.io/mercury/connections/1.0/request",
    |  "body" : {"goal_code": "Connect"},
    |  "from" : "did:test:alice",
    |  "to" : ["did:test:bob"]
    |}""".stripMargin // TODO rename 'piuri' to `type`

  // val connectionResponse = """{
  //   |  "id" : "bafa9e69-5ce0-411c-89eb-eb4bade6949d",
  //   |  "typ" : "application/didcomm-plain+json",
  //   |  "type" : "https://didcomm.org/coordinate-mediation/2.0/mediate-grant",
  //   |  "body" : {
  //   |    "routing_did" : "did:peer:2.Ez6LSoUkkHrYaDgGUdF6Vs6NnNyjahQmLKGsHhuZRy92qL1y4.Vz6MknogVLc4883Gys8GjzGZWnu4R1RjiCLDDrDxb7ToaDUeU.SeyJpZCI6Im5ldy1pZCIsInQiOiJkbSIsInMiOiJodHRwOi8vcm9vdHNpZC1tZWRpYXRvcjo4MDAwIiwiYSI6WyJkaWRjb21tL3YyIl19"
  //   |  }
  //   |}""".stripMargin

  test("parse ConnectionRequest") {
    val aux = parse(connectionRequest)
      .flatMap(_.as[Message]) // Message
      .map(e => ConnectionRequest.readFromMessage(e))
    println(aux)
    assertEquals(
      aux,
      Right(
        ConnectionRequest(
          `type` = "https://atalaprism.io/mercury/connections/1.0/request",
          id = "46430433-d872-4d4c-8376-0fa72cf124c2",
          from = DidId("did:test:alice"),
          to = DidId("did:test:bob"),
          thid = Some("544659a7-a8e3-4101-a918-6afef6ad7bbb"),
          body = ConnectionRequest.Body(goal_code = Some("Connect")),
        )
      )
    )
  }

  // test("parse ConnectionResponse") {
  //   val aux = parse(connectionResponse).flatMap(_.as[ConnectionResponse])
  //   assert(aux.isRight)
  // }

}
