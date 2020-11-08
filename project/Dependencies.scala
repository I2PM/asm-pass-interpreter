import sbt._

object Dependencies {
  val coreASMVersion = "1.7.3-locke-4"
  val coreASMEngine = "de.athalis.coreasm" % "coreasm-engine"   % coreASMVersion
  val coreASMCarma  = "de.athalis.coreasm" % "coreasm-ui-carma" % coreASMVersion

  // 1.7.30 is most recent, but 1.7.25 avoids most evictions (i.e. logback-classic and jsonld-java depend on it)
  val slf4jVersion = "1.7.25"
  val slf4j = "org.slf4j" % "slf4j-api" % slf4jVersion

  val logbackVersion = "1.2.3"
  val logback = "ch.qos.logback" % "logback-classic" % logbackVersion

  val akkaVersion = "2.5.31"
  val akkaActor  = "com.typesafe.akka" %% "akka-actor"  % akkaVersion
  val akkaRemote = "com.typesafe.akka" %% "akka-remote" % akkaVersion
  val akkaSlf4j  = "com.typesafe.akka" %% "akka-slf4j"  % akkaVersion

  // 1.4.0 is most recent, stayed at 1.3.3 which akka depends on
  val typesafeConfigVersion = "1.3.3"
  val typesafeConfig = "com.typesafe" % "config" % typesafeConfigVersion

  val scalaTestVersion = "3.2.2"
  val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion

  val jparsecVersion = "3.1"
  val jparsec = "org.jparsec" % "jparsec" % jparsecVersion

  val jAnsiVersion = "1.18"
  val jAnsi = "org.fusesource.jansi" % "jansi" % jAnsiVersion

  val jlineVersion = "3.16.0"
  val jline = "org.jline" % "jline" % jlineVersion

  val asyncVersion = "0.10.0"
  val async = "org.scala-lang.modules" %% "scala-async" % asyncVersion

  val scalaXmlVersion = "1.3.0"
  val scalaXml = "org.scala-lang.modules" %% "scala-xml" % scalaXmlVersion

  val scalaVersion = Commons.appScalaVersion
  val scalaReflect = "org.scala-lang" % "scala-reflect" % scalaVersion


  val PASSProcessModelDependencies: Seq[ModuleID] = Seq(
    scalaTest % "test"
  )

  val PASSProcessUtilBaseDependencies: Seq[ModuleID] = Seq.empty

  val PASSProcessUtilDependencies: Seq[ModuleID] = Seq(
    slf4j,
    logback, // required to enforce logging level in ConsoleUtil
    scalaTest % "test"
  )

  val PASSProcessParserASTDependencies: Seq[ModuleID] = Seq(
    slf4j,
    jparsec,
    scalaTest % "test"
  )

  val PASSProcessParserGraphMLDependencies: Seq[ModuleID] = Seq(
    typesafeConfig,
    slf4j,
    jparsec,
    scalaReflect,
    scalaXml,
    scalaTest % "test"
  )

  val PASSProcessWriterASMDependencies: Seq[ModuleID] = Seq(
    slf4j,
    scalaTest % "test"
  )

  val asmDependencies: Seq[ModuleID] = Seq(
    coreASMEngine % "test",
    coreASMEngine % "test" classifier "tests",
    scalaTest % "test",
    coreASMCarma  % "provided" notTransitive() // needed somewhere to get carmaLibraryPath for release
  )


  val CoreASMBaseDependencies: Seq[ModuleID] = Seq(
  )

  val CoreASMHelperDependencies: Seq[ModuleID] = Seq(
    coreASMEngine % "provided" notTransitive(),
    coreASMEngine % "test" classifier "tests",
    scalaTest % "test"
  )

  val CoreASMBindingDependencies: Seq[ModuleID] = Seq(
  )

  val CoreASMBindingAkkaDependencies: Seq[ModuleID] = Seq(
    akkaActor % "provided" notTransitive()
  )


  val AkkaStorageLibDependencies: Seq[ModuleID] = Seq(
  )

  val AkkaStoragePluginDependencies: Seq[ModuleID] = Seq(
    akkaActor,
    slf4j,
    akkaRemote, // required at runtime
    akkaSlf4j,  // required at runtime
    typesafeConfig,
    coreASMEngine % "provided" notTransitive(),
    scalaTest     % "test"
  )


  val ASMSemanticDependencies: Seq[ModuleID] = Seq(
  )


  val PASSInterpreterConsoleDependencies: Seq[ModuleID] = Seq(
    akkaActor,
    akkaRemote, // required at runtime
    akkaSlf4j,  // required at runtime
    typesafeConfig,
    jAnsi, // needed for support on Windows
    jline,
    async,
    scalaTest % "test",
    scalaReflect % "provided" notTransitive() // workaround for https://github.com/scala/scala-async/issues/220
  )
}
