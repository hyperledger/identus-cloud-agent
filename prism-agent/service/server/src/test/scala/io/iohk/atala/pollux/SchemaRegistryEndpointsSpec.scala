package io.iohk.atala.pollux

import io.iohk.atala.agent.server.http.ZHttp4sBlazeServer
import io.iohk.atala.api.http.{BadRequest, NotFound}
import io.iohk.atala.pollux.SchemaRegistryEndpointsSpec.schemaReqistrySchemasUri
import io.iohk.atala.pollux.schema.*
import io.iohk.atala.pollux.schema.model.{
  VerifiableCredentialSchema,
  VerifiableCredentialSchemaInput,
  VerifiableCredentialSchemaPage
}
import io.iohk.atala.pollux.service.{SchemaRegistryService, SchemaRegistryServiceInMemory}
import sttp.client3.testing.SttpBackendStub
import sttp.client3.ziojson.*
import sttp.client3.{DeserializationException, ResponseException, SttpBackend, UriContext, basicRequest}
import sttp.model.{StatusCode, Uri}
import sttp.monad.MonadError
import sttp.tapir.server.interceptor.CustomiseInterceptors
import sttp.tapir.server.interceptor.RequestResult.Response
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.json.{DecoderOps, EncoderOps, JsonDecoder}
import zio.stream.ZSink
import zio.stream.ZSink.*
import zio.stream.ZStream.unfold
import zio.test.*
import zio.test.Assertion.*
import zio.test.Gen.*
import zio.{Random, ZIO, ZLayer, *}

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID

object Generators {
  val schemaId = Gen.uuid
  val schemaName = Gen.alphaNumericStringBounded(4, 12)
  val schemaVersion = Gen.int(1, 5).map(i => s"$i.0")
  val schemaDescription = Gen.alphaNumericStringBounded(5, 30)
  val schemaAttribute = Gen.alphaNumericStringBounded(3, 9)
  val schemaAttributes = Gen.setOfBounded(1, 4)(schemaAttribute).map(_.toList)
  val schemaAuthored = Gen.offsetDateTime(min = OffsetDateTime.now().minusMonths(6), max = OffsetDateTime.now())
  val schemaTag: Gen[Any, String] = Gen.alphaNumericStringBounded(3, 5)
  val schemaTags: Gen[Any, List[String]] = Gen.setOfBounded(0, 3)(schemaTag).map(_.toList)

  val schemaInput = for {
    id <- schemaId
    name <- schemaName
    version <- schemaVersion
    description <- schemaDescription
    attributes <- schemaAttributes
    tags <- schemaTags
    authored <- schemaAuthored
  } yield VerifiableCredentialSchemaInput(
    id = Some(id),
    name = name,
    version = version,
    description = Some(description),
    attributes = attributes.toList,
    authored = Some(authored),
    tags = tags.toList
  )
}

object SchemaRegistryEndpointsSpec extends ZIOSpecDefault:

  type SchemaBadRequestResponse = sttp.client3.Response[Either[DeserializationException[String], BadRequest]]
  type SchemaResponse = sttp.client3.Response[Either[DeserializationException[String], VerifiableCredentialSchema]]
  type SchemaPageResponse =
    sttp.client3.Response[Either[DeserializationException[String], VerifiableCredentialSchemaPage]]

  private val schemaId = UUID.randomUUID()

  private val schemaInput = VerifiableCredentialSchemaInput(
    Option(schemaId),
    name = "test schema",
    version = "1.0",
    description = Option("schema description"),
    attributes = List("first_name", "dob"),
    authored = Option(OffsetDateTime.now(ZoneOffset.UTC)),
    tags = List("test")
  )

  private val schemaReqistrySchemasUri = uri"http://test.com/schema-registry/schemas"
  private val schema = VerifiableCredentialSchema(schemaInput)
    .withBaseUri(schemaReqistrySchemasUri)

  def bootstrapOptions[F[_]](monadError: MonadError[F]) = {
    new CustomiseInterceptors[F, Any](_ => ())
      .defaultHandlers(BadRequest.failureResponseHandler)
  }

  def httpBackend(schemaRegistryService: SchemaRegistryService) = {

    val schemaRegistryEndpoints = SchemaRegistryServerEndpoints(schemaRegistryService)

    val backend =
      TapirStubInterpreter(bootstrapOptions(new RIOMonadError[Any]), SttpBackendStub(new RIOMonadError[Any]))
        .whenServerEndpoint(schemaRegistryEndpoints.createSchemaServerEndpoint)
        .thenRunLogic()
        .whenServerEndpoint(schemaRegistryEndpoints.getSchemaByIdServerEndpoint)
        .thenRunLogic()
        .whenServerEndpoint(schemaRegistryEndpoints.lookupSchemasByQueryServerEndpoint)
        .thenRunLogic()
        .backend()
    backend
  }

  def generateSchemasN(count: Int): ZIO[SchemaRegistryService, Throwable, List[VerifiableCredentialSchemaInput]] =
    for {
      schemaRegistryService <- ZIO.service[SchemaRegistryService]
      backend = httpBackend(schemaRegistryService)
      inputs <- Generators.schemaInput.runCollectN(count)
      _ <- inputs
        .map(in =>
          basicRequest
            .post(schemaReqistrySchemasUri)
            .body(in.toJsonPretty)
            .response(asJsonAlways[VerifiableCredentialSchema])
            .send(backend)
        )
        .reduce((l, r) => l.flatMap(_ => r))
    } yield inputs

  def fetchAllPages(uri: Uri): ZIO[SchemaRegistryService, Throwable, List[VerifiableCredentialSchemaPage]] = {
    for {
      schemaRegistryService <- ZIO.service[SchemaRegistryService]
      backend = httpBackend(schemaRegistryService)
      response: SchemaPageResponse <- basicRequest
        .get(uri)
        .response(asJsonAlways[VerifiableCredentialSchemaPage])
        .send(backend)
      firstPage <- ZIO.fromEither(response.body)
      otherPagesStream = zio.stream.ZStream
        .unfoldZIO[Any, Throwable, VerifiableCredentialSchemaPage, VerifiableCredentialSchemaPage](firstPage)(page =>
          page.next
            .map(n => uri"$n")
            .fold(
              ZIO.succeed(Option.empty[(VerifiableCredentialSchemaPage, VerifiableCredentialSchemaPage)])
            )(nextPageUri =>
              for {
                nextPageResponse: SchemaPageResponse <-
                  basicRequest
                    .get(nextPageUri)
                    .response(asJsonAlways[VerifiableCredentialSchemaPage])
                    .send(backend)
                nextPage <- ZIO.fromEither(nextPageResponse.body)
              } yield Some((nextPage, nextPage))
            )
        )
      otherPages <- otherPagesStream.runCollect
        .fold(_ => List.empty[VerifiableCredentialSchemaPage], success => success.toList)

    } yield List(firstPage) ++ otherPages
  }

  def spec = suite("schema-registy endpoints spec")(
    schemaCreateAndGetOperationsSpec,
    schemaPaginationSpec,
    schemaBadRequestAsJsonSpec
  ).provideLayer(SchemaRegistryServiceInMemory.layer)

  private val schemaCreateAndGetOperationsSpec = suite("schema-registy create and get by ID operations logic")(
    test("create the new schema") {
      for {
        schemaRegistryService <- ZIO.service[SchemaRegistryService]
        backend = httpBackend(schemaRegistryService)

        response = basicRequest
          .post(schemaReqistrySchemasUri)
          .body(schemaInput.toJsonPretty)
          .response(asJsonAlways[VerifiableCredentialSchema])
          .send(backend)

        assertion <- assertZIO(response.map(_.body))(isRight(equalTo(schema)))
      } yield assertion
    },
    test("create and get the schema by id") {
      for {
        schemaRegistryService <- ZIO.service[SchemaRegistryService]
        backend = httpBackend(schemaRegistryService)

        _ <- basicRequest
          .post(schemaReqistrySchemasUri)
          .body(schemaInput.toJsonPretty)
          .send(backend)

        response = basicRequest
          .get(schemaReqistrySchemasUri.addPath(schemaId.toString))
          .response(asJsonAlways[VerifiableCredentialSchema])
          .send(backend)

        assertion <- assertZIO(response.map(_.body))(isRight(equalTo(schema)))
      } yield assertion
    },
    test("create and get the schema by the wrong id") {
      for {
        schemaRegistryService <- ZIO.service[SchemaRegistryService]
        backend = httpBackend(schemaRegistryService)

        _ <- basicRequest
          .post(schemaReqistrySchemasUri)
          .body(schemaInput.toJsonPretty)
          .send(backend)

        uuid = UUID.randomUUID()

        response = basicRequest
          .get(schemaReqistrySchemasUri.addPath(uuid.toString))
          .response(asJsonAlways[NotFound])
          .send(backend)

        assertion <- assertZIO(response.map(_.code))(equalTo(StatusCode.NotFound))
      } yield assertion
    }
  )

  private val schemaPaginationSpec = suite("schema-registry pagination logic")(
    test("pagination of the first page with the empty query params") {
      for {
        schemaRegistryService <- ZIO.service[SchemaRegistryService]
        backend = httpBackend(schemaRegistryService)

        inputs <- Generators.schemaInput.runCollectN(101)
        _ <- inputs
          .map(in =>
            basicRequest
              .post(schemaReqistrySchemasUri)
              .body(in.toJsonPretty)
              .response(asJsonAlways[VerifiableCredentialSchema])
              .send(backend)
          )
          .reduce((l, r) => l.flatMap(_ => r))

        response: SchemaPageResponse <- basicRequest
          .get(schemaReqistrySchemasUri)
          .response(asJsonAlways[VerifiableCredentialSchemaPage])
          .send(backend)

        schemaPage <- ZIO.fromEither(response.body)

        itIsSuccessfulResponse = assert(response.code)(equalTo(StatusCode.Ok))
        itReturnedDefaultLimitOfItemsInOnePage = assert(schemaPage.contents.length)(equalTo(100))
        nextPage_isNonEmpty = assertTrue(schemaPage.next.nonEmpty)
        previousPage_isEmpty = assertTrue(schemaPage.previous.isEmpty)
        self_equalsTo_theValidUri = assert(schemaPage.self)(equalTo(schemaReqistrySchemasUri.toString))
        pageOf_equalTo_theValidUri = assert(schemaPage.pageOf)(equalTo(schemaReqistrySchemasUri.toString))

      } yield itIsSuccessfulResponse &&
        itReturnedDefaultLimitOfItemsInOnePage &&
        nextPage_isNonEmpty &&
        previousPage_isEmpty &&
        self_equalsTo_theValidUri &&
        pageOf_equalTo_theValidUri
    },
    test("pagination of navigation over the pages") {
      for {
        inputs <- generateSchemasN(100)

        allPagesWithLimit1 <- fetchAllPages(uri"$schemaReqistrySchemasUri?offset=0&limit=1")
        allPagesWithLimit10 <- fetchAllPages(uri"$schemaReqistrySchemasUri?offset=0&limit=10")
        allPagesWithLimit15 <- fetchAllPages(uri"$schemaReqistrySchemasUri?offset=0&limit=15")
      } yield assert(inputs.length)(equalTo(100)) &&
        assert(allPagesWithLimit1.length)(equalTo(100)) &&
        assert(allPagesWithLimit10.length)(equalTo(10)) &&
        assert(allPagesWithLimit15.length)(equalTo(7))
    }
  )

  private val schemaBadRequestAsJsonSpec = suite("schema-registry BadRequest as json logic")(
    test("create the schema with wrong json body returns BadRequest as json") {
      for {
        schemaRegistryService <- ZIO.service[SchemaRegistryService]
        backend = httpBackend(schemaRegistryService)
        response: SchemaBadRequestResponse <- basicRequest
          .post(schemaReqistrySchemasUri)
          .body("""{"invalid":true}""")
          .response(asJsonAlways[BadRequest])
          .send(backend)

        itIsABadRequestStatusCode = assert(response.code)(equalTo(StatusCode.BadRequest))
        theBodyWasParsedFromJsonAsBadRequest = assert(response.body)(isRight(isSubtype[BadRequest](Assertion.anything)))
      } yield itIsABadRequestStatusCode && theBodyWasParsedFromJsonAsBadRequest
    }
  )
