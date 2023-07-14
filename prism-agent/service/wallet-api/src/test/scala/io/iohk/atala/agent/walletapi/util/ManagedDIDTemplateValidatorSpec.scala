package io.iohk.atala.agent.walletapi.util

import io.iohk.atala.agent.walletapi.model.{DIDPublicKeyTemplate, ManagedDIDTemplate}
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.castor.core.model.did.ServiceEndpoint
import io.iohk.atala.castor.core.model.did.ServiceEndpoint.UriOrJsonEndpoint
import io.iohk.atala.castor.core.model.did.ServiceEndpoint.UriValue
import io.iohk.atala.castor.core.model.did.{Service, ServiceType, VerificationRelationship}
import scala.language.implicitConversions
import zio.*
import zio.test.*
import zio.test.Assertion.*

object ManagedDIDTemplateValidatorSpec extends ZIOSpecDefault {

  override def spec = suite("ManagedDIDTemplateValidator")(
    test("accept empty DID template") {
      val template = ManagedDIDTemplate(publicKeys = Nil, services = Nil, contexts = Nil)
      assert(ManagedDIDTemplateValidator.validate(template))(isRight)
    },
    test("accept valid non-empty DID template") {
      val template = ManagedDIDTemplate(
        publicKeys = Seq(
          DIDPublicKeyTemplate(
            id = "auth0",
            purpose = VerificationRelationship.Authentication
          )
        ),
        services = Seq(
          Service(
            id = "service0",
            `type` = ServiceType.Single(ServiceType.Name.fromStringUnsafe("LinkedDomains")),
            serviceEndpoint = ServiceEndpoint.Single(UriValue.fromString("http://example.com/").toOption.get)
          )
        ),
        contexts = Nil
      )
      assert(ManagedDIDTemplateValidator.validate(template))(isRight)
    },
    test("reject DID template if contain reserved key-id") {
      val template = ManagedDIDTemplate(
        publicKeys = Seq(
          DIDPublicKeyTemplate(
            id = ManagedDIDService.DEFAULT_MASTER_KEY_ID,
            purpose = VerificationRelationship.Authentication
          )
        ),
        services = Nil,
        contexts = Nil
      )
      assert(ManagedDIDTemplateValidator.validate(template))(isLeft)
    }
  )

}
