package io.iohk.atala.agent.walletapi.util

import zio.*
import zio.test.*
import zio.test.Assertion.*
import io.iohk.atala.agent.walletapi.crypto.ApolloSpecHelper
import io.iohk.atala.shared.models.HexString
import io.iohk.atala.agent.walletapi.model.ManagedDIDTemplate
import io.iohk.atala.castor.core.model.did.InternalPublicKey
import io.iohk.atala.castor.core.model.did.InternalKeyPurpose
import io.iohk.atala.agent.walletapi.model.DIDPublicKeyTemplate
import io.iohk.atala.castor.core.model.did.VerificationRelationship
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.agent.walletapi.model.HdKeyIndexCounter
import io.iohk.atala.agent.walletapi.model.UpdateManagedDIDAction
import io.iohk.atala.castor.core.model.did.PrismDIDOperation
import io.iohk.atala.agent.walletapi.model.VerificationRelationshipCounter
import io.iohk.atala.castor.core.model.did.UpdateDIDAction

object OperationFactorySpec extends ZIOSpecDefault, ApolloSpecHelper {

  private val didExample = PrismDID.buildLongFormFromOperation(PrismDIDOperation.Create(Nil, Nil, Nil)).asCanonical

  private val previousOperationHash = didExample.stateHash.toByteArray

  private val seed = HexString
    .fromStringUnsafe(
      "fffcf9f6f3f0edeae7e4e1dedbd8d5d2cfccc9c6c3c0bdbab7b4b1aeaba8a5a29f9c999693908d8a8784817e7b7875726f6c696663605d5a5754514e4b484542"
    )
    .toByteArray

  private val operationFactory = OperationFactory(apollo)

  override def spec =
    suite("OperationFactory")(makeCreateOpeartionHdKeySpec, makeUpdateOperationHdKeySpec)

  private val makeCreateOpeartionHdKeySpec = suite("makeCreateOpeartionHdKeySpec ")(
    test("make CrateOperation from same seed is deterministic") {
      val didTemplate = ManagedDIDTemplate(Nil, Nil)
      for {
        result1 <- operationFactory.makeCreateOperationHdKey("master0", seed)(0, didTemplate)
        (op1, hdKey1) = result1
        result2 <- operationFactory.makeCreateOperationHdKey("master0", seed)(0, didTemplate)
        (op2, hdKey2) = result2
      } yield assert(op1)(equalTo(op2)) &&
        assert(hdKey1)(equalTo(hdKey2))
    },
    test("make CreateOperation must contain 1 master key") {
      val didTemplate = ManagedDIDTemplate(Nil, Nil)
      for {
        result <- operationFactory.makeCreateOperationHdKey("master-0", seed)(0, didTemplate)
        (op, hdKey) = result
        pk = op.publicKeys.head.asInstanceOf[InternalPublicKey]
      } yield assert(op.publicKeys)(hasSize(equalTo(1))) &&
        assert(pk.id)(equalTo("master-0")) &&
        assert(pk.purpose)(equalTo(InternalKeyPurpose.Master))
    },
    test("make CreateOperation containing multiple keys") {
      val didTemplate = ManagedDIDTemplate(
        Seq(
          DIDPublicKeyTemplate("auth-0", VerificationRelationship.Authentication),
          DIDPublicKeyTemplate("auth-1", VerificationRelationship.Authentication),
          DIDPublicKeyTemplate("issue-0", VerificationRelationship.AssertionMethod),
        ),
        Nil
      )
      for {
        result <- operationFactory.makeCreateOperationHdKey("master-0", seed)(0, didTemplate)
        (op, hdKey) = result
      } yield assert(op.publicKeys.length)(equalTo(4)) &&
        assert(hdKey.internalKeyPaths.size)(equalTo(1)) &&
        assert(hdKey.keyPaths.size)(equalTo(3)) &&
        assert(hdKey.internalKeyPaths.get("master-0").get.keyIndex)(equalTo(0)) &&
        assert(hdKey.keyPaths.get("auth-0").get.keyIndex)(equalTo(0)) &&
        assert(hdKey.keyPaths.get("auth-1").get.keyIndex)(equalTo(1)) &&
        assert(hdKey.keyPaths.get("issue-0").get.keyIndex)(equalTo(0))
    }
  )

  private val makeUpdateOperationHdKeySpec = suite("makeUpdateOperationHdKeySpec ")(
    test("make UpdateOperation from same seed is deterministic") {
      val counter = HdKeyIndexCounter.zero(0)
      val actions =
        Seq(UpdateManagedDIDAction.AddKey(DIDPublicKeyTemplate("issue-42", VerificationRelationship.AssertionMethod)))
      for {
        result1 <- operationFactory.makeUpdateOperationHdKey(seed)(didExample, previousOperationHash, actions, counter)
        (op1, hdKey1) = result1
        result2 <- operationFactory.makeUpdateOperationHdKey(seed)(didExample, previousOperationHash, actions, counter)
        (op2, hdKey2) = result2
      } yield assert(op1)(equalTo(op2)) && assert(hdKey1)(equalTo(hdKey2))
    },
    test("make UpdateOperation correctly construct operation and increment counter") {
      val counter = HdKeyIndexCounter
        .zero(42)
        .copy(
          verificationRelationship = VerificationRelationshipCounter.zero.copy(
            authentication = 3,
            assertionMethod = 1,
          )
        )
      val actions = Seq(
        UpdateManagedDIDAction.AddKey(DIDPublicKeyTemplate("auth-42", VerificationRelationship.Authentication)),
        UpdateManagedDIDAction.AddKey(DIDPublicKeyTemplate("issue-42", VerificationRelationship.AssertionMethod)),
      )
      for {
        result <- operationFactory.makeUpdateOperationHdKey(seed)(didExample, previousOperationHash, actions, counter)
        (op, hdKey) = result
      } yield {
        // counter is correct
        assert(hdKey.counter.didIndex)(equalTo(42)) &&
        assert(hdKey.counter.verificationRelationship.authentication)(equalTo(4)) &&
        assert(hdKey.counter.verificationRelationship.assertionMethod)(equalTo(2)) &&
        assert(hdKey.counter.verificationRelationship.capabilityDelegation)(equalTo(0)) &&
        assert(hdKey.counter.verificationRelationship.capabilityDelegation)(equalTo(0)) &&
        assert(hdKey.counter.verificationRelationship.keyAgreement)(equalTo(0)) &&
        assert(hdKey.counter.internalKey.master)(equalTo(0)) &&
        assert(hdKey.counter.internalKey.revocation)(equalTo(0)) &&
        // path is correct
        assert(hdKey.newKeyPaths.size)(equalTo(2)) &&
        assert(hdKey.newKeyPaths.get("auth-42").get.keyIndex)(equalTo(3)) &&
        assert(hdKey.newKeyPaths.get("issue-42").get.keyIndex)(equalTo(1)) &&
        // operation is correct
        assert(op.actions)(hasSize(equalTo(2))) &&
        assert(op.actions.collect { case UpdateDIDAction.AddKey(pk) => pk.id })(
          hasSameElements(Seq("auth-42", "issue-42"))
        )
      }
    }
  )

}
