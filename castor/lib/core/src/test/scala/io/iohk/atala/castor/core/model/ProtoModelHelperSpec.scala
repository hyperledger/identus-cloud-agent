package io.iohk.atala.castor.core.model

import com.google.protobuf.timestamp.Timestamp
import io.iohk.atala.castor.core.model.did.ServiceType
import io.iohk.atala.castor.core.util.GenUtils
import io.iohk.atala.prism.protos.common_models.Ledger
import zio.*
import zio.test.*
import zio.test.Assertion.*
import io.iohk.atala.prism.protos.node_models

import java.time.Instant

object ProtoModelHelperSpec extends ZIOSpecDefault {

  import ProtoModelHelper.*

  private def makePublicKey(id: String, revokedOn: Option[node_models.LedgerData] = None): node_models.PublicKey =
    node_models.PublicKey(
      id = id,
      usage = node_models.KeyUsage.AUTHENTICATION_KEY,
      addedOn = None,
      revokedOn = revokedOn,
      keyData = node_models.PublicKey.KeyData.CompressedEcKeyData(
        node_models.CompressedECKeyData("secp256k1", Array.emptyByteArray.toProto)
      )
    )

  private def makeService(id: String, deletedOn: Option[node_models.LedgerData] = None): node_models.Service =
    node_models.Service(
      id = id,
      `type` = ServiceType.LinkedDomains.name,
      serviceEndpoint = Seq(),
      addedOn = None,
      deletedOn = deletedOn
    )

  override def spec = suite("ProtoModelHelper")(conversionSpec, didDataFilterSpec)

  extension (i: Instant) {
    def toLedgerData: node_models.LedgerData = {
      val timestamp = Timestamp.of(i.getEpochSecond, i.getNano)
      val timestampInfo = node_models.TimestampInfo(
        blockSequenceNumber = 0,
        operationSequenceNumber = 0,
        blockTimestamp = Some(timestamp)
      )
      node_models.LedgerData(
        transactionId = "",
        ledger = Ledger.IN_MEMORY,
        timestampInfo = Some(timestampInfo)
      )
    }
  }

  private val conversionSpec = suite("round trip model conversion does not change data of models")(
    test("PublicKeyData") {
      check(GenUtils.publicKeyData) { pkd =>
        val result = pkd.toProto.toDomain
        assert(result)(isRight(equalTo(pkd)))
      }
    },
    test("PublicKey") {
      check(GenUtils.publicKey) { pk =>
        val result = pk.toProto.toDomain
        assert(result)(isRight(equalTo(pk)))
      }
    },
    test("InternalPublicKey") {
      check(GenUtils.internalPublicKey) { pk =>
        val result = pk.toProto.toDomain
        assert(result)(isRight(equalTo(pk)))
      }
    },
    test("Service") {
      check(GenUtils.service) { service =>
        val result = service.toProto.toDomain
        assert(result)(isRight(equalTo(service)))
      }
    }
  )

  private val didDataFilterSpec = suite("filterRevokedKeysAndServices")(
    test("not filter keys if revokedOn is empty") {
      val didData = node_models.DIDData(
        id = "123",
        publicKeys = Seq(
          makePublicKey(id = "key1"),
          makePublicKey(id = "key2"),
          makePublicKey(id = "key3")
        ),
        services = Seq(),
        context = Seq()
      )
      assertZIO(didData.filterRevokedKeysAndServices.map(_.publicKeys.map(_.id)))(
        hasSameElements(Seq("key1", "key2", "key3"))
      )
    },
    test("not filter keys if revokedOn timestamp has not passed") {
      for {
        now <- Clock.instant
        revokeTime = now.plusSeconds(5)
        ledgerData = revokeTime.toLedgerData
        didData = node_models.DIDData(
          id = "123",
          publicKeys = Seq(
            makePublicKey("key1"),
            makePublicKey("key2", revokedOn = Some(ledgerData)),
            makePublicKey("key3", revokedOn = Some(ledgerData))
          ),
          services = Seq(),
          context = Seq()
        )
        validKeysId <- didData.filterRevokedKeysAndServices.map(_.publicKeys.map(_.id))
      } yield assert(validKeysId)(hasSameElements(Seq("key1", "key2", "key3")))
    },
    test("filter keys if revokedOn timestamp has passed") {
      for {
        now <- Clock.instant
        revokeTime = now.minusSeconds(5)
        ledgerData = revokeTime.toLedgerData
        didData = node_models.DIDData(
          id = "123",
          publicKeys = Seq(
            makePublicKey("key1"),
            makePublicKey("key2", revokedOn = Some(ledgerData)),
            makePublicKey("key3", revokedOn = Some(ledgerData))
          ),
          services = Seq(),
          context = Seq()
        )
        validKeysId <- didData.filterRevokedKeysAndServices.map(_.publicKeys.map(_.id))
      } yield assert(validKeysId)(hasSameElements(Seq("key1")))
    },
    test("filter keys if revokedOn timestamp is exactly now") {
      for {
        now <- Clock.instant
        ledgerData = now.toLedgerData
        didData = node_models.DIDData(
          id = "123",
          publicKeys = Seq(makePublicKey(id = "key1", revokedOn = Some(ledgerData))),
          services = Seq(),
          context = Seq()
        )
        validKeysId <- didData.filterRevokedKeysAndServices.map(_.publicKeys.map(_.id))
      } yield assert(validKeysId)(isEmpty)
    },
    test("not filter services if deletedOn is empty") {
      val didData = node_models.DIDData(
        id = "123",
        publicKeys = Seq(),
        services = Seq(
          makeService("service1"),
          makeService("service2"),
          makeService("service3")
        ),
        context = Seq()
      )
      assertZIO(didData.filterRevokedKeysAndServices.map(_.services.map(_.id)))(
        hasSameElements(Seq("service1", "service2", "service3"))
      )
    },
    test("not filter services if deletedOn timestamp has not passed") {
      for {
        now <- Clock.instant
        revokeTime = now.plusSeconds(5)
        ledgerData = revokeTime.toLedgerData
        didData = node_models.DIDData(
          id = "123",
          publicKeys = Seq(),
          services = Seq(
            makeService(id = "key1"),
            makeService(id = "key2", deletedOn = Some(ledgerData)),
            makeService(id = "key3", deletedOn = Some(ledgerData))
          ),
          context = Seq()
        )
        validKeysId <- didData.filterRevokedKeysAndServices.map(_.services.map(_.id))
      } yield assert(validKeysId)(hasSameElements(Seq("key1", "key2", "key3")))
    },
    test("filter services if deletedOn timestamp has passed") {
      for {
        now <- Clock.instant
        revokeTime = now.minusSeconds(5)
        ledgerData = revokeTime.toLedgerData
        didData = node_models.DIDData(
          id = "123",
          publicKeys = Seq(),
          services = Seq(
            makeService(id = "key1"),
            makeService(id = "key2", deletedOn = Some(ledgerData)),
            makeService(id = "key3", deletedOn = Some(ledgerData))
          ),
          context = Seq()
        )
        validKeysId <- didData.filterRevokedKeysAndServices.map(_.services.map(_.id))
      } yield assert(validKeysId)(hasSameElements(Seq("key1")))
    },
    test("filter services if deletedOn timestamp is exactly now") {
      for {
        now <- Clock.instant
        ledgerData = now.toLedgerData
        didData = node_models.DIDData(
          id = "123",
          publicKeys = Seq(),
          services = Seq(makeService(id = "key1", deletedOn = Some(ledgerData))),
          context = Seq()
        )
        validKeysId <- didData.filterRevokedKeysAndServices.map(_.services.map(_.id))
      } yield assert(validKeysId)(isEmpty)
    }
  )

}
