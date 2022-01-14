import sbt._

object Dependencies {
  val scalaVersion = "2.12.15"

  val scalafixOrganizeImportsVersion = "0.5.0"

  val coreASMVersion = "1.7.3-locke-5"
  val coreASMEngine = "de.athalis.coreasm" % "coreasm-engine"   % coreASMVersion
  val coreASMCarma  = "de.athalis.coreasm" % "coreasm-ui-carma" % coreASMVersion

  val slf4jVersion = "1.7.32"
  val slf4j = "org.slf4j" % "slf4j-api" % slf4jVersion

  val logbackVersion = "1.2.10"
  val logback = "ch.qos.logback" % "logback-classic" % logbackVersion

  val akkaVersion = "2.6.18"
  val akkaActor  = "com.typesafe.akka" %% "akka-actor"  % akkaVersion
  val akkaRemote = "com.typesafe.akka" %% "akka-remote" % akkaVersion
  val akkaSlf4j  = "com.typesafe.akka" %% "akka-slf4j"  % akkaVersion

  val typesafeConfigVersion = "1.4.1"
  val typesafeConfig = "com.typesafe" % "config" % typesafeConfigVersion

  val scalaTestVersion = "3.2.10"
  val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion

  val jparsecVersion = "3.1"
  val jparsec = "org.jparsec" % "jparsec" % jparsecVersion

  // 3.x just because it is most recent, 3.16.0 was fine
  // >= 3.17.0 for support of `run` inside sbt https://github.com/jline/jline3/commit/fdc2fb53f9dc618bfccc3b20ae447cabce3a809f
  val jlineVersion = "3.21.0"
  val jline = "org.jline" % "jline" % jlineVersion

  // JLine 3.21.0 depends on 2.4.0
  val jAnsiVersion = "2.4.0"
  val jAnsi = "org.fusesource.jansi" % "jansi" % jAnsiVersion

  val asyncVersion = "1.0.1"
  val async = "org.scala-lang.modules" %% "scala-async" % asyncVersion

  val scalaXmlVersion = "2.0.1"
  val scalaXml = "org.scala-lang.modules" %% "scala-xml" % scalaXmlVersion

  val scalaReflect = "org.scala-lang" % "scala-reflect" % scalaVersion


  val PASSProcessModelDependencies: Seq[ModuleID] = Seq(
    scalaTest % Test
  )

  val PASSProcessModelOperationDependencies: Seq[ModuleID] = Seq(
    scalaTest % Test
  )

  val PASSProcessModelTUDarmstadtDependencies: Seq[ModuleID] = Seq(
    scalaTest % Test
  )

  val PASSProcessUtilBaseDependencies: Seq[ModuleID] = Seq.empty

  val PASSProcessModelInterfaceDependencies: Seq[ModuleID] = Seq(
    slf4j,
    logback, // required to enforce logging level in ConsoleUtil
    scalaTest % Test
  )

  val PASSProcessModelParserASTDependencies: Seq[ModuleID] = Seq(
    slf4j,
    jparsec,
    logback % Test,
    scalaTest % Test
  )

  val PASSProcessModelParserGraphMLDependencies: Seq[ModuleID] = Seq(
    typesafeConfig,
    slf4j,
    jparsec,
    scalaReflect,
    scalaXml,
    logback % Test,
    scalaTest % Test
  )

  val PASSProcessModelWriterASMDependencies: Seq[ModuleID] = Seq(
    slf4j,
    scalaTest % Test
  )

  val asmDependencies: Seq[ModuleID] = Seq(
    coreASMEngine % Test,
    coreASMEngine % Test classifier "tests",
    scalaTest % Test,
    coreASMCarma  % Provided notTransitive() // needed somewhere to get carmaLibraryPath for release
  )


  val CoreASMBaseDependencies: Seq[ModuleID] = Seq(
  )

  val CoreASMHelperDependencies: Seq[ModuleID] = Seq(
    coreASMEngine % Provided,
    coreASMEngine % Test classifier "tests",
    scalaTest % Test
  )

  val CoreASMBindingDependencies: Seq[ModuleID] = Seq(
  )

  val CoreASMBindingAkkaDependencies: Seq[ModuleID] = Seq(
    akkaActor % Provided
  )


  val AkkaStorageLibDependencies: Seq[ModuleID] = Seq(
  )

  val AkkaStoragePluginDependencies: Seq[ModuleID] = Seq(
    akkaActor,
    slf4j,
    akkaRemote % Runtime,
    akkaSlf4j % Runtime,
    typesafeConfig,
    coreASMEngine % Provided,
    scalaTest     % Test
  )


  val ASMSemanticDependencies: Seq[ModuleID] = Seq(
  )


  val PASSInterpreterConsoleDependencies: Seq[ModuleID] = Seq(
    akkaActor,
    akkaRemote % Runtime,
    akkaSlf4j % Runtime,
    logback % Runtime,
    typesafeConfig,
    jAnsi, // needed for support on Windows
    jline,
    async,
    scalaTest % Test,
    scalaReflect % Provided // workaround for https://github.com/scala/scala-async/issues/220
  )
}
