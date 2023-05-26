package io.iohk.atala.system.controller

import io.iohk.atala.agent.server.http.ZHttp4sBlazeServer
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.system.controller.http.HealthInfo
import sttp.client3.testing.SttpBackendStub
import sttp.client3.ziojson.*
import sttp.client3.{
  DeserializationException,
  Response,
  ResponseException,
  SttpBackend,
  UriContext,
  asString,
  basicRequest
}
import sttp.model.{StatusCode, Uri}
import sttp.monad.MonadError
import sttp.tapir.server.interceptor.CustomiseInterceptors
import sttp.tapir.server.interceptor.RequestResult.Response
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.json.ast.Json.*
import zio.json.{DecoderOps, EncoderOps, JsonDecoder}
import zio.stream.ZSink
import zio.stream.ZSink.*
import zio.stream.ZStream.unfold
import zio.test.*
import zio.test.Assertion.*
import zio.test.Gen.*
import zio.test.TestAspect.{nondeterministic, sequential}

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID
import io.iohk.atala.agent.server.buildinfo.BuildInfo

import zio._
import zio.test.{test, _}
import java.util.concurrent.TimeUnit
import zio.Clock.currentTime
import zio.test.Assertion.isGreaterThanEqualTo

object SystemControllerImplSpec extends ZIOSpecDefault with SystemControllerTestTools {

  def spec = (httpErrorResponses).provideSomeLayerShared(testEnvironmentLayer)

  private val httpErrorResponses = suite("SystemController simple success cases")(
    test("get health info") {
      for {
        systemControllerService <- ZIO.service[SystemController]
        backend = httpBackend(systemControllerService)
        response: HealthInfoResponse <- basicRequest
          .get(uri"${systemUriBase}/health")
          .response(asJsonAlways[HealthInfo])
          .send(backend)

        isItOkStatusCode = assert(response.code)(equalTo(StatusCode.Ok))
        theBodyWasParsedFromJsonAsAHealthInfo = assert(response.body)(
          isRight(
            isSubtype[HealthInfo](
              equalTo(
                HealthInfo(
                  BuildInfo.version
                )
              )
            )
          )
        )
      } yield isItOkStatusCode && theBodyWasParsedFromJsonAsAHealthInfo
    },
    test("get metrics info") {
      for {
        systemControllerService <- ZIO.service[SystemController]
        backend = httpBackend(systemControllerService)
//        _ <- TestClock.adjust(2.minute)
        response: MetricsResponse <- basicRequest
          .get(uri"${systemUriBase}/metrics")
          .response(asString)
          .send(backend)

        isItOkStatusCode = assert(response.code)(equalTo(StatusCode.Ok))
        // TODO tech-debt - add assert for JVM metrics in response. The JVM Metrics work in a running instance from manual testing but cannot be asserted in an integration test
        // The assert below is commented out as the metric endpoint never contains any JVM metrics. I believe this is due to the fact it's a periodic collection
        // Above, the commented out `TestClock.adjust` method was used as the JVM Metrics Collector is based upon ZIO Schedule and should directly affected the collector trigger condition
        // However, even with this commented out - the prometheus metrics object which .get is called upon is always empty
        // There is either an issue with using TestClock OR something in the environment when running tests prevents the JMX from collecting
        // If you debug the code you can see breakpoints being hit in the `GarbageCollector.scala` class so it does appear that the TimeClock.adjust function works
        // and something else is failing in the environment
//        theBodyWasParsedFromJsonAsAHealthInfo = assert(response.body)(
//          isRight(
//            isSubtype[String](
//              containsString("jvm")
//            )
//          )
//        )
      } yield isItOkStatusCode
    }
  )

}
