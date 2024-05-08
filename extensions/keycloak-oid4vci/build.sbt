ThisBuild / organization     := "io.iohk.atala"
ThisBuild / organizationName := "Input Output Global"
ThisBuild / autoScalaLibrary := false
ThisBuild / crossPaths       := false

val V = new {
  val keycloak = "23.0.7"
}

lazy val root = (project in file("."))
  .settings(
    name := "keycloak-oea-oidc",
    libraryDependencies ++= Seq(
      "org.keycloak" % "keycloak-core" % V.keycloak % "provided",
      "org.keycloak" % "keycloak-common" % V.keycloak % "provided",
      "org.keycloak" % "keycloak-adapter-core" % V.keycloak % "provided",
      "org.keycloak" % "keycloak-saml-core" % V.keycloak % "provided",
      "org.keycloak" % "keycloak-saml-core-public" % V.keycloak % "provided",
      "org.keycloak" % "keycloak-server-spi" % V.keycloak % "provided",
      "org.keycloak" % "keycloak-server-spi-private" % V.keycloak % "provided",
      "org.keycloak" % "keycloak-services" % V.keycloak % "provided",
    )
  )
