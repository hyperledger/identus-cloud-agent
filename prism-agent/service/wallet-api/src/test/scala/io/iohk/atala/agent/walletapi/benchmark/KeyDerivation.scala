package io.iohk.atala.agent.walletapi.benchmark

import zio.*
import zio.test.*
import zio.test.Assertion.*
import io.iohk.atala.agent.walletapi.crypto.Apollo
import io.iohk.atala.castor.core.model.did.EllipticCurve
import io.iohk.atala.shared.models.HexString
import io.iohk.atala.agent.walletapi.crypto.DerivationPath

object KeyDerivation extends ZIOSpecDefault {

  private val seedHex = "00" * 64
  private val seed = HexString.fromStringUnsafe(seedHex).toByteArray

  override def spec = suite("Key derivation benchamrk")(
    deriveKeyBenchmark.provideLayer(Apollo.prism14Layer),
    queryKeyBenchmark
  ) @@ TestAspect.sequential @@ TestAspect.timed @@ TestAspect.tag("benchmark")

  private val deriveKeyBenchmark = suite("Derive key benchmark")(
    test("derive 10000 keys - sequential") {
      for {
        apollo <- ZIO.service[Apollo]
        durationList <- ZIO
          .foreach(1 to 10000) { i =>
            Live.live {
              apollo.ecKeyFactory
                .deriveKeyPair(EllipticCurve.SECP256K1, seed)(derivationPath(keyIndex = i): _*)
                .timed
                .map(_._1)
            }
          }
        _ <- logStats(durationList)
      } yield assertCompletes
    },
    test("derive 10000 keys - 16 parallelism") {
      for {
        apollo <- ZIO.service[Apollo]
        durationList <- ZIO
          .foreachPar(1 to 10000) { i =>
            Live.live {
              apollo.ecKeyFactory
                .deriveKeyPair(EllipticCurve.SECP256K1, seed)(derivationPath(keyIndex = i): _*)
                .timed
                .map(_._1)
            }
          }
          .withParallelism(16)
          .map(_.toVector.sorted)
        _ <- logStats(durationList)
      } yield assertCompletes
    },
    test("derive 10000 keys - 32 parallelism") {
      for {
        apollo <- ZIO.service[Apollo]
        durationList <- ZIO
          .foreachPar(1 to 10000) { i =>
            Live.live {
              apollo.ecKeyFactory
                .deriveKeyPair(EllipticCurve.SECP256K1, seed)(derivationPath(keyIndex = i): _*)
                .timed
                .map(_._1)
            }
          }
          .withParallelism(32)
          .map(_.toVector.sorted)
        _ <- logStats(durationList)
      } yield assertCompletes
    },
  ) @@ TestAspect.beforeAll(deriveKeyWarmUp())

  private val queryKeyBenchmark = suite("Query key benchmark - vault storage")(
    test("query 10000 keys - sequential") {
      assertCompletes
    }
  )

  private def deriveKeyWarmUp(n: Int = 10000) = {
    for {
      _ <- ZIO.debug("running key derivation warm-up")
      apollo <- ZIO.service[Apollo]
      _ <- ZIO
        .foreach(1 to n) { i =>
          Live.live {
            apollo.ecKeyFactory
              .deriveKeyPair(EllipticCurve.SECP256K1, seed)(derivationPath(keyIndex = i): _*)
              .timed
              .map(_._1)
          }
        }
    } yield ()
  }

  private def derivationPath(keyIndex: Int = 0): Seq[DerivationPath] = {
    Seq(
      DerivationPath.Hardened(0x1d),
      DerivationPath.Hardened(0),
      DerivationPath.Hardened(0),
      DerivationPath.Hardened(keyIndex),
    )
  }

  private def logStats(durationList: Seq[Duration]) = {
    val sortedDurationInMicro = durationList.sorted.map(_.toNanos() / 1000.0)
    val avg = sortedDurationInMicro.sum / durationList.length
    val p50 = sortedDurationInMicro.apply(5000)
    val p75 = sortedDurationInMicro.apply(7500)
    val p90 = sortedDurationInMicro.apply(9000)
    val p99 = sortedDurationInMicro.apply(9900)
    ZIO.debug(s"execution time in us. avg: $avg | p50: $p50 | p90: $p90 | p99: $p99")
  }
}
