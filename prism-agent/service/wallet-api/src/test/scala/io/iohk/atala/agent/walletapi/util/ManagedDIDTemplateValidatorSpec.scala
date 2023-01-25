package io.iohk.atala.agent.walletapi.util

import io.iohk.atala.castor.core.model.did.{Service, ServiceType, VerificationRelationship}
import io.iohk.atala.agent.walletapi.model.{DIDPublicKeyTemplate, ManagedDIDTemplate}
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import zio.*
import zio.test.*
import zio.test.Assertion.*

import java.net.URI

object ManagedDIDTemplateValidatorSpec extends ZIOSpecDefault {

  override def spec = suite("ManagedDIDTemplateValidator")(
    test("accept empty DID template") {
      val template = ManagedDIDTemplate(publicKeys = Nil, services = Nil)
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
            `type` = ServiceType.MediatorService,
            serviceEndpoint = Seq(URI.create("http://example.com"))
          )
        )
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
        services = Nil
      )
      assert(ManagedDIDTemplateValidator.validate(template))(isLeft)
    }
  )

}
