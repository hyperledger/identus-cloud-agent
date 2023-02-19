addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("com.codecommit" % "sbt-github-packages" % "0.5.3")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.9")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
//addDependencyTreePlugin

libraryDependencies ++= Seq("org.openapitools" % "openapi-generator" % "6.3.0")
