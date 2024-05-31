package org.hyperledger.identus.agent.walletapi.util

import org.hyperledger.identus.agent.walletapi.model.{DIDPublicKeyTemplate, ManagedDIDTemplate}
import org.hyperledger.identus.agent.walletapi.service.ManagedDIDService
import org.hyperledger.identus.castor.core.model.did.{
  EllipticCurve,
  Service,
  ServiceEndpoint,
  ServiceType,
  VerificationRelationship
}
import org.hyperledger.identus.castor.core.model.did.ServiceEndpoint.{UriOrJsonEndpoint, UriValue}
import zio.*
import zio.test.*
import zio.test.Assertion.*

import scala.language.implicitConversions

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
            purpose = VerificationRelationship.Authentication,
            curve = EllipticCurve.SECP256K1
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
            purpose = VerificationRelationship.Authentication,
            curve = EllipticCurve.SECP256K1
          )
        ),
        services = Nil,
        contexts = Nil
      )
      assert(ManagedDIDTemplateValidator.validate(template))(isLeft)
    },
    test("reject DID template if key usage is not allowed") {
      val makeTemplate = (keyTemplate: DIDPublicKeyTemplate) =>
        ManagedDIDTemplate(
          publicKeys = Seq(keyTemplate),
          services = Nil,
          contexts = Nil
        )
      val template1 = makeTemplate(
        DIDPublicKeyTemplate(
          id = "key-1",
          purpose = VerificationRelationship.KeyAgreement,
          curve = EllipticCurve.ED25519
        )
      )
      val template2 = makeTemplate(
        DIDPublicKeyTemplate(
          id = "key-1",
          purpose = VerificationRelationship.Authentication,
          curve = EllipticCurve.X25519
        )
      )
      assert(ManagedDIDTemplateValidator.validate(template1))(isLeft) &&
      assert(ManagedDIDTemplateValidator.validate(template2))(isLeft)
    }
  )

}
