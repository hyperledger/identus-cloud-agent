package io.iohk.atala.agent.walletapi.util

import io.iohk.atala.agent.walletapi.model.ManagedDIDTemplate
import zio.*
import zio.test.*
import zio.test.Assertion.*

object ManagedDIDTemplateValidatorSpec extends ZIOSpecDefault {

  override def spec = suite("ManagedDIDTemplateValidator")(
    test("accept empty DID template") {
      val template = ManagedDIDTemplate(publicKeys = Nil, services = Nil)
      assert(ManagedDIDTemplateValidator.validate(template))(isRight)
    }
  )

}
