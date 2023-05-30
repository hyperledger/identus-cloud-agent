package io.iohk.atala.agent.walletapi.util

import zio.*
import zio.test.*
import zio.test.Assertion.*
import io.iohk.atala.agent.walletapi.crypto.ApolloSpecHelper

object SeedResolverSpec extends ZIOSpecDefault, ApolloSpecHelper {

  override def spec = suite("SeedResolverSpec ")(
    resolveSpec
  ).provide(Runtime.removeDefaultLoggers)

  private val resolveSpec = suite("resolve")(
    test("generate new seed if not set in env") {
      val result =
        for {
          resolver <- ZIO.service[SeedResolver]
          seed1 <- resolver.resolve
          seed2 <- resolver.resolve
          seed3 <- resolver.resolve
        } yield assert(Set(seed1, seed2, seed3))(hasSize(equalTo(3)))
      result.provide(SeedResolver.layer(), apolloLayer)
    },
    test("read seed from env if set") {
      val result = for {
        _ <- TestSystem.putEnv("WALLET_SEED", "00" * 32)
        seed <- ZIO.serviceWithZIO[SeedResolver](_.resolve)
      } yield assert(seed)(equalTo(Array.fill(32)(0)))
      result.provide(SeedResolver.layer(), apolloLayer)
    },
    test("fail if seed from env in invalid") {
      val result = for {
        _ <- TestSystem.putEnv("WALLET_SEED", "xyz")
        exit <- ZIO.serviceWithZIO[SeedResolver](_.resolve).exit
      } yield assert(exit)(fails(anything))
      result.provide(SeedResolver.layer(), apolloLayer)
    },
    test("read seed override if set") {
      val result = for {
        seed <- ZIO.serviceWithZIO[SeedResolver](_.resolve)
      } yield assert(seed)(equalTo((Array.fill(32)(0))))
      result.provide(SeedResolver.layer(Some("00" * 32)), apolloLayer)
    },
    test("fail if seed override is invalid") {
      val result = for {
        exit <- ZIO.serviceWithZIO[SeedResolver](_.resolve).exit
      } yield assert(exit)(fails(anything))
      result.provide(SeedResolver.layer(Some("xyz")), apolloLayer)
    },
    test("seed override take precedence over WALLET_SEED variable") {
      val result = for {
        _ <- TestSystem.putEnv("WALLET_SEED", "00" * 32)
        seed <- ZIO.serviceWithZIO[SeedResolver](_.resolve)
      } yield assert(seed)(equalTo((Array.fill(32)(1))))
      result.provide(SeedResolver.layer(Some("01" * 32)), apolloLayer)
    }
  )

}
