package io.iohk.atala.agent.walletapi.vault

import io.github.jopenlibs.vault.Vault
import io.github.jopenlibs.vault.VaultConfig
import io.github.jopenlibs.vault.response.LogicalResponse
import zio.*

import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.*

trait VaultKVClient {
  def get[T: KVCodec](path: String): Task[Option[T]]
  def set[T: KVCodec](path: String, data: T): Task[Unit]
}

class VaultKVClientImpl(vaultRef: Ref[Vault]) extends VaultKVClient {
  import VaultKVClientImpl.*

  override def get[T: KVCodec](path: String): Task[Option[T]] = {
    for {
      vault <- vaultRef.get
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
      vault <- vaultRef.get
      _ <- ZIO
        .attemptBlocking {
          vault
            .logical()
            .write(path, kv.asJava)
        }
        .handleVaultError("Error writing a secret to Vault.")
    } yield ()
  }

}

object VaultKVClientImpl {

  private final case class TokenRefreshState(leaseDuration: Duration)

  def fromToken(address: String, token: String): Task[VaultKVClient] =
    for {
      vault <- createVaultInstance(address, Some(token))
      vaultRef <- Ref.make(vault)
    } yield VaultKVClientImpl(vaultRef)

  def fromAppRole(address: String, roleId: String, secretId: String): Task[VaultKVClient] =
    for {
      vault <- createVaultInstance(address)
      vaultRef <- vaultTokenRefreshLogic(vault, address, roleId, secretId)
    } yield VaultKVClientImpl(vaultRef)

  private def vaultTokenRefreshLogic(
      authVault: Vault,
      address: String,
      roleId: String,
      secretId: String,
      tokenRefreshBuffer: Duration = 15.seconds,
      retrySchedule: Schedule[Any, Any, Any] = Schedule.spaced(5.second) && Schedule.recurs(10)
  ): Task[Ref[Vault]] = {
    val getToken = ZIO
      .attempt {
        val authResponse = authVault.auth().loginByAppRole(roleId, secretId)
        val ttlSecond = authResponse.getAuthLeaseDuration()
        val token = authResponse.getAuthClientToken()
        (token, ttlSecond)
      }
      .retry(retrySchedule)

    for {
      tokenWithTtl <- getToken
      (token, ttlSecond) = tokenWithTtl
      vaultWithToken <- createVaultInstance(address, Some(token))
      vaultRef <- Ref.make(vaultWithToken)
      _ <- ZIO
        .iterate(TokenRefreshState(ttlSecond.seconds))(_ => true) { state =>
          val durationUntilRefresh = state.leaseDuration.minus(tokenRefreshBuffer).max(1.second)
          for {
            _ <- ZIO.sleep(durationUntilRefresh)
            tokenWithTtl <- getToken
            (token, ttlSecond) = tokenWithTtl
            vaultWithToken <- createVaultInstance(address, Some(token))
            _ <- vaultRef.set(vaultWithToken)
          } yield state.copy(leaseDuration = ttlSecond.seconds)
        }
        .fork
    } yield vaultRef
  }

  private def createVaultInstance(address: String, token: Option[String] = None): Task[Vault] =
    ZIO.attempt {
      val config = VaultConfig()
        .engineVersion(2)
        .address(address)
      token.foreach(config.token)
      Vault.create(config.build())
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
