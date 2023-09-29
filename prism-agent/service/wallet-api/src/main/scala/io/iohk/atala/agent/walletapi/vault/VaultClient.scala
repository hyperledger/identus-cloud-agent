package io.iohk.atala.agent.walletapi.vault

import io.github.jopenlibs.vault.Vault
import io.github.jopenlibs.vault.response.LogicalResponse
import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.*
import zio.*
import io.github.jopenlibs.vault.VaultConfig

trait VaultKVClient {
  def get[T: KVCodec](path: String): Task[Option[T]]
  def set[T: KVCodec](path: String, data: T): Task[Unit]
}

class VaultKVClientImpl(vault: Vault) extends VaultKVClient {

  override def get[T: KVCodec](path: String): Task[Option[T]] = {
    for {
      maybeData <- ZIO
        .attemptBlocking(
          vault
            .logical()
            .read(path)
        )
        .handleVaultErrorOpt("Error reading a secret from Vault.")
        .map(_.map(_.getData().asScala.toMap))
      decodedData <- maybeData.fold(ZIO.none)(data => ZIO.fromTry(summon[KVCodec[T]].decode(data)).asSome)
    } yield decodedData
  }

  override def set[T: KVCodec](path: String, data: T): Task[Unit] = {
    val kv = summon[KVCodec[T]].encode(data)
    for {
      _ <- ZIO
        .attemptBlocking {
          vault
            .logical()
            .write(path, kv.asJava)
        }
        .handleVaultError("Error writing a secret to Vault.")
    } yield ()
  }

  extension [R](resp: RIO[R, LogicalResponse]) {

    /** Handle non 200 Vault response as error and 404 as optioanl */
    def handleVaultErrorOpt(message: String): RIO[R, Option[LogicalResponse]] = {
      resp
        .flatMap { resp =>
          val status = resp.getRestResponse().getStatus()
          val bytes = resp.getRestResponse().getBody()
          val body = new String(bytes, StandardCharsets.UTF_8)
          status match {
            case 200 => ZIO.some(resp)
            case 404 => ZIO.none
            case _ =>
              ZIO
                .fail(Exception(s"$message - Got response status code $status, expected 200"))
                .tapError(_ => ZIO.logError(s"$message - Response status: $status. Response body: $body"))
          }
        }
    }

    /** Handle non 200 Vault response as error */
    def handleVaultError(message: String): RIO[R, LogicalResponse] = {
      resp
        .flatMap { resp =>
          val status = resp.getRestResponse().getStatus()
          val bytes = resp.getRestResponse().getBody()
          val body = new String(bytes, StandardCharsets.UTF_8)
          status match {
            case 200 => ZIO.succeed(resp)
            case _ =>
              ZIO
                .fail(Exception(s"$message - Got response status code $status, expected 200"))
                .tapError(_ => ZIO.logError(s"$message - Response status: $status. Response body: $body"))
          }
        }
    }
  }
}

object VaultKVClientImpl {
  def fromAddressAndToken(address: String, token: String): Task[VaultKVClient] =
    ZIO.attempt {
      val config = VaultConfig()
        .engineVersion(2)
        .address(address)
        .token(token)
        .build()
      val vault = Vault.create(config)
      VaultKVClientImpl(vault)
    }
}
