package io.iohk.atala.agent.walletapi.util

import zio.*
import zio.test.*
import zio.test.Assertion.*
import io.iohk.atala.agent.walletapi.crypto.ApolloSpecHelper
import io.iohk.atala.agent.walletapi.crypto.Prism14Apollo
import io.iohk.atala.agent.walletapi.model.DIDPublicKeyTemplate
import io.iohk.atala.agent.walletapi.model.ManagedDIDTemplate
import io.iohk.atala.castor.core.model.did.EllipticCurve
import io.iohk.atala.castor.core.model.did.VerificationRelationship
import io.iohk.atala.shared.models.{HexString, Base64UrlString}
import org.didcommx.didcomm.diddoc.VerificationMethod

object OperationFactorySpec extends ZIOSpecDefault, ApolloSpecHelper {

  override def spec = suite("OperationFactory")(
    test("dummy") {
      val seed = HexString
        .fromStringUnsafe(
          "511ed85f7b162caa4ea6a7448bcf3018ce06dd4fbf63665e21a35eb02c8cdb8631007432be2809cecd871d56d44532faf5cfc81bac4936cf0d937fdea6aedfad"
        )
        .toByteArray
      val factory = OperationFactory(apollo)
      val template = ManagedDIDTemplate(
        Seq(
          DIDPublicKeyTemplate(id = "auth0", purpose = VerificationRelationship.Authentication),
          DIDPublicKeyTemplate(id = "iss0", purpose = VerificationRelationship.AssertionMethod),
          DIDPublicKeyTemplate(id = "iss1", purpose = VerificationRelationship.AssertionMethod),
        ),
        Nil
      )
      for {
        operationWithHdKey <- factory.makeCreateOperationHdKey("master0", seed)(0, template)
      } yield assertCompletes
    } @@ TestAspect.tag("dev")
  )

}
