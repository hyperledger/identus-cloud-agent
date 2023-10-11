package io.iohk.atala.pollux.schema

import com.dimafeng.testcontainers.PostgreSQLContainer
import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.container.util.MigrationAspects.migrate
import io.iohk.atala.iam.authentication.AuthenticatorAuthorizer
import io.iohk.atala.pollux.credentialschema.*
import io.iohk.atala.pollux.credentialschema.controller.CredentialSchemaController
import io.iohk.atala.pollux.credentialschema.http.{
  CredentialSchemaInput,
  CredentialSchemaResponse,
  CredentialSchemaResponsePage
}
import io.iohk.atala.shared.models.{WalletAccessContext, WalletId}
import sttp.client3.ziojson.*
import sttp.client3.{DeserializationException, Response, UriContext, basicRequest}
import sttp.model.{StatusCode, Uri}
import zio.*
import zio.json.EncoderOps
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*

object CredentialSchemaLookupAndPaginationSpec
    extends ZIOSpecDefault
    with CredentialSchemaTestTools
    with CredentialSchemaGen:

  def fetchAllPages(
      uri: Uri
  ): ZIO[CredentialSchemaController & AuthenticatorAuthorizer[BaseEntity], Throwable, List[
    CredentialSchemaResponsePage
  ]] = {
    for {
      controller <- ZIO.service[CredentialSchemaController]
      authenticator <- ZIO.service[AuthenticatorAuthorizer[BaseEntity]]
      backend = httpBackend(controller, authenticator)
      response: SchemaPageResponse <- basicRequest
        .get(uri)
        .response(asJsonAlways[CredentialSchemaResponsePage])
        .send(backend)
      firstPage <- ZIO.fromEither(response.body)
      otherPagesStream = zio.stream.ZStream
        .unfoldZIO[Any, Throwable, CredentialSchemaResponsePage, CredentialSchemaResponsePage](firstPage)(page =>
          page.next
            .map(n => uri"$n")
            .fold(
              ZIO.succeed(Option.empty[(CredentialSchemaResponsePage, CredentialSchemaResponsePage)])
            )(nextPageUri =>
              for {
                nextPageResponse: SchemaPageResponse <-
                  basicRequest
                    .get(nextPageUri)
                    .response(asJsonAlways[CredentialSchemaResponsePage])
                    .send(backend)
                nextPage <- ZIO.fromEither(nextPageResponse.body)
              } yield Some((nextPage, nextPage))
            )
        )
      otherPages <- otherPagesStream.runCollect
        .fold(_ => List.empty[CredentialSchemaResponsePage], success => success.toList)

    } yield List(firstPage) ++ otherPages
  }

  def spec = (
    schemaPaginationSpec @@
      nondeterministic @@ sequential @@ timed @@ migrate(
        schema = "public",
        paths = "classpath:sql/pollux"
      )
  ).provideSomeLayerShared(mockManagedDIDServiceLayer.exactly(201).toLayer >+> testEnvironmentLayer)

  private val schemaPaginationSpec = suite("schema-registry pagination logic")(
    test("pagination of the first page with the empty query params") {
      for {
        _ <- deleteAllCredentialSchemas
        controller <- ZIO.service[CredentialSchemaController]
        authenticator <- ZIO.service[AuthenticatorAuthorizer[BaseEntity]]
        backend = httpBackend(controller, authenticator)

        inputs <- Generator.schemaInput.runCollectN(101)
        _ <- inputs
          .map(in =>
            basicRequest
              .post(credentialSchemaUriBase)
              .body(in.toJsonPretty)
              .response(asJsonAlways[CredentialSchemaResponse])
              .send(backend)
          )
          .reduce((l, r) => l.flatMap(_ => r))

        response: SchemaPageResponse <- basicRequest
          .get(credentialSchemaUriBase)
          .response(asJsonAlways[CredentialSchemaResponsePage])
          .send(backend)

        schemaPage <- ZIO.fromEither(response.body)

        itIsSuccessfulResponse = assert(response.code)(equalTo(StatusCode.Ok))
        itReturnedDefaultLimitOfItemsInOnePage = assert(schemaPage.contents.length)(equalTo(100))
        nextPage_isNonEmpty = assertTrue(schemaPage.next.nonEmpty)
        previousPage_isEmpty = assertTrue(schemaPage.previous.isEmpty)
        self_equalsTo_theValidUri = assert(schemaPage.self)(equalTo(credentialSchemaUriBase.toString))
        pageOf_equalTo_theValidUri = assert(schemaPage.pageOf)(equalTo(credentialSchemaUriBase.toString))

      } yield itIsSuccessfulResponse &&
        itReturnedDefaultLimitOfItemsInOnePage &&
        nextPage_isNonEmpty &&
        previousPage_isEmpty &&
        self_equalsTo_theValidUri &&
        pageOf_equalTo_theValidUri
    },
    test("pagination and navigation over the pages") {
      for {
        _ <- deleteAllCredentialSchemas
        inputs <- generateSchemasN(100)

        allPagesWithLimit1 <- fetchAllPages(uri"$credentialSchemaUriBase?offset=0&limit=1")
        allPagesWithLimit10 <- fetchAllPages(uri"$credentialSchemaUriBase?offset=0&limit=10")
        allPagesWithLimit15 <- fetchAllPages(uri"$credentialSchemaUriBase?offset=0&limit=15")
      } yield assert(inputs.length)(equalTo(100)) &&
        assert(allPagesWithLimit1.length)(equalTo(100)) &&
        assert(allPagesWithLimit10.length)(equalTo(10)) &&
        assert(allPagesWithLimit15.length)(equalTo(7))
    }
  ).provideSomeLayer(ZLayer.succeed(WalletAccessContext(WalletId.default)))
