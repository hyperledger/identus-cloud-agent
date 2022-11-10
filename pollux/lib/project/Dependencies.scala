import sbt._

object Dependencies {
  object Versions {
    val zio = "2.0.2"
    val doobie = "1.0.0-RC2"
    val zioCatsInterop = "3.3.0"
    val prismSdk = "v1.3.3-snapshot-1657194253-992dd96"
    val iris = "0.1.0"
    val shared = "0.1.0"
    val mercury = "0.4.0"
  }

  private lazy val zio = "dev.zio" %% "zio" % Versions.zio
  private lazy val zioCatsInterop = "dev.zio" %% "zio-interop-cats" % Versions.zioCatsInterop

  private lazy val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % Versions.doobie
  private lazy val doobieHikari = "org.tpolecat" %% "doobie-hikari" % Versions.doobie

  // We have to exclude bouncycastle since for some reason bitcoinj depends on bouncycastle jdk15to18
  // (i.e. JDK 1.5 to 1.8), but we are using JDK 11
  private lazy val prismCrypto = "io.iohk.atala" % "prism-crypto-jvm" % Versions.prismSdk excludeAll
    ExclusionRule(
      organization = "org.bouncycastle"
    )

  private lazy val shared = "io.iohk.atala" % "shared" % Versions.shared

  private lazy val polluxVcJwt = "io.iohk.atala" %% "pollux-vc-jwt" % "0.1.0-SNAPSHOT" changing ()

  private lazy val irisClient = "io.iohk.atala" %% "iris-client" % Versions.iris

  private lazy val mercuryModels = "io.iohk.atala" %% "mercury-data-models" % Versions.mercury
  private lazy val mercuryAgent = "io.iohk.atala" %% "mercury-agent-didcommx" % Versions.mercury
  private lazy val mercuryResolver = "io.iohk.atala" %% "mercury-resolver" % Versions.mercury

  // Dependency Modules
  private lazy val baseDependencies: Seq[ModuleID] = Seq(zio, prismCrypto, shared)
  private lazy val doobieDependencies: Seq[ModuleID] = Seq(doobiePostgres, doobieHikari)
  private lazy val mercuryDependencies: Seq[ModuleID] = Seq(mercuryModels, mercuryAgent, mercuryResolver)

  // Project Dependencies
  lazy val coreDependencies: Seq[ModuleID] =
    baseDependencies ++ Seq(polluxVcJwt) ++ Seq(irisClient) ++ mercuryDependencies
  lazy val sqlDoobieDependencies: Seq[ModuleID] = baseDependencies ++ doobieDependencies ++ Seq(zioCatsInterop)
}
