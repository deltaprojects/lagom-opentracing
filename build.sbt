import Dependencies._
import sbt.Keys.publishArtifact
import ReleaseTransformations._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.deltaprojects",
      scalaVersion := "2.12.6",
      description := "OpenTracing helpers for the Lagom Framework"
    )),
    licenses := Seq("MIT-0" → url("https://github.com/aws/mit-0")),
    homepage := Some(url("https://deltaprojects.com")),
    name := "lagom-opentracing",
    crossScalaVersions := Seq("2.11.12", "2.12.6"),
    libraryDependencies ++= Seq(
      scalaTest % Test,
      "com.lightbend.lagom" %% "lagom-scaladsl-api" % "1.4.7" % "provided",
      "com.lightbend.lagom" %% "lagom-scaladsl-server" % "1.4.7" % "provided",
      "io.opentracing" % "opentracing-api" % "0.31.0" % "provided",
      "io.opentracing" % "opentracing-util" % "0.31.0" % "provided",
      "io.opentracing.contrib" %% "opentracing-scala-concurrent" % "0.0.4"
    ),
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      setNextVersion,
      commitNextVersion,
      pushChanges,
      releaseStepCommand("sonatypeRelease")
    ),
    publishMavenStyle := true,
    releaseCrossBuild := true,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    publishArtifact in Test := false,
    pomIncludeRepository := (_ ⇒ false),
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    startYear := Some(2018),
    organizationHomepage := Some(url("https://github.com/deltaprojects")),
    developers := Developer("jonaslan", "Jonas Lantto", "jonas.lantto@deltaprojects.com", url("https://github.com/jonaslan")) :: Nil,
    scmInfo := Some(
      ScmInfo(
        browseUrl = url("https://github.com/deltaprojects/lagom-opentracing.git"),
        connection = "scm:git:git@github.com:deltaprojects/lagom-opentracing.git"
      )
    )
  )
