package io.iohk.atala.pollux.credentialdefinition

import io.iohk.atala.agent.walletapi.service.MockManagedDIDService
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.container.util.MigrationAspects.migrate
import io.iohk.atala.pollux.credentialdefinition.controller.CredentialDefinitionController
import sttp.client3.DeserializationException
import sttp.client3.basicRequest
import sttp.client3.ziojson.*
import sttp.model.StatusCode
import zio.*
import zio.ZIO.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*

object CredentialDefinitionFailureSpec extends ZIOSpecDefault with CredentialDefinitionTestTools:
  def spec = (suite("credential-definition-registry bad request spec")(
    credentialDefinitionBadRequestAsJsonSpec
  ) @@ nondeterministic @@ sequential @@ timed @@ migrate(
    schema = "public",
    paths = "classpath:sql/pollux"
  )).provideSomeLayerShared(MockManagedDIDService.empty >+> testEnvironmentLayer)

  private val credentialDefinitionBadRequestAsJsonSpec =
    test("create the credential definition with wrong json body returns BadRequest as json") {
      for {
        credentialDefinitionRegistryService <- ZIO.service[CredentialDefinitionController]
        backend = httpBackend(credentialDefinitionRegistryService)
        response: CredentialDefinitionBadRequestResponse <- basicRequest
          .post(credentialDefinitionUriBase)
          .body("""{"foo":"bar"}""")
          .response(asJsonAlways[ErrorResponse])
          .send(backend)

        itIsABadRequestStatusCode = assert(response.code)(equalTo(StatusCode.BadRequest))
        theBodyWasParsedFromJsonAsBadRequest = assert(response.body)(
          isRight(isSubtype[ErrorResponse](Assertion.anything))
        )
      } yield itIsABadRequestStatusCode // && theBodyWasParsedFromJsonAsBadRequest
    }
