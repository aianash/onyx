import sbt._
import sbt.Classpaths.publishTask
import Keys._

import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.{ MultiJvm, extraOptions, jvmOptions, scalatestOptions, multiNodeExecuteTests, multiNodeJavaName, multiNodeHostsFileName, multiNodeTargetDirName, multiTestOptions }
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging

import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

// import com.typesafe.sbt.SbtStartScript

import sbtassembly.AssemblyPlugin.autoImport._

import org.apache.maven.artifact.handler.DefaultArtifactHandler

import com.typesafe.sbt.SbtNativePackager._, autoImport._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd, CmdLike}

import com.goshoplane.sbt.standard.libraries.StandardLibraries


object OnyxBuild extends Build with StandardLibraries {

  lazy val makeScript = TaskKey[Unit]("make-script", "make script in local directory to run main classes")

  def sharedSettings = Seq(
    organization := "com.goshoplane",
    version := "0.1.0",
    scalaVersion := Version.scala,
    crossScalaVersions := Seq(Version.scala, "2.10.4"),
    scalacOptions := Seq("-unchecked", "-optimize", "-deprecation", "-feature", "-language:higherKinds", "-language:implicitConversions", "-language:postfixOps", "-language:reflectiveCalls", "-Yinline-warnings", "-encoding", "utf8"),
    retrieveManaged := true,

    fork := true,
    javaOptions += "-Xmx2500M",

    resolvers ++= StandardResolvers,

    publishMavenStyle := true
  ) ++ net.virtualvoid.sbt.graph.Plugin.graphSettings

  lazy val onyx = Project(
    id = "onyx",
    base = file("."),
    settings = Project.defaultSettings ++
      sharedSettings
  ) aggregate (core, datasetGenerators, queryModelling)

  lazy val core = Project(
    id = "onyx-core",
    base = file("core"),
    settings = Project.defaultSettings ++ sharedSettings
  ).settings(
    name := "onyx-core",

    libraryDependencies ++= Seq(
      "jline" % "jline" % "2.12.1",
      "com.goshoplane" %% "hemingway-dictionary" % "0.1.0"
    ) ++ Libs.mapdb
  )

  lazy val datasetGenerators = Project(
    id = "onyx-dataset-generators",
    base = file("dataset-generators"),
    settings = Project.defaultSettings ++ sharedSettings
  ).enablePlugins(JavaAppPackaging)
  .settings(
    name := "onyx-dataset-generators",

    libraryDependencies ++= Seq(
      "com.goshoplane" %% "hemingway-dictionary" % "0.1.0",
      "com.goshoplane" %% "creed-query-models" % "1.0.0",
      "edu.stanford.nlp" % "stanford-corenlp" % "3.5.2",
      "edu.stanford.nlp" % "stanford-corenlp" % "3.5.2" classifier "models"
    ) ++ Libs.lucene
      ++ Libs.fastutil
      ++ Libs.scallop
      ++ Libs.playJson
      ++ Libs.mapdb,

    mainClass in Compile := Some("onyx.datasets.generators.ALTStyleItemDatasetGenerator"),

    makeScript <<= (stage in Universal, stagingDirectory in Universal, baseDirectory in ThisBuild, streams) map { (_, dir, cwd, streams) =>
      var path = dir / "bin" / "onyx-dataset-generators"
      sbt.Process(Seq("ln", "-sf", path.toString, "onyx-dataset-generators"), cwd) ! streams.log
    }
  ) dependsOn(core)

  lazy val queryModelling = Project(
    id = "onyx-querymodelling",
    base = file("querymodelling"),
    settings = Project.defaultSettings ++ sharedSettings
  ).enablePlugins(JavaAppPackaging)
  .settings(
    name := "onyx-querymodelling",

    libraryDependencies ++= Seq(
      "com.goshoplane" %% "hemingway-dictionary" % "0.1.0"
    ) ++ Libs.lucene
      ++ Libs.fastutil
      ++ Libs.scallop
      ++ Libs.playJson
      ++ Libs.mapdb,

    makeScript <<= (stage in Universal, stagingDirectory in Universal, baseDirectory in ThisBuild, streams) map { (_, dir, cwd, streams) =>
      var path = dir / "bin" / "onyx-querymodelling"
      sbt.Process(Seq("ln", "-sf", path.toString, "onyx-querymodelling"), cwd) ! streams.log
    }
  ) dependsOn(core)

}