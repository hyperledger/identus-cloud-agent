package org.hyperledger.identus.pollux.prex

import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.*
import zio.*
import zio.test.*

import scala.io.Source
import scala.util.Using

object PresentationSubmissionSpec extends ZIOSpecDefault {

  final case class ExampleTransportEnvelope(presentation_submission: PresentationSubmission)

  override def spec = suite("PresentationSubmissionSpec")(
    test("parse presentation-submission exmaples from spec") {
      val resourcePaths = Seq(
        "ps/basic_presentation.json",
        "ps/nested_presentation.json",
      )
      ZIO
        .foreach(resourcePaths) { path =>
          ZIO
            .fromTry(Using(Source.fromResource(path))(_.mkString))
            .flatMap(json => ZIO.fromEither(decode[ExampleTransportEnvelope](json)))
            .map(_.presentation_submission)
        }
        .as(assertCompletes)
    }
  )

}
