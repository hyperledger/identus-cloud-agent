val tapirVersion = "1.0.1"
val VERSION = "0.1.0-SNAPSHOT"

inThisBuild(
  Seq(
    organization := "io.iohk.atala",
    scalaVersion := "3.1.3"
  )
)

lazy val V = new {
  // FIXME another bug in the test framework https://github.com/scalameta/munit/issues/554
  val munit = "1.0.0-M6" // "0.7.29"

  // https://mvnrepository.com/artifact/dev.zio/zio
  val zio = "2.0.0"
  val zioJson = "0.3.0-RC10"
}

/** Dependencies */
lazy val D = new {
  val zio = Def.setting("dev.zio" %% "zio" % V.zio)
  val zioStreams = Def.setting("dev.zio" %% "zio-streams" % V.zio)
  val zioJson = Def.setting("dev.zio" %% "zio-json" % V.zioJson)

  // Test DID comm
  val didcomm = Def.setting("org.didcommx" % "didcomm" % "0.3.1")

  // For munit https://scalameta.org/munit/docs/getting-started.html#scalajs-setup
  val munit = Def.setting("org.scalameta" %% "munit" % V.munit % Test)
}

// lazy val mediator = project
//   .in(file("mediator"))
//   .settings(name := "mercury-mediator", version := VERSION)
//   .settings(
//     libraryDependencies ++= Seq(
//       "com.softwaremill.sttp.tapir" %% "tapir-http4s-server-zio" % tapirVersion,
//       "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
//       "org.http4s" %% "http4s-blaze-server" % "0.23.12",
//       "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
//       "ch.qos.logback" % "logback-classic" % "1.2.11",
//       "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion % Test,
//       "dev.zio" %% "zio-test" % "2.0.0" % Test,
//       "dev.zio" %% "zio-test-sbt" % "2.0.0" % Test,
//       "com.softwaremill.sttp.client3" %% "circe" % "3.7.1" % Test,
//       "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % tapirVersion,
//       "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % "1.0.0-M9",
//       "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui" % tapirVersion,
//       "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s" % "0.19.0-M1",
//       "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml" % "0.2.1",
//       D.didcomm.value,
//       "org.jetbrains.kotlin" % "kotlin-runtime" % "1.2.71",
//       "org.jetbrains.kotlin" % "kotlin-stdlib" % "1.7.10",
//       "com.google.code.gson" % "gson" % "2.9.1"
//     ),
//     testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
//   )

lazy val resolver = project
  .in(file("resolver"))
  .settings(name := "mercury-resolver", version := VERSION)
  .settings(
    libraryDependencies ++= Seq(
      D.didcomm.value,
      "org.jetbrains.kotlin" % "kotlin-runtime" % "1.2.71",
      "org.jetbrains.kotlin" % "kotlin-stdlib" % "1.7.10",
    )
  )

lazy val agents = project
  .in(file("agent"))
  .settings(name := "mercury-agent", version := VERSION)
  .settings(
    libraryDependencies ++= Seq(D.zio.value, D.didcomm.value)
  )
  .dependsOn(resolver)
