import sbt._

object Dependencies {
  val jdkVersion_2_12_target  =  "8" // Unlike Scala 2.13, Scala 2.12 cannot emit valid class files for target bytecode versions newer than 8
  val jdkVersion_2_12_release = "11"
  val jdkVersion_2_13         = "17"
  val jdkVersion_3            = "17"

  val scalaVersion_2_12 = "2.12.18"
  val scalaVersion_2_13 = "2.13.12"
  val scalaVersion_3_1  = "3.1.3"
  val scalaVersion_3_2  = "3.2.2"
  val scalaVersion_3_3  = "3.3.1"
  // note: default to Scala 2.13
  // Akka has only experimental support for Scala 3
  val scalaVersions = List(
    scalaVersion_2_13,
    scalaVersion_3_1,
    scalaVersion_3_2,
    scalaVersion_3_3,
    scalaVersion_2_12,
  )
  val scalaVersion_default = scalaVersions.head

  val semanticdbVersion_2_12 = "4.8.11"
  val semanticdbVersion_2_13 = semanticdbVersion_2_12
  val semanticdbVersion_3    = semanticdbVersion_2_13
  def semanticdbVersion(scalaVersion: String): String = CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, n)) if n <= 12 => semanticdbVersion_2_12
    case Some((2, n)) if n == 13 => semanticdbVersion_2_13
    case Some((3, _))            => semanticdbVersion_3
  }

  val scalafixOrganizeImportsVersion = "0.6.0"
  val scalafixOrganizeImports = "com.github.liancheng" %% "organize-imports" % scalafixOrganizeImportsVersion

  val coreASMVersion = "1.7.3-locke-6"
  val coreASMEngine = "de.athalis.coreasm" % "coreasm-engine"   % coreASMVersion
  val coreASMCarma  = "de.athalis.coreasm" % "coreasm-ui-carma" % coreASMVersion

  val slf4jVersion = "2.0.9"
  val slf4j = "org.slf4j" % "slf4j-api" % slf4jVersion

  // NOTE: "Logback 1.3.x supports the Java EE edition whereas logback 1.4.x supports Jakarta EE, otherwise the two versions are feature identical."
  val logbackVersion = "1.3.11"
  val logback = "ch.qos.logback" % "logback-classic" % logbackVersion

  // NOTE: Akka 2.7 has a new commercial license
  val akkaVersion = "2.6.21"
  val akkaActor  = "com.typesafe.akka" %% "akka-actor"  % akkaVersion
  val akkaRemote = "com.typesafe.akka" %% "akka-remote" % akkaVersion
  val akkaSlf4j  = "com.typesafe.akka" %% "akka-slf4j"  % akkaVersion

  val typesafeConfigVersion = "1.4.2"
  val typesafeConfig = "com.typesafe" % "config" % typesafeConfigVersion

  val scalaTestVersion = "3.2.17"
  val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion

  val jparsecVersion = "3.1"
  val jparsec = "org.jparsec" % "jparsec" % jparsecVersion

  // 3.x just because it is most recent
  // 3.16.0 was fine, but then:
  // >= 3.17.0 for support of `run` inside sbt https://github.com/jline/jline3/commit/fdc2fb53f9dc618bfccc3b20ae447cabce3a809f
  // 3.21.0 and 3.22.0 were fine
  val jlineVersion = "3.23.0"
  val jline = "org.jline" % "jline" % jlineVersion

  // JLine 3.21.0, 3.22.0, and 3.23.0 depend on 2.4.0
  val jAnsiVersion = "2.4.0"
  val jAnsi = "org.fusesource.jansi" % "jansi" % jAnsiVersion

  val asyncVersion = "1.0.1"
  def async(scalaVersion: String): Seq[ModuleID] = CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, n)) if n <= 12 => Seq("org.scala-lang.modules" %% "scala-async" % asyncVersion)
    case Some((2, n)) if n == 13 => Seq("org.scala-lang.modules" %% "scala-async" % asyncVersion)
    case Some((3, _))            => Seq("com.github.rssh" %% "shim-scala-async-dotty-cps-async" % "0.9.8") // TODO: scala-async is not available for scala 3
  }

  val scalaXmlVersion_2_12 = "1.3.0" // old version needed for https://github.com/scoverage/sbt-scoverage/issues/439
  val scalaXmlVersion_2_13 = "2.1.0"
  val scalaXmlVersion_3    = scalaXmlVersion_2_13
  def scalaXml(scalaVersion: String): ModuleID = CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, n)) if n <= 12 => "org.scala-lang.modules" %% "scala-xml" % scalaXmlVersion_2_12
    case Some((2, n)) if n == 13 => "org.scala-lang.modules" %% "scala-xml" % scalaXmlVersion_2_13
    case Some((3, _))            => "org.scala-lang.modules" %% "scala-xml" % scalaXmlVersion_3
  }

  // 1.0.2 is most recent, but most likely incompatible with 0.x & akka 2.5.x and 2.6.x for scala 2.12 depend on 0.8.0
  val java8compatVersion_2_12 = "0.9.1"
  val java8compatVersion_2_13 = "1.0.2"
  val java8compatVersion_3    = java8compatVersion_2_13
  def java8compat(scalaVersion: String): Seq[ModuleID] = CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, n)) if n <= 12 => Seq("org.scala-lang.modules" %% "scala-java8-compat" % java8compatVersion_2_12)
    case Some((2, n)) if n == 13 => Seq("org.scala-lang.modules" %% "scala-java8-compat" % java8compatVersion_2_13)
    case Some((3, _))            => Seq("org.scala-lang.modules" %% "scala-java8-compat" % java8compatVersion_3)
  }

  val scalaParallelCollectionsVersion_2_13 = "1.0.4"
  val scalaParallelCollectionsVersion_3    = scalaParallelCollectionsVersion_2_13
  def scalaParallelCollections(scalaVersion: String): Seq[ModuleID] = CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, n)) if n <= 12 => Seq()
    case Some((2, n)) if n == 13 => Seq("org.scala-lang.modules" %% "scala-parallel-collections" % scalaParallelCollectionsVersion_2_13)
    case Some((3, _))            => Seq("org.scala-lang.modules" %% "scala-parallel-collections" % scalaParallelCollectionsVersion_3)
  }


  private val defaultTest: Seq[ModuleID] = Seq(
    scalaTest % Test,
    logback % Test,
  )

  val PASSProcessModelDependencies: Seq[ModuleID] = Seq(
  ) ++ defaultTest

  val PASSProcessModelOperationDependencies: Seq[ModuleID] = Seq(
    typesafeConfig,
  ) ++ defaultTest

  val PASSProcessModelTUDarmstadtDependencies: Seq[ModuleID] = Seq(
  ) ++ defaultTest

  def PASSProcessModelInterfaceDependencies(scalaVersion: String): Seq[ModuleID] = Seq(
    slf4j,
  ) ++ defaultTest ++
    scalaParallelCollections(scalaVersion)

  val PASSProcessModelInterfaceCLIDependencies: Seq[ModuleID] = Seq(
    slf4j,
    logback, // required to enforce logging level in ConsoleUtil
    jAnsi, // needed for support on Windows (CLI apps use logging which uses colored output (could be disabled though))
  )

  def PASSProcessModelParserASTDependencies(scalaVersion: String): Seq[ModuleID] = Seq(
    slf4j,
    jparsec,
  ) ++ defaultTest ++
    scalaParallelCollections(scalaVersion)

  def PASSProcessModelParserGraphMLDependencies(scalaVersion: String): Seq[ModuleID] = Seq(
    typesafeConfig,
    slf4j,
    jparsec,
    scalaXml(scalaVersion),
  ) ++ defaultTest ++
    scalaParallelCollections(scalaVersion)

  val PASSProcessModelWriterASMDependencies: Seq[ModuleID] = Seq(
    slf4j,
  ) ++ defaultTest

  val asmDependencies: Seq[ModuleID] = Seq(
    coreASMEngine % Test,
    coreASMEngine % Test classifier "tests",
    coreASMCarma  % Provided classifier "jar-with-dependencies" notTransitive() // needed somewhere to get carmaLibraryPath for release
  ) ++ defaultTest


  val CoreASMBaseDependencies: Seq[ModuleID] = Seq(
  )

  val CoreASMHelperDependencies: Seq[ModuleID] = Seq(
    coreASMEngine % Provided,
    coreASMEngine % Test classifier "tests",
  ) ++ defaultTest

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
  ) ++ defaultTest


  val ASMSemanticDependencies: Seq[ModuleID] = Seq(
  )


  def PASSInterpreterConsoleDependencies(scalaVersion: String): Seq[ModuleID] = Seq(
    akkaActor,
    akkaRemote % Runtime,
    akkaSlf4j % Runtime,
    logback % Runtime,
    typesafeConfig,
    jAnsi, // needed for support on Windows
    jline,
  ) ++ defaultTest ++
    async(scalaVersion) ++
    scalaParallelCollections(scalaVersion)

}
