package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.shared.models.WalletAccessContext
import zio.*
import zio.json.ast.Json

import java.util.UUID
import scala.util.Try

final case class CredentialDefinitionSecret(json: Json) // to be moved to pollux?

trait GenericSecretStorage {
  def set[K, V](key: K, secret: V)(implicit ev: GenericSecret[K, V]): RIO[WalletAccessContext, Unit]
  def get[K, V](key: K)(implicit ev: GenericSecret[K, V]): RIO[WalletAccessContext, Option[V]]
}

trait GenericSecret[K, V] {
  def keyPath(id: K): String
  def encodeValue(secret: V): Json
  def decodeValue(json: Json): Try[V]
}

object GenericSecret {
  given GenericSecret[UUID, CredentialDefinitionSecret] = new {
    override def keyPath(id: UUID): String = s"credential-definitions/${id.toString()}"
    override def encodeValue(secret: CredentialDefinitionSecret): Json = secret.json
    override def decodeValue(json: Json): Try[CredentialDefinitionSecret] = Try(CredentialDefinitionSecret(json))
  }
}
