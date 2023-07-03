package io.iohk.atala.castor.core.model.did

import zio.*
import zio.test.*
import zio.test.Assertion.*

object DIDSpec extends ZIOSpecDefault {

  override def spec = suite("DID")(fromStringSpec)

  // TODO: check this https://github.com/w3c/did-test-suite
  private val fromStringSpec = suite("DID.fromString")(
    test("parse valid PRISM did") {
      // val did = "did:prism:c191d4dfe2806d59df4632f78d38f80dfbfbd88187804783717e29116ffb"
      // did:prism:c191d4dfe2806d59df4632f78d38f80dfbfbd88187804783717e29116ffb:Cu0CCuoCEjoKBmF1dGgtMRAESi4KCXNlY3AyNTZrMRIhAz3656yBEIzdpZuG3yYn8Npoty3_qQhIbpOC8QmdVnpeEj8KC2Fzc2VydGlvbi0xEAJKLgoJc2VjcDI1NmsxEiECbZrA1SxPTpIEl9VBY4a6hGqaPJbJE7v8U5b236bCbWUSOwoHbWFzdGVyMBABSi4KCXNlY3AyNTZrMRIhA0BNFWJYhLqaVji9EYfIvdi
      val did =
        "did:prism:c191d4dfe2806d59df4632f78d38f80dfbfbd88187804783717e29116ffb:Cu0CCuoCEjoKBmF1dGgtMRAESi4KCXNlY3AyNTZrMRIhAz3656yBEIzdpZuG3yYn8Npoty3_qQhIbpOC8QmdVnpeEj8KC2Fzc2VydGlvbi0xEAJKLgoJc2VjcDI1NmsxEiECbZrA1SxPTpIEl9VBY4a6hGqaPJbJE7v8U5b236bCbWUSOwoHbWFzdGVyMBABSi4KCXNlY3AyNTZrMRIhA0BNFWJYhLqaVji9EYfIvdi"
      val parsed = DID.fromString(did)
      println(parsed)
      println(parsed.map(_.method))
      println(parsed.map(_.methodSpecificId))
      assert(parsed)(isRight(anything))
    }
  ) @@ TestAspect.tag("dev")

}
