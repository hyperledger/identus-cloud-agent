package io.iohk.atala.agent.walletapi.util

import zio.*
import io.iohk.atala.shared.models.HexString
import io.iohk.atala.agent.walletapi.crypto.Apollo

trait SeedResolver {
  def resolve: Task[Array[Byte]]
}

object SeedResolver {
  def layer(isDevMode: Boolean = false): URLayer[Apollo, SeedResolver] =
    ZLayer.fromFunction(SeedResolverImpl(_, isDevMode))
}

private class SeedResolverImpl(apollo: Apollo, isDevMode: Boolean) extends SeedResolver {
  override def resolve: Task[Array[Byte]] = {
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
        _ <- ZIO.logInfo("WALLET_SEED environment is not found.").when(maybeSeed.isEmpty)
        // When DEV_MODE=fase, the WALLET_SEED must be set.
        _ <- ZIO
          .fail(Exception("WALLET_SEED must be present when running with DEV_MODE=false"))
          .when(maybeSeed.isEmpty && !isDevMode)
      } yield maybeSeed

    val seedRand =
      for {
        _ <- ZIO.logInfo("Generating a new wallet seed")
        seedWithMnemonic <- apollo.ecKeyFactory.randomBip32Seed()
        (seed, mnemonic) = seedWithMnemonic
        seedHex = HexString.fromByteArray(seed)
        _ <- ZIO
          .logInfo(s"New seed generated : $seedHex (${mnemonic.mkString("[", ", ", "]")})")
          .when(isDevMode)
      } yield seed

    seedEnv.flatMap {
      case Some(seed) => ZIO.succeed(seed)
      case None       => seedRand
    }
  }

}
