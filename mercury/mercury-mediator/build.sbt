import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

inThisBuild(
  Seq(
    organization := "io.iohk.atala",
    scalaVersion := "3.2.1",
    fork := true,
    run / connectInput := true,
    versionScheme := Some("semver-spec"),
    githubOwner := "input-output-hk",
    githubRepository := "atala-prism-building-blocks",
    githubTokenSource := TokenSource.Environment("ATALA_GITHUB_TOKEN")
  )
)

// Custom keys
val apiBaseDirectory =
  settingKey[File]("The base directory for Castor API specifications")
ThisBuild / apiBaseDirectory := baseDirectory.value / "api"
ThisBuild / resolvers += Resolver.githubPackages("input-output-hk", "atala-prism-building-blocks")

lazy val V = new {
  val munit = "1.0.0-M6" // "0.7.29"
  val munitZio = "0.1.1"

  // https://mvnrepository.com/artifact/dev.zio/zio
  val zio = "2.0.9"
  val zioLogging = "2.0.0"
  val zioJson = "0.4.2"
  val zioHttp = "0.0.3" // "2.0.0-RC10" // "2.0.0-RC11" TODO

  // https://mvnrepository.com/artifact/io.circe/circe-core
  val circe = "0.14.2"

  val mercury = "0.19.0"
}

/** Dependencies */
lazy val D = new {

  val mercuryModels = Def.setting("io.iohk.atala" %% "mercury-data-models" % V.mercury)
  val mercuryAgent = Def.setting("io.iohk.atala" %% "mercury-agent-didcommx" % V.mercury)
  val mercuryResolver = Def.setting("io.iohk.atala" %% "mercury-resolver" % V.mercury)
  // Def.setting("io.iohk.atala" %% "mercury-agent-didscala" % V.mercury)
  // Def.setting("io.iohk.atala" %% "mercury-agent-core" % V.mercury)
  // Def.setting("io.iohk.atala" %% "mercury-protocol-outofband-login" % V.mercury)
  // Def.setting("io.iohk.atala" %% "mercury-protocol-routing-2_0" % V.mercury)
  // Def.setting("io.iohk.atala" %% "mercury-protocol-coordinate-mediation" % V.mercury)
  // Def.setting("io.iohk.atala" %% "mercury-protocol-invitation" % V.mercury)
  // Def.setting("io.iohk.atala" %% "mercury-protocol-did-exchange" % V.mercury)
  // Def.setting("io.iohk.atala" %% "mercury-protocol-mailbox" % V.mercury)
  // Def.setting("io.iohk.atala" %% "mercury-protocol-connection" % V.mercury)

  val zio = Def.setting("dev.zio" %% "zio" % V.zio)
  val zioStreams = Def.setting("dev.zio" %% "zio-streams" % V.zio)
  val zioLog = Def.setting("dev.zio" %% "zio-logging" % V.zioLogging)
  val zioSLF4J = Def.setting("dev.zio" %% "zio-logging-slf4j" % V.zioLogging)
  val zioJson = Def.setting("dev.zio" %% "zio-json" % V.zioJson)

  val zioHttp = Def.setting("dev.zio" %% "zio-http" % V.zioHttp) // FIXME USE THIS ONE
  // val zioHttp = Def.setting("io.d11" %% "zhttp" % V.zioHttp) // REMOVE (this is the old name)

  val circeCore = Def.setting("io.circe" %% "circe-core" % V.circe)
  val circeGeneric = Def.setting("io.circe" %% "circe-generic" % V.circe)
  val circeParser = Def.setting("io.circe" %% "circe-parser" % V.circe)

  // For munit https://scalameta.org/munit/docs/getting-started.html#scalajs-setup
  val munit = Def.setting("org.scalameta" %% "munit" % V.munit % Test)
  // For munit zio https://github.com/poslegm/munit-zio
  val munitZio = Def.setting("com.github.poslegm" %% "munit-zio" % V.munitZio % Test)

}

// ################
// ### Mediator ###
// ################

/** The mediator service */
lazy val mediator = project
  .in(file("."))
  .settings(name := "mercury-mediator")
  .settings(libraryDependencies += D.zio.value)
  .settings(libraryDependencies += D.munitZio.value)
  .settings(
    libraryDependencies ++= Seq(D.mercuryModels.value, D.mercuryAgent.value, D.zioHttp.value),
    Compile / unmanagedResourceDirectories += apiBaseDirectory.value,
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    githubTokenSource := TokenSource.Environment("ATALA_GITHUB_TOKEN"),
    // ### Build Docker Image ###
    Docker / maintainer := "atala-coredid@iohk.io",
    Docker / dockerRepository := Some("ghcr.io"),
    Docker / dockerUsername := Some("input-output-hk"),
    Docker / githubOwner := "atala-prism-building-blocks",
    Docker / dockerUpdateLatest := true,
    dockerExposedPorts := Seq(8080),
    dockerBaseImage := "openjdk:11"
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin)

// ### ReleaseStep ###
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  ReleaseStep(releaseStepTask(Docker / publish)),
  setNextVersion
)
