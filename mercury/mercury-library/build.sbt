val SCALA_VERSION = sys.env.get("SBT_SCOVERAGE") match {
  case None    => "3.2.1"
  case Some(_) => "3.2.2-RC1-bin-20221026-a210b7f-NIGHTLY" // Needed for sbt-scoverage
}

inThisBuild(
  Seq(
    organization := "io.iohk.atala",
    scalaVersion := SCALA_VERSION,
    fork := true,
    run / connectInput := true,
    releaseUseGlobalVersion := false,
    versionScheme := Some("semver-spec"),
    githubOwner := "input-output-hk",
    githubRepository := "atala-prism-building-blocks",
  )
)

ThisBuild / resolvers += Resolver.githubPackages("FabioPinheiro", "scala-did")

val useDidLib = false
def didScalaAUX =
  if (useDidLib) (libraryDependencies += D.didScala.value)
  else (libraryDependencies ++= Seq())

lazy val V = new {
  val munit = "1.0.0-M6" // "0.7.29"
  val munitZio = "0.1.1"

  // https://mvnrepository.com/artifact/dev.zio/zio
  val zio = "2.0.2"
  val zioLogging = "2.0.0"
  val zioJson = "0.3.0"
  val zioHttp = "2.0.0-RC11"

  // https://mvnrepository.com/artifact/io.circe/circe-core
  val circe = "0.14.2"

  val tapir = "1.0.3"
}

/** Dependencies */
lazy val D = new {
  val zio = Def.setting("dev.zio" %% "zio" % V.zio)
  val zioStreams = Def.setting("dev.zio" %% "zio-streams" % V.zio)
  val zioLog = Def.setting("dev.zio" %% "zio-logging" % V.zioLogging)
  val zioSLF4J = Def.setting("dev.zio" %% "zio-logging-slf4j" % V.zioLogging)
  val zioJson = Def.setting("dev.zio" %% "zio-json" % V.zioJson)

  // TODO waiting for https://github.com/zio/zio-http/pull/1774 to use zio-http
  // val zioHttp = Def.setting("dev.zio" %% "zio-http" % "0.0.1") // FIXME USE THIS ONE
  val zioHttp = Def.setting("io.d11" %% "zhttp" % V.zioHttp) // REMOVE (this is the old name)

  val circeCore = Def.setting("io.circe" %% "circe-core" % V.circe)
  val circeGeneric = Def.setting("io.circe" %% "circe-generic" % V.circe)
  val circeParser = Def.setting("io.circe" %% "circe-parser" % V.circe)

  // Test DID comm
  val didcommx = Def.setting("org.didcommx" % "didcomm" % "0.3.1")
  val peerDidcommx = Def.setting("org.didcommx" % "peerdid" % "0.3.0")
  val didScala = Def.setting("app.fmgp" %% "did" % "0.0.0+113-61efa271-SNAPSHOT")

  val jwk = Def.setting("com.nimbusds" % "nimbus-jose-jwt" % "9.25.4")

  // For munit https://scalameta.org/munit/docs/getting-started.html#scalajs-setup
  val munit = Def.setting("org.scalameta" %% "munit" % V.munit % Test)
  // For munit zio https://github.com/poslegm/munit-zio
  val munitZio = Def.setting("com.github.poslegm" %% "munit-zio" % V.munitZio % Test)

}

publish / skip := true

// #########################
// ### Models & Services ###
// #########################

/** Just data models and interfaces of service.
  *
  * This module must not depend on external libraries!
  */
lazy val models = project
  .in(file("models"))
  .settings(name := "mercury-data-models")
  .settings(
    libraryDependencies ++= Seq(D.zio.value),
    libraryDependencies ++= Seq(
      D.circeCore.value,
      D.circeGeneric.value,
      D.circeParser.value
    ), // TODO try to remove this from this module
    // libraryDependencies += D.didScala.value
  )

// #################
// ### Protocols ###
// #################

lazy val protocolConnection = project
  .in(file("protocol-connection"))
  .settings(name := "mercury-protocol-connection")
  .settings(libraryDependencies += D.zio.value)
  .settings(libraryDependencies ++= Seq(D.circeCore.value, D.circeGeneric.value, D.circeParser.value))
  .dependsOn(models, protocolInvitation)

lazy val protocolCoordinateMediation = project
  .in(file("protocol-coordinate-mediation"))
  .settings(name := "mercury-protocol-coordinate-mediation")
  .settings(libraryDependencies += D.zio.value)
  .settings(libraryDependencies ++= Seq(D.circeCore.value, D.circeGeneric.value, D.circeParser.value))
  .settings(libraryDependencies += D.munitZio.value)
  .dependsOn(models)

lazy val protocolDidExchange = project
  .in(file("protocol-did-exchange"))
  .settings(name := "mercury-protocol-did-exchange")
  .settings(libraryDependencies += D.zio.value)
  .settings(libraryDependencies ++= Seq(D.circeCore.value, D.circeGeneric.value, D.circeParser.value))
  .dependsOn(models, protocolInvitation)

lazy val protocolInvitation = project
  .in(file("protocol-invitation"))
  .settings(name := "mercury-protocol-invitation")
  .settings(libraryDependencies += D.zio.value)
  .settings(
    libraryDependencies ++= Seq(
      D.circeCore.value,
      D.circeGeneric.value,
      D.circeParser.value,
      D.munit.value,
      D.munitZio.value
    )
  )
  .dependsOn(models)

lazy val protocolMercuryMailbox = project
  .in(file("protocol-mercury-mailbox"))
  .settings(name := "mercury-protocol-mailbox")
  .settings(libraryDependencies += D.zio.value)
  .dependsOn(models, protocolInvitation, protocolRouting)

lazy val protocolLogin = project
  .in(file("protocol-outofband-login"))
  .settings(name := "mercury-protocol-outofband-login")
  .settings(libraryDependencies += D.zio.value)
  .settings(libraryDependencies += D.zio.value)
  .settings(libraryDependencies ++= Seq(D.circeCore.value, D.circeGeneric.value, D.circeParser.value))
  .settings(libraryDependencies += D.munitZio.value)
  .dependsOn(models)

lazy val protocolReportProblem = project
  .in(file("protocol-report-problem"))
  .settings(name := "mercury-protocol-report-problem")
  .dependsOn(models)

lazy val protocolRouting = project
  .in(file("protocol-routing"))
  .settings(name := "mercury-protocol-routing-2-0")
  .settings(libraryDependencies += D.zio.value)
  .dependsOn(models)

lazy val protocolIssueCredential = project
  .in(file("protocol-issue-credential"))
  .settings(name := "mercury-protocol-issue-credential")
  .settings(libraryDependencies += D.zio.value)
  .settings(libraryDependencies ++= Seq(D.circeCore.value, D.circeGeneric.value, D.circeParser.value))
  .settings(libraryDependencies += D.munitZio.value)
  .dependsOn(models)

lazy val protocolPresentProof = project
  .in(file("protocol-present-proof"))
  .settings(name := "mercury-protocol-present-proof")
  .settings(libraryDependencies += D.zio.value)
  .settings(libraryDependencies ++= Seq(D.circeCore.value, D.circeGeneric.value, D.circeParser.value))
  .settings(libraryDependencies += D.munitZio.value)
  .dependsOn(models)

// ################
// ### Resolver ###
// ################

// TODO move stuff to the models module
lazy val resolver = project // maybe merge into models
  .in(file("resolver"))
  .settings(name := "mercury-resolver")
  .settings(
    libraryDependencies ++= Seq(
      D.didcommx.value,
      D.peerDidcommx.value,
      D.munit.value,
      D.munitZio.value,
      D.jwk.value,
      "org.jetbrains.kotlin" % "kotlin-runtime" % "1.2.71",
      "org.jetbrains.kotlin" % "kotlin-stdlib" % "1.7.10",
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(models)

// ##############
// ### Agents ###
// ##############

lazy val agent = project // maybe merge into models
  .in(file("agent"))
  .settings(name := "mercury-agent-core")
  .settings(libraryDependencies += "com.google.zxing" % "core" % "3.5.0")
  .settings(libraryDependencies ++= Seq(D.zioLog.value)) // , D.zioSLF4J.value))
  .dependsOn(
    models,
    resolver,
    protocolCoordinateMediation,
    protocolInvitation,
    protocolRouting,
    protocolMercuryMailbox,
    protocolLogin,
    protocolIssueCredential,
    protocolPresentProof,
  )

/** agents implementation with didcommx */
lazy val agentDidcommx = project
  .in(file("agent-didcommx"))
  .settings(name := "mercury-agent-didcommx")
  .settings(libraryDependencies += D.didcommx.value)
  .settings(libraryDependencies += D.munitZio.value)
  .dependsOn(agent)

/** Demos agents and services implementation with didcommx */
lazy val agentCliDidcommx = project
  .in(file("agent-cli-didcommx"))
  .settings(name := "mercury-agent-cli-didcommx")
  .settings(libraryDependencies += D.zioHttp.value)
  .dependsOn(agentDidcommx)

///** TODO Demos agents and services implementation with did-scala */
lazy val agentDidScala =
  project
    .in(file("agent-did-scala"))
    .settings(name := "mercury-agent-didscala")
    .settings(
      skip / publish := true,
      didScalaAUX,
      if (useDidLib) (Compile / sources ++= Seq())
      else (Compile / sources := Seq()),
    )
    .dependsOn(agent)

// ### ReleaseStep ###
sys.env
  .get("SBT_SCOVERAGE") // SEE also plugin.sbt
  .map { _ =>
    println("### Config sbt-scoverage (releaseProcess) ###")
  import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    publishArtifacts,
    setNextVersion
  )
  }
  .toSeq
