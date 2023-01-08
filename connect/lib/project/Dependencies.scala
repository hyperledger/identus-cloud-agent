import sbt._

object Dependencies {
  object Versions {
    val zio = "2.0.5"
    val doobie = "1.0.0-RC2"
    val zioCatsInterop = "3.3.0"
    val iris = "0.1.0"
    val mercury = "0.15.0"
    val flyway = "9.8.3"
    val shared = "0.2.0"
  }

  private lazy val zio = "dev.zio" %% "zio" % Versions.zio
  private lazy val zioCatsInterop = "dev.zio" %% "zio-interop-cats" % Versions.zioCatsInterop

  private lazy val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % Versions.doobie
  private lazy val doobieHikari = "org.tpolecat" %% "doobie-hikari" % Versions.doobie

  private lazy val flyway = "org.flywaydb" % "flyway-core" % Versions.flyway

  private lazy val mercuryProtocolConnection = "io.iohk.atala" %% "mercury-protocol-connection" % Versions.mercury

  private lazy val shared = "io.iohk.atala" % "shared" % Versions.shared

  // Dependency Modules
  private lazy val baseDependencies: Seq[ModuleID] = Seq(zio, shared)
  private lazy val doobieDependencies: Seq[ModuleID] = Seq(doobiePostgres, doobieHikari, flyway)

  // Project Dependencies
  lazy val coreDependencies: Seq[ModuleID] =
    baseDependencies ++ Seq(mercuryProtocolConnection)
  lazy val sqlDoobieDependencies: Seq[ModuleID] = baseDependencies ++ doobieDependencies ++ Seq(zioCatsInterop)
}
