import Dependencies._
lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "se.deltaprojects",
      scalaVersion := "2.12.6",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "lagom-opentracing",
    libraryDependencies ++= Seq(
      scalaTest % Test,
      "com.lightbend.lagom" %% "lagom-scaladsl-api" % "1.4.7",
      "com.lightbend.lagom" %% "lagom-scaladsl-server" % "1.4.7",
      "io.opentracing" % "opentracing-api" % "0.31.0",
      "io.opentracing" % "opentracing-util" % "0.31.0"
    )
  )
