import sbt._

object Dependencies {
  object Versions {
    val zio = "2.0.2"
    val doobie = "1.0.0-RC2"
    val zioCatsInterop = "3.3.0"
    val prismSdk = "v1.4.1"
    val iris = "0.1.0"
    val shared = "0.1.0"
    val mercury = "0.7.0"
    val flyway = "9.7.0"
  }

  private lazy val zio = "dev.zio" %% "zio" % Versions.zio
  private lazy val zioCatsInterop = "dev.zio" %% "zio-interop-cats" % Versions.zioCatsInterop

  private lazy val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % Versions.doobie
  private lazy val doobieHikari = "org.tpolecat" %% "doobie-hikari" % Versions.doobie

  private lazy val flyway = "org.flywaydb" % "flyway-core" % Versions.flyway

  // We have to exclude bouncycastle since for some reason bitcoinj depends on bouncycastle jdk15to18
  // (i.e. JDK 1.5 to 1.8), but we are using JDK 11
  private lazy val prismCrypto = "io.iohk.atala" % "prism-crypto-jvm" % Versions.prismSdk excludeAll
    ExclusionRule(
      organization = "org.bouncycastle"
    )

  private lazy val shared = "io.iohk.atala" % "shared" % Versions.shared
  private lazy val irisClient = "io.iohk.atala" %% "iris-client" % Versions.iris

  private lazy val mercuryProtocolIssueCredential =
    "io.iohk.atala" %% "mercury-protocol-issue-credential" % Versions.mercury
  private lazy val mercuryProtocolPresentProof =
    "io.iohk.atala" %% "mercury-protocol-present-proof" % Versions.mercury
  // Dependency Modules
  private lazy val baseDependencies: Seq[ModuleID] = Seq(zio, prismCrypto, shared)
  private lazy val doobieDependencies: Seq[ModuleID] = Seq(doobiePostgres, doobieHikari, flyway)

  // Project Dependencies
  lazy val coreDependencies: Seq[ModuleID] =
    baseDependencies ++ Seq(irisClient) ++ Seq(mercuryProtocolIssueCredential,mercuryProtocolPresentProof)
  lazy val sqlDoobieDependencies: Seq[ModuleID] = baseDependencies ++ doobieDependencies ++ Seq(zioCatsInterop)
}
