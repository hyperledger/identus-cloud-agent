addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
addSbtPlugin("com.codecommit" % "sbt-github-packages" % "0.5.3")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.7")

libraryDependencies ++= Seq("com.thesamet.scalapb" %% "compilerplugin" % "0.11.12")
