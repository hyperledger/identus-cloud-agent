package io.iohk.atala.castor.core.model.did

import io.iohk.atala.shared.models.HexStrings.HexString
import zio.*
import zio.test.*
import zio.test.Assertion.*

object PrismDIDSpec extends ZIOSpecDefault {

  override def spec = suite("PrismDID")(prismDIDV1Suite, longFormPrismDIDV1Suite) @@ TestAspect.samples(20)

  private val createOpDummy = PublishedDIDOperation.Create(
    updateCommitment = HexString.fromStringUnsafe("00"),
    recoveryCommitment = HexString.fromStringUnsafe("00"),
    storage = DIDStorage.Cardano("testnet"),
    document = DIDDocument(
      publicKeys = Nil,
      services = Nil
    )
  )

  private val prismDIDV1Suite =
    suite("PrismDIDV1")(
      test("render correct DID syntax") {
        val prismDID = PrismDIDV1.fromCreateOperation(createOpDummy)
        assert(prismDID.toString)(
          equalTo("did:prism:1:testnet:3511ffa95ef4f0d9b289329dd2a2276c4ecd9034ab2d79ec72abaef950bfe553")
        )
      },
      test("DID syntax is the same when converted to standard DID") {
        val ledgerNameGen = Gen.stringN(8)(Gen.asciiChar)
        check(ledgerNameGen) { ledgerName =>
          val op = createOpDummy.copy(storage = DIDStorage.Cardano(ledgerName))
          val prismDID = PrismDIDV1.fromCreateOperation(op)
          assert(prismDID.toString)(equalTo(prismDID.did.toString))
        }
      }
    )

  private val longFormPrismDIDV1Suite =
    suite("LongFormPrismDIDV1")(
      test("render correct DID syntax") {
        val longFormPrismDID = LongFormPrismDIDV1.fromCreateOperation(createOpDummy)
        assert(longFormPrismDID.toString)(
          equalTo(
            "did:prism:1:testnet:3511ffa95ef4f0d9b289329dd2a2276c4ecd9034ab2d79ec72abaef950bfe553:CgEAEgEAGgd0ZXN0bmV0IgA="
          )
        )
      },
      test("convert to canonical PrismDIDV1") {
        val longFormPrismDID = LongFormPrismDIDV1.fromCreateOperation(createOpDummy)
        val prismDID = longFormPrismDID.toCanonical
        assert(prismDID.toString)(
          equalTo("did:prism:1:testnet:3511ffa95ef4f0d9b289329dd2a2276c4ecd9034ab2d79ec72abaef950bfe553")
        )
      },
      test("DID syntax is the same when converted to standard DID") {
        val ledgerNameGen = Gen.stringN(8)(Gen.asciiChar)
        check(ledgerNameGen) { ledgerName =>
          val op = createOpDummy.copy(storage = DIDStorage.Cardano(ledgerName))
          val longFormPrismDID = LongFormPrismDIDV1.fromCreateOperation(op)
          assert(longFormPrismDID.toString)(equalTo(longFormPrismDID.did.toString))
        }
      }
    )

}
