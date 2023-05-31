package io.iohk.atala.agent.walletapi.util

import zio.*
import io.iohk.atala.shared.models.HexString
import io.iohk.atala.agent.walletapi.crypto.Apollo

trait SeedResolver {
  def resolve: Task[Array[Byte]]
}

object SeedResolver {
  def layer(seedOverrideHex: Option[String] = None): URLayer[Apollo, SeedResolver] =
    ZLayer.fromFunction(SeedResolverImpl(_, seedOverrideHex))
}

private class SeedResolverImpl(apollo: Apollo, seedOverrideHex: Option[String]) extends SeedResolver {
  override def resolve: Task[Array[Byte]] = {
    val seedOverride =
      for {
        _ <- ZIO.logInfo("Resolving a wallet seed using seed-override")
        maybeSeed <- seedOverrideHex
          .fold(ZIO.none) { hex =>
            ZIO.fromTry(HexString.fromString(hex)).map(_.toByteArray).asSome
          }
          .tapError(e => ZIO.logError("Failed to parse seed-override"))
        _ <- ZIO.logInfo("seed-override is not found. Fallback to the next resolver").when(maybeSeed.isEmpty)
      } yield maybeSeed

    val seedEnv =
      for {
        _ <- ZIO.logInfo("Resolving a wallet seed using WALLET_SEED environemnt variable")
        maybeSeed <- System
          .env("WALLET_SEED")
          .flatMap {
            case Some(hex) => ZIO.fromTry(HexString.fromString(hex)).map(_.toByteArray).asSome
            case None      => ZIO.none
          }
          .tapError(e => ZIO.logError("Failed to parse WALLET_SEED"))
        _ <- ZIO.logInfo("WALLET_SEED environment is not found. Fallback to the next resolver").when(maybeSeed.isEmpty)
      } yield maybeSeed

    val seedRand =
      for {
        _ <- ZIO.logInfo("Generating a new wallet seed")
        seed <- apollo.ecKeyFactory.randomBip32Seed()
      } yield seed

    seedOverride
      .flatMap {
        case Some(seed) => ZIO.some(seed)
        case None       => seedEnv
      }
      .flatMap {
        case Some(seed) => ZIO.succeed(seed)
        case None       => seedRand
      }
  }
}
