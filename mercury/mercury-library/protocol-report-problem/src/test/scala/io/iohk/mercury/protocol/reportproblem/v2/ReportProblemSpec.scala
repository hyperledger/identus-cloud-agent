package io.iohk.atala.mercury.protocol.reportproblem.v2

import io.circe.Json
import io.circe.parser.*
import io.circe.syntax.*
import io.iohk.atala.mercury.model.AttachmentDescriptor
import munit.*
import io.iohk.atala.mercury.model._
import zio.*
import cats.syntax.either._
import io.circe._, io.circe.parser._
class ReportProblemSpec extends ZSuite {

  test("ReportProblem") {

    // val reportproblem = ReportProblem.build("")
    val fromDid = DidId("did:prism:test1")
    val toDid = DidId("did:prism:test2")

    val body = ReportProblem.Body(
      code = ProblemCode("e.p.xfer.cant-use-endpoint"),
      comment = Some("Unable to use the {1} endpoint for {2}."),
      args = Some(Seq("https://agents.r.us/inbox", "did:sov:C805sNYhMrjHiqZDTUASHg")),
      escalate_to = Some("mailto:admin@foo.org")
    )
    val reportproblem = ReportProblem(
      id = "7c9de639-c51c-4d60-ab95-103fa613c805",
      from = fromDid,
      to = toDid,
      thid = None,
      pthid = Some("1e513ad4-48c9-444e-9e7e-5b8b45c5e325"),
      ack = Some(Seq("1e513ad4-48c9-444e-9e7e-5b8b45c5e325")),
      body = body
    )

    val expectedProposalJson = parse(s"""{
  "from" : "did:prism:test1",      
  "to" : "did:prism:test2", 
  "type": "https://didcomm.org/report-problem/2.0/problem-report",
  "id": "7c9de639-c51c-4d60-ab95-103fa613c805",
  "pthid": "1e513ad4-48c9-444e-9e7e-5b8b45c5e325",
  "ack": ["1e513ad4-48c9-444e-9e7e-5b8b45c5e325"],
  "body": {
    "code": "e.p.xfer.cant-use-endpoint",
    "comment": "Unable to use the {1} endpoint for {2}.",
    "args": [
      "https://agents.r.us/inbox",
      "did:sov:C805sNYhMrjHiqZDTUASHg"
    ],
    "escalate_to": "mailto:admin@foo.org"
  }
}""".stripMargin).getOrElse(Json.Null)
    val result = reportproblem.asJson.deepDropNullValues

    println("************************")
    println(result)
    println("************************")

    assertEquals(result, expectedProposalJson)
  }

  test("ReportProblemBuild") {

    val fromDid = DidId("did:prism:test1")
    val toDid = DidId("did:prism:test2")

    val reportproblem = ReportProblem.build(
      fromDID = fromDid,
      toDID = toDid,
      pthid = "1e513ad4-48c9-444e-9e7e-5b8b45c5e325",
      code = ProblemCode("e.p.xfer.cant-use-endpoint"),
      comment = Some("Unable to use the {1} endpoint for {2}.")
    )

    val result = reportproblem.asJson.deepDropNullValues

    println("************************")
    println(result)
    println("************************")
    assertEquals(result.hcursor.get[String]("pthid").toOption, Some("1e513ad4-48c9-444e-9e7e-5b8b45c5e325"))
    assertEquals(result.hcursor.get[String]("from").toOption, Some("did:prism:test1"))
    assertEquals(result.hcursor.get[String]("to").toOption, Some("did:prism:test2"))
    assertEquals(
      result.hcursor.get[String]("type").toOption,
      Some("https://didcomm.org/report-problem/2.0/problem-report")
    )
    assertEquals(result.hcursor.downField("body").get[String]("code").toOption, Some("e.p.xfer.cant-use-endpoint"))
    assertEquals(
      result.hcursor.downField("body").get[String]("comment").toOption,
      Some("Unable to use the {1} endpoint for {2}.")
    )

  }

}
