package io.iohk.atala.pollux.core.service

import io.iohk.atala.agent.walletapi.storage.{GenericSecret, GenericSecretStorage}
import io.iohk.atala.pollux.anoncreds.*
import io.iohk.atala.pollux.core.model.error.LinkSecretError
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*
import zio.json.ast.Json

import scala.util.Try

class LinkSecretServiceImpl(genericSecretStorage: GenericSecretStorage) extends LinkSecretService {

  import LinkSecretServiceImpl.given

  type Result[T] = ZIO[WalletAccessContext, LinkSecretError, T]

  override def fetchOrCreate(): Result[lib.LinkSecretWithId] = {
    genericSecretStorage
      .get[String, lib.LinkSecret](LinkSecretServiceImpl.defaultLinkSecretId)
      .flatMap {
        case Some(secret) => ZIO.succeed(secret)
        case None =>
          val linkSecret = lib.LinkSecret()
          genericSecretStorage
            .set[String, lib.LinkSecret](LinkSecretServiceImpl.defaultLinkSecretId, linkSecret)
            .as(linkSecret)
      }
      .map(linkSecret => lib.LinkSecretWithId(LinkSecretServiceImpl.defaultLinkSecretId, linkSecret))
      .mapError(LinkSecretError.apply)
  }
}

object LinkSecretServiceImpl {
  val defaultLinkSecretId = "default-link-secret-id"

  val layer: URLayer[
    GenericSecretStorage,
    LinkSecretService
  ] =
    ZLayer.fromFunction(LinkSecretServiceImpl(_))

  given GenericSecret[String, lib.LinkSecret] = new {
    override def keyPath(id: String): String = s"link-secret/${id.toString}"

    override def encodeValue(secret: lib.LinkSecret): Json = Json.Str(secret.data)

    override def decodeValue(json: Json): Try[lib.LinkSecret] = json match {
      case Json.Str(data) => Try(lib.LinkSecret(data))
      case _              => scala.util.Failure(new Exception("Invalid JSON format for LinkSecret"))
    }
  }
}
