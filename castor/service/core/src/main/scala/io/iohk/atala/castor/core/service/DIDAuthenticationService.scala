package io.iohk.atala.castor.core.service

import zio.*

import scala.concurrent.duration.FiniteDuration

// TODO: replace with actual implementation
trait DIDAuthenticationService {
  def createAuthChallenge(state: Option[String], ttl: FiniteDuration): UIO[Unit]
  def createAuthChallengeSubmission(challenge: String, signature: String): UIO[Unit]
}

object MockDIDAuthenticationService {
  val layer: ULayer[DIDAuthenticationService] = ZLayer.succeed {
    new DIDAuthenticationService {
      override def createAuthChallenge(state: Option[String], ttl: FiniteDuration): UIO[Unit] = ZIO.unit
      override def createAuthChallengeSubmission(challenge: String, signature: String): UIO[Unit] = ZIO.unit
    }
  }
}
