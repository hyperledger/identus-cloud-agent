package io.iohk.atala.agent.walletapi.util

import zio.*
import zio.test.*
import zio.test.Assertion.*
import io.iohk.atala.agent.walletapi.crypto.ApolloSpecHelper

object SeedResolverSpec extends ZIOSpecDefault, ApolloSpecHelper {

  override def spec =
    suite("SeedResolverSpec ")(
      resolveSpecDevMode,
      resolveSpecProdMode
    ).provide(Runtime.removeDefaultLoggers)

  private val resolveSpecDevMode = suite("resolve - DEV_MODE=true")(
    test("generate new seed if not set in env") {
      val result =
        for {
          resolver <- ZIO.service[SeedResolver]
          seed1 <- resolver.resolve
          seed2 <- resolver.resolve
          seed3 <- resolver.resolve
        } yield assert(Set(seed1, seed2, seed3))(hasSize(equalTo(3)))
      result.provide(SeedResolver.layer(isDevMode = true), apolloLayer)
    },
    test("read seed from env if set") {
      val result = for {
        _ <- TestSystem.putEnv("WALLET_SEED", "00" * 64)
        seed <- ZIO.serviceWithZIO[SeedResolver](_.resolve)
      } yield assert(seed.toByteArray)(equalTo(Array.fill(64)(0)))
      result.provide(SeedResolver.layer(isDevMode = true), apolloLayer)
    },
    test("fail if seed from env in invalid") {
      val result = for {
        _ <- TestSystem.putEnv("WALLET_SEED", "xyz")
        exit <- ZIO.serviceWithZIO[SeedResolver](_.resolve).exit
      } yield assert(exit)(fails(anything))
      result.provide(SeedResolver.layer(isDevMode = true), apolloLayer)
    },
    test("fail if seed is valid hex but not a 64-bytes seed") {
      val result = for {
        _ <- TestSystem.putEnv("WALLET_SEED", "00" * 32)
        exit <- ZIO.serviceWithZIO[SeedResolver](_.resolve).exit
      } yield assert(exit)(fails(anything))
      result.provide(SeedResolver.layer(isDevMode = true), apolloLayer)
    }
  )

  private val resolveSpecProdMode = suite("resolve - DEV_MODE=false")(
    test("fail when WALLET_SEED is not set") {
      val result =
        for {
          resolver <- ZIO.service[SeedResolver]
          exit <- resolver.resolve.exit
        } yield assert(exit)(fails(anything))
      result.provide(SeedResolver.layer(isDevMode = false), apolloLayer)
    },
    test("read seed from env if set") {
      val result = for {
        _ <- TestSystem.putEnv("WALLET_SEED", "00" * 64)
        seed <- ZIO.serviceWithZIO[SeedResolver](_.resolve)
      } yield assert(seed.toByteArray)(equalTo(Array.fill(64)(0)))
      result.provide(SeedResolver.layer(isDevMode = false), apolloLayer)
    },
    test("fail if seed from env in invalid") {
      val result = for {
        _ <- TestSystem.putEnv("WALLET_SEED", "xyz")
        exit <- ZIO.serviceWithZIO[SeedResolver](_.resolve).exit
      } yield assert(exit)(fails(anything))
      result.provide(SeedResolver.layer(isDevMode = false), apolloLayer)
    },
    test("fail if seed is valid hex but not a 64-bytes seed") {
      val result = for {
        _ <- TestSystem.putEnv("WALLET_SEED", "00" * 32)
        exit <- ZIO.serviceWithZIO[SeedResolver](_.resolve).exit
      } yield assert(exit)(fails(anything))
      result.provide(SeedResolver.layer(isDevMode = false), apolloLayer)
    }
  )

}
