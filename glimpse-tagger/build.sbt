import com.typesafe.sbt.packager.archetypes.JavaAppPackaging

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
    )
  )

scalacOptions ++= Seq("-feature",  "-language:postfixOps", "-language:reflectiveCalls")

routesGenerator := InjectedRoutesGenerator