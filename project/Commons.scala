import sbt._
import Keys._
import sbtassembly.AssemblyPlugin.autoImport.assembly

object Commons {
  val appVersion = "2.0.0-M4-public"
  val appScalaVersion = "2.12.11"

  val settings: Def.SettingsDefinition = Def.settings(
    Global / cancelable := true,
    version := appVersion,
    scalaVersion := appScalaVersion,
    resolvers ++= Seq(
      Resolver.mavenLocal
    ),
    assembly / test := {},
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-encoding", "utf8",
      //"-Xlint", // TODO: disabled as it looks like there is a bug in scalac: https://issues.scala-lang.org/browse/SI-9211
      "-language:higherKinds",
      "-language:implicitConversions",
      "-Xfuture",
      //"-Yinline-warnings", // TODO: seems to be not available in scala 2.12 and also not important
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-unused-import",
      //"-Ywarn-infer-any", // TODO: might enable, may related to -Xlint
      //"-Ywarn-value-discard", // TODO: this should be enabled
      "-Xfatal-warnings",
    )
  )
}
