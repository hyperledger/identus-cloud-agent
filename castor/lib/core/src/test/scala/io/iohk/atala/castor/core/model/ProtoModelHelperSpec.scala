package io.iohk.atala.castor.core.model

import com.google.protobuf.timestamp.Timestamp
import zio.*
import zio.test.*
import zio.test.Assertion.*
import io.iohk.atala.prism.protos.node_models

import java.time.Instant

object ProtoModelHelperSpec extends ZIOSpecDefault {

  import ProtoModelHelper.*

  override def spec = suite("ProtoModelHelper")(protobufModelExtensionSpec)

  extension (i: Instant) {
    def toTimestamp: Timestamp = Timestamp.of(i.getEpochSecond, i.getNano)
  }

  private val protobufModelExtensionSpec = suite("DIDData.filterRevokedKeysAndServices")(
    test("not filter keys if revokedOn is empty") {
      val didData = node_models.DIDData(
        id = "123",
        publicKeys = Seq(
          node_models.PublicKey(id = "key1"),
          node_models.PublicKey(id = "key2"),
          node_models.PublicKey(id = "key3")
        )
      )
      assertZIO(didData.filterRevokedKeysAndServices.map(_.publicKeys.map(_.id)))(
        hasSameElements(Seq("key1", "key2", "key3"))
      )
    },
    test("not filter keys if revokedOn timestamp has not passed") {
      for {
        now <- Clock.instant
        revokeTime = now.minusSeconds(5)
        ledgerData = node_models.LedgerData(timestampInfo =
          Some(node_models.TimestampInfo(blockTimestamp = Some(revokeTime.toTimestamp)))
        )
        didData = node_models.DIDData(
          id = "123",
          publicKeys = Seq(
            node_models.PublicKey(id = "key1"),
            node_models.PublicKey(id = "key2", revokedOn = Some(ledgerData)),
            node_models.PublicKey(id = "key3", revokedOn = Some(ledgerData))
          )
        )
        validKeysId <- didData.filterRevokedKeysAndServices.map(_.publicKeys.map(_.id))
      } yield assert(validKeysId)(hasSameElements(Seq("key1", "key2", "key3")))
    },
    test("filter keys if revokedOn timestamp has passed") {
      for {
        now <- Clock.instant
        revokeTime = now.plusSeconds(5)
        ledgerData = node_models.LedgerData(timestampInfo =
          Some(node_models.TimestampInfo(blockTimestamp = Some(revokeTime.toTimestamp)))
        )
        didData = node_models.DIDData(
          id = "123",
          publicKeys = Seq(
            node_models.PublicKey(id = "key1"),
            node_models.PublicKey(id = "key2", revokedOn = Some(ledgerData)),
            node_models.PublicKey(id = "key3", revokedOn = Some(ledgerData))
          )
        )
        validKeysId <- didData.filterRevokedKeysAndServices.map(_.publicKeys.map(_.id))
      } yield assert(validKeysId)(hasSameElements(Seq("key1")))
    },
    test("filter keys if revokedOn timestamp is exactly now") {
      for {
        now <- Clock.instant
        ledgerData = node_models.LedgerData(timestampInfo =
          Some(node_models.TimestampInfo(blockTimestamp = Some(now.toTimestamp)))
        )
        didData = node_models.DIDData(
          id = "123",
          publicKeys = Seq(node_models.PublicKey(id = "key1", revokedOn = Some(ledgerData)))
        )
        validKeysId <- didData.filterRevokedKeysAndServices.map(_.publicKeys.map(_.id))
      } yield assert(validKeysId)(isEmpty)
    },
    test("not filter services if deletedOn is empty") {
      val didData = node_models.DIDData(
        id = "123",
        services = Seq(
          node_models.Service(id = "service1"),
          node_models.Service(id = "service2"),
          node_models.Service(id = "service3")
        )
      )
      assertZIO(didData.filterRevokedKeysAndServices.map(_.services.map(_.id)))(
        hasSameElements(Seq("service1", "service2", "service3"))
      )
    },
    test("not filter services if deletedOn timestamp has not passed") {
      for {
        now <- Clock.instant
        revokeTime = now.minusSeconds(5)
        ledgerData = node_models.LedgerData(timestampInfo =
          Some(node_models.TimestampInfo(blockTimestamp = Some(revokeTime.toTimestamp)))
        )
        didData = node_models.DIDData(
          id = "123",
          services = Seq(
            node_models.Service(id = "key1"),
            node_models.Service(id = "key2", deletedOn = Some(ledgerData)),
            node_models.Service(id = "key3", deletedOn = Some(ledgerData))
          )
        )
        validKeysId <- didData.filterRevokedKeysAndServices.map(_.services.map(_.id))
      } yield assert(validKeysId)(hasSameElements(Seq("key1", "key2", "key3")))
    },
    test("filter services if deletedOn timestamp has passed") {
      for {
        now <- Clock.instant
        revokeTime = now.plusSeconds(5)
        ledgerData = node_models.LedgerData(timestampInfo =
          Some(node_models.TimestampInfo(blockTimestamp = Some(revokeTime.toTimestamp)))
        )
        didData = node_models.DIDData(
          id = "123",
          services = Seq(
            node_models.Service(id = "key1"),
            node_models.Service(id = "key2", deletedOn = Some(ledgerData)),
            node_models.Service(id = "key3", deletedOn = Some(ledgerData))
          )
        )
        validKeysId <- didData.filterRevokedKeysAndServices.map(_.services.map(_.id))
      } yield assert(validKeysId)(hasSameElements(Seq("key1")))
    },
    test("filter services if deletedOn timestamp is exactly now") {
      for {
        now <- Clock.instant
        ledgerData = node_models.LedgerData(timestampInfo =
          Some(node_models.TimestampInfo(blockTimestamp = Some(now.toTimestamp)))
        )
        didData = node_models.DIDData(
          id = "123",
          services = Seq(node_models.Service(id = "key1", deletedOn = Some(ledgerData)))
        )
        validKeysId <- didData.filterRevokedKeysAndServices.map(_.services.map(_.id))
      } yield assert(validKeysId)(isEmpty)
    }
  )

}
