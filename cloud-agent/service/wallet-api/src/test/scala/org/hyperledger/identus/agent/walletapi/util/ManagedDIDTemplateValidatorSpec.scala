package org.hyperledger.identus.agent.walletapi.util

import org.hyperledger.identus.agent.walletapi.model.{DIDPublicKeyTemplate, ManagedDIDTemplate}
import org.hyperledger.identus.agent.walletapi.service.ManagedDIDService
import org.hyperledger.identus.castor.core.model.did.EllipticCurve
import org.hyperledger.identus.castor.core.model.did.ServiceEndpoint
import org.hyperledger.identus.castor.core.model.did.ServiceEndpoint.UriOrJsonEndpoint
import org.hyperledger.identus.castor.core.model.did.ServiceEndpoint.UriValue
import org.hyperledger.identus.castor.core.model.did.{Service, ServiceType, VerificationRelationship}
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
    }
  )

}
