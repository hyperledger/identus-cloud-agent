import sbt._

object Dependencies {
  object Versions {
    val zio = "2.0.8"
    val doobie = "1.0.0-RC2"
    val zioCatsInterop = "3.3.0"
    val prismNodeClient = "0.3.0"
    val prismSdk = "v1.4.1" // scala-steward:off
    val shared = "0.2.0"
    val flyway = "9.8.3"
  }

  private lazy val zio = "dev.zio" %% "zio" % Versions.zio
  private lazy val zioCatsInterop = "dev.zio" %% "zio-interop-cats" % Versions.zioCatsInterop

  private lazy val zioTest = "dev.zio" %% "zio-test" % Versions.zio % Test
  private lazy val zioTestSbt = "dev.zio" %% "zio-test-sbt" % Versions.zio % Test
  private lazy val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % Versions.zio % Test

  private lazy val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % Versions.doobie
  private lazy val doobieHikari = "org.tpolecat" %% "doobie-hikari" % Versions.doobie

  private lazy val flyway = "org.flywaydb" % "flyway-core" % Versions.flyway

  private lazy val shared = "io.iohk.atala" % "shared" % Versions.shared
  private lazy val prismNodeClient = "io.iohk.atala" %% "prism-node-client" % Versions.prismNodeClient

  // We have to exclude bouncycastle since for some reason bitcoinj depends on bouncycastle jdk15to18
  // (i.e. JDK 1.5 to 1.8), but we are using JDK 11
  private lazy val prismCrypto = "io.iohk.atala" % "prism-crypto-jvm" % Versions.prismSdk excludeAll
    ExclusionRule(
      organization = "org.bouncycastle"
    )
  private lazy val prismIdentity = "io.iohk.atala" % "prism-identity-jvm" % Versions.prismSdk

  // Dependency Modules
  private lazy val baseDependencies: Seq[ModuleID] = Seq(zio, zioTest, zioTestSbt, zioTestMagnolia, shared, prismCrypto, prismIdentity, prismNodeClient)
  private lazy val doobieDependencies: Seq[ModuleID] = Seq(doobiePostgres, doobieHikari, flyway)

  // Project Dependencies
  lazy val coreDependencies: Seq[ModuleID] = baseDependencies
  lazy val sqlDoobieDependencies: Seq[ModuleID] = baseDependencies ++ doobieDependencies ++ Seq(zioCatsInterop)
}
