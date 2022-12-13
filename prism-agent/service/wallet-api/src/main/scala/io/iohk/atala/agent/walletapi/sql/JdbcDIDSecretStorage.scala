package io.iohk.atala.agent.walletapi.sql

import io.iohk.atala.agent.walletapi.storage.DIDSecretStorage

import cats.instances.seq
import doobie.*
import doobie.implicits.*
import io.circe._
import io.circe.parser._
import io.circe.syntax._

import java.time.Instant
import java.util.UUID

import zio.*
import zio.interop.catz.*
import io.iohk.atala.agent.walletapi.model.error.DIDSecretStorageError
import io.iohk.atala.agent.walletapi.model.error.DIDSecretStorageError.KeyNotFoundError
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.agent.walletapi.model.ECKeyPair
import com.nimbusds.jose.jwk.OctetKeyPair

// TODO: replace with actual implementation
class JdbcDIDSecretStorage(xa: Transactor[Task]) extends DIDSecretStorage {

  // Uncomment to have Doobie LogHandler in scope and automatically output SQL statements in logs
  // given logHandler: LogHandler = LogHandler.jdkLogHandler

  given uuidGet: Get[UUID] = Get[String].map(UUID.fromString)
  given uuidPut: Put[UUID] = Put[String].contramap(_.toString())

  given instantGet: Get[Instant] = Get[Long].map(Instant.ofEpochSecond)
  given instantPut: Put[Instant] = Put[Long].contramap(_.getEpochSecond())

  given octetKeyPairGet: Get[OctetKeyPair] = Get[String].map(OctetKeyPair.parse)
  given octetKeyPairPut: Put[OctetKeyPair] = Put[String].contramap(_.toJSONString)

  override def removeDIDSecret(did: PrismDID): Task[Unit] = ???

  override def getKey(did: PrismDID, keyId: String): Task[Option[ECKeyPair]] = ???

  override def upsertKey(did: PrismDID, keyId: String, keyPair: ECKeyPair): Task[Unit] = ???

  override def listKeys(did: PrismDID): Task[Map[String, ECKeyPair]] = ???

  override def removeKey(did: PrismDID, keyId: String): Task[Unit] = ???

  override def getKey(did: DidId, keyId: String): Task[Option[OctetKeyPair]] = {
    val cxnIO = sql"""
        | SELECT
        |   key_pair
        | FROM public.did_secret_storage
        | WHERE 
        |   did = $did
        |   AND key_id = $keyId
        """.stripMargin
      .query[OctetKeyPair]
      .option

    cxnIO.transact(xa)
  }

  override def insertKey(did: DidId, keyId: String, keyPair: OctetKeyPair): Task[Int] = {
    val cxnIO = sql"""
        | INSERT INTO public.did_secret_storage(
        |   did,
        |   created_at,
        |   key_id,
        |   key_pair
        | ) values (
        |   ${did.value},
        |   ${Instant.now()},
        |   ${keyId},
        |   ${keyPair.toJSONString()}
        | )
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

}

object JdbcDIDSecretStorage {
  val layer: URLayer[Transactor[Task], DIDSecretStorage] =
    ZLayer.fromFunction(new JdbcDIDSecretStorage(_))
}
