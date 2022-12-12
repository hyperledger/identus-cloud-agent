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
    |  "type" : "https://atalaprism.io/mercury/connections/1.0/request",
    |  "body" : {"goal_code": "Connect"},
    |  "from" : "did:test:alice",
    |  "to" : ["did:test:bob"]
    |}""".stripMargin

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

  // TODO
  // test("parse ConnectionResponse") {
  //   val aux = parse(connectionResponse).flatMap(_.as[ConnectionResponse])
  //   assert(aux.isRight)
  // }

}
