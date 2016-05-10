import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd, CmdLike}

name := """glimpse-tagger"""

version := "0.0.1"

scalaVersion := "2.11.7"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-cluster" % "2.4.4",
    "com.typesafe.akka" %% "akka-stream" % "2.4.4"
    ),
    dockerExposedPorts := Seq(9000),
    dockerEntrypoint := Seq("sh", "-c", "bin/glimpse-tagger $*"),
    dockerRepository := Some("docker"),
    dockerBaseImage := "shoplane/baseimage",
    dockerCommands ++= Seq(
      Cmd("USER", "root")
    )
  )

scalacOptions ++= Seq("-feature",  "-language:postfixOps", "-language:reflectiveCalls")

routesGenerator := InjectedRoutesGenerator