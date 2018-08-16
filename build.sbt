import Dependencies._
import sbt.Keys.publishArtifact
lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "se.deltaprojects",
      scalaVersion := "2.12.6",
      version      := "0.1.0-SNAPSHOT",
      description := "OpenTracing helpers for the Lagom Framework"
    )),
    licenses := Seq("MIT No Attribution" → url("https://github.com/aws/mit-0")),
    homepage := Some(url("https://deltaprojects.com")),
    name := "lagom-opentracing",
    crossScalaVersions := Seq("2.11.12", "2.12.6"),
    libraryDependencies ++= Seq(
      scalaTest % Test,
      "com.lightbend.lagom" %% "lagom-scaladsl-api" % "1.4.7",
      "com.lightbend.lagom" %% "lagom-scaladsl-server" % "1.4.7",
      "io.opentracing" % "opentracing-api" % "0.31.0",
      "io.opentracing" % "opentracing-util" % "0.31.0"
    ),
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := (_ ⇒ false),
    publishTo := Some(
      if (version.value.trim.endsWith("SNAPSHOT"))
        "snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
      else
        "releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    ),
    startYear := Some(2018),
    organizationHomepage := Some(url("https://github.com/deltaprojects")),
    developers := Developer("jonaslan", "Jonas Lantto", "", url("https://github.com/jonaslan")) :: Nil,
    scmInfo := Some(
      ScmInfo(
        browseUrl = url("https://github.com/deltaprojects/lagom-opentracing.git"),
        connection = "scm:git:git@github.com:deltaprojects/lagom-opentracing.git"
      )
    )
  )
