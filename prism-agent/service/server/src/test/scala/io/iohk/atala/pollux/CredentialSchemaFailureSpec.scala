package io.iohk.atala.pollux

import io.iohk.atala.agent.walletapi.service.MockManagedDIDService
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.container.util.MigrationAspects.migrate
import io.iohk.atala.pollux.credentialschema.*
import io.iohk.atala.pollux.credentialschema.controller.CredentialSchemaController
import sttp.client3.ziojson.*
import sttp.client3.{DeserializationException, basicRequest}
import sttp.model.StatusCode
import zio.*
import zio.test.*
import zio.test.Assertion.*

object CredentialSchemaFailureSpec extends ZIOSpecDefault with CredentialSchemaTestTools:
  def spec = (schemaBadRequestAsJsonSpec @@ migrate(
    schema = "public",
    paths = "classpath:sql/pollux"
  )).provideSomeLayerShared(MockManagedDIDService.empty >+> testEnvironmentLayer)

  private val schemaBadRequestAsJsonSpec = suite("schema-registry BadRequest as json logic")(
    test("create the schema with wrong json body returns BadRequest as json") {
      for {
        schemaRegistryService <- ZIO.service[CredentialSchemaController]
        backend = httpBackend(schemaRegistryService)
        response: SchemaBadRequestResponse <- basicRequest
          .post(credentialSchemaUriBase)
          .body("""{"foo":"bar"}""")
          .response(asJsonAlways[ErrorResponse])
          .send(backend)

        itIsABadRequestStatusCode = assert(response.code)(equalTo(StatusCode.BadRequest))
        theBodyWasParsedFromJsonAsBadRequest = assert(response.body)(
          isRight(isSubtype[ErrorResponse](Assertion.anything))
        )
      } yield itIsABadRequestStatusCode && theBodyWasParsedFromJsonAsBadRequest
    }
  )
