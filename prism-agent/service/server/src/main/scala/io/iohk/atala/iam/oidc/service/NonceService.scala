package io.iohk.atala.iam.oidc.service

import io.iohk.atala.iam.oidc.service.NonceService.NonceGenerator
import zio.Task

import java.time.Instant
import scala.collection.concurrent.TrieMap

trait NonceService {
  def generateNonce()(implicit gen: NonceGenerator): String = gen()
  def validateNonce(nonce: String): Task[Boolean]
  def storeNonce(nonce: String, expireAt: Long): Task[Unit]
}

object NonceService {
  type NonceGenerator = () => String
  given randomUUID: NonceGenerator = () => java.util.UUID.randomUUID().toString
}

case class InMemoryNonceService() extends NonceService {
  import zio.{Task, ZIO}
  private case class NonceRecord(nonce: String, expireAt: Long, fired: Boolean = false)

  private val nonces: TrieMap[String, NonceRecord] = TrieMap.empty

  override def validateNonce(nonce: String): Task[Boolean] = {
    nonces.get(nonce) match {
      case None =>
        ZIO.succeed(false)
      case Some(n) if !n.fired && n.expireAt > Instant.now().toEpochMilli =>
        nonces.replace(nonce, n, n.copy(fired = true))
        ZIO.succeed(true)
    }
  }

  override def storeNonce(nonce: String, expireAt: Long): Task[Unit] = {
    nonces.putIfAbsent(nonce, NonceRecord(nonce, expireAt)) match {
      case Some(_) => ZIO.fail(new RuntimeException(s"Nonce $nonce already exists"))
      case None    => ZIO.succeed(())
    }
  }
}
