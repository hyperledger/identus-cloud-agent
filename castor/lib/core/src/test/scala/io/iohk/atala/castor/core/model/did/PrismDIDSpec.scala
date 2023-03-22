package io.iohk.atala.castor.core.model.did

import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.{Sha256, Sha256Digest}
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.shared.models.HexStrings.*
import zio.*
import zio.test.*
import zio.test.Assertion.*
import io.iohk.atala.castor.core.util.DIDOperationValidator

object PrismDIDSpec extends ZIOSpecDefault {

  private val canonicalSuffixHex = "9b5118411248d9663b6ab15128fba8106511230ff654e7514cdcc4ce919bde9b"
  private val canonicalSuffix = Sha256Digest.fromHex(canonicalSuffixHex)
  private val encodedStateUsedBase64 =
    "Cj8KPRI7CgdtYXN0ZXIwEAFKLgoJc2VjcDI1NmsxEiEDHpf-yhIns-LP3tLvA8icC5FJ1ZlBwbllPtIdNZ3q0jU"

  private val short = PrismDID.buildCanonical(canonicalSuffix.getValue).toOption.get
  private val long = PrismDID
    .buildLongFormFromAtalaOperation(
      node_models.AtalaOperation.parseFrom(Base64UrlString.fromStringUnsafe(encodedStateUsedBase64).toByteArray)
    )
    .toOption
    .get
  override def spec = suite("PrismDID")(didParserSpec)

  private val didParserSpec = suite("PrismDID.fromString")(
    test("dummy") {
      val result = for {
        did <- PrismDID.fromString("did:prism:adbe9289d0c5fb56ffa50387d1804ab4a1576527c946a5252817fa8e8a2cced0:CssBCsgBEmIKDW1hc3RlcihpbmRleCkQAUJPCglTZWNwMjU2azESIE-LiGL3QHdUnNkpYbJhsTkmXPXISqVz76c1U9xhP9rpGiDqV59sEzJVRCjR4kM0gHxluP6eMJItw267fXVRa0xC5hJiCg1tYXN0ZXIoaW5kZXgpEARCTwoJU2VjcDI1NmsxEiBPi4hi90B3VJzZKWGyYbE5Jlz1yEqlc--nNVPcYT_a6Rog6lefbBMyVUQo0eJDNIB8Zbj-njCSLcNuu311UWtMQuY")
        validator = DIDOperationValidator(DIDOperationValidator.Config.default)
        _ <- validator.validate(did.asInstanceOf[LongFormPrismDID].createOperation)
      } yield ()
      assert(result)(isRight)
    } @@ TestAspect.tag("dev"),
    test("success for valid DID") {
      val stateHash = Sha256.compute(Array()).getValue
      val validDID = PrismDID.buildCanonical(stateHash).toOption.get
      val unsafeDID = PrismDID.fromString(validDID.toString)
      assert(unsafeDID)(isRight(equalTo(validDID)))
    },
    test("success for long form string") {
      val longAsString = long.toString
      val unsafeDID = PrismDID.fromString(longAsString)
      assert(unsafeDID)(isRight(equalTo(long)))
    },
    test("success for canonical form string") {
      val canonicalAsString = short.toString
      val unsafeDID = PrismDID.fromString(canonicalAsString)
      assert(unsafeDID)(isRight(equalTo(short)))
    },
    test("fail for invalid DID") {
      val unsafeDID = PrismDID.fromString("invalid-did")
      assert(unsafeDID)(isLeft)
    },
    test("fail for long form initial state is not CreateDID") {
      val mockAtalaOperation = node_models.AtalaOperation(
        node_models.AtalaOperation.Operation.UpdateDid(
          node_models.UpdateDIDOperation(
            previousOperationHash = ByteString.EMPTY,
            id = "update0",
            actions = Nil
          )
        )
      )
      val encodedState = mockAtalaOperation.toByteArray
      val encodedStateBase64 = Base64UrlString.fromByteArray(encodedState).toStringNoPadding
      val stateHash = Sha256.compute(encodedState).getHexValue
      val didString = s"did:prism:$stateHash:$encodedStateBase64"
      val unsafeDID = PrismDID.fromString(didString)
      assert(unsafeDID)(isLeft(containsString("CreateDid Atala operation expected")))
    }
  )

}
