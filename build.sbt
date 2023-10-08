import sbtassembly.AssemblyKeys.assemblyMergeStrategy
import sbtassembly.AssemblyPlugin.autoImport.{MergeStrategy, assembly}

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.FalseFileFilter

val projectRoot        = file(".").getAbsoluteFile
val releaseFolder      = projectRoot / "release"
val scriptsFolder      = projectRoot / "scripts"
val processesFolder    = projectRoot / "processes"

lazy val cleanRelease = taskKey[Unit]("cleans release folder")
lazy val prepareRelease = taskKey[File]("prepares all files for the release folder and returns it")
lazy val prepareCleanRelease = taskKey[File]("cleans release folder, then prepares all files for the release folder and returns it")
lazy val zipRelease = taskKey[File]("zips the release folder")

ThisBuild / version := "2.0.0-M8-public"
ThisBuild / versionScheme := Some(VersionScheme.Strict) // NOTE: as of now, semver (or at least PVP) is aimed, but cannot be guaranteed as "the public API" needs to be defined

ThisBuild / scalaVersion := Dependencies.scalaVersion_default

ThisBuild / packageTimestamp := Package.gitCommitDateTimestamp

ThisBuild / resolvers ++= Seq(
  Resolver.mavenLocal
)

ThisBuild / scalafixDependencies += Dependencies.scalafixOrganizeImports

val scalacOptionsAll = Seq(
  "-unchecked",
  "-feature",
  "-encoding", "utf8",
)

val scalacOptionsPrimary = Seq(
  "-deprecation",
  "-Xfatal-warnings",
)

val scalacOptions_2 = Seq(
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  //"-Ywarn-value-discard", // TODO: this should be enabled
  "-Xasync", // for scala-async 1.0.0
  // TODO: -Xsource:3 ?
)

val scalacOptions_2_12 = Seq(
  "-Ywarn-unused-import",
  "-Xlint:-unused", // TODO: all unused warnings should be enabled
  "-release", Dependencies.jdkVersion_2_12_release,
  "-target:" + Dependencies.jdkVersion_2_12_target,
)

val scalacOptions_2_13 = Seq(
  "-Xlint",
  "-release", Dependencies.jdkVersion_2_13, // implies -target
)

val scalacOptions_3 = Seq(
  "-release", Dependencies.jdkVersion_3, // implies -target
  // TODO: Xlint etc. are no longer / not yet supported
)


ThisBuild / assembly / assemblyMergeStrategy := {
  case x if x.endsWith("module-info.class") => MergeStrategy.discard
  case "application.conf" => MergeStrategy.discard
  case PathList(ps @ _*) if (ps.last.startsWith("logback") && ps.last.endsWith(".xml")) => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

lazy val defaultSettings: Seq[Def.Setting[_]] = Seq(

  // Forking is required only in certain cases; forking always to be safe.
  // - If not forked, and an execution runs with the same scala version as sbt, typesafe config will be scoped to the SBT process
  // - For asm/test fork is needed, as the CoreASM Engine writes to System.out and parallel processes would conflict
  fork := true,

  // Non-forked tasks execute in the root directory, but forked tasks execute in the module folder.
  // Change the baseDirectory to be always the root folder, to avoid this difference.
  // For `run` it would be unintuitive to access the `processes` folder from the parent directory,
  // for tests it would be surprising to have a different behaviour depending whether its forked opr not.
  run / baseDirectory := projectRoot,
  Test / baseDirectory := projectRoot,

  crossScalaVersions := Dependencies.scalaVersions,

  scalacOptions ++= {
    scalacOptionsAll ++ (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n <= 12 => scalacOptions_2 ++ scalacOptions_2_12 ++ scalacOptionsPrimary
      case Some((2, n)) if n == 13 => scalacOptions_2 ++ scalacOptions_2_13
      case Some((3, _))            => scalacOptions_3
    })
  },

  semanticdbEnabled := true,
  semanticdbVersion := Dependencies.semanticdbVersion(scalaVersion.value),

  scalafixConfig := (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, _)) => Some(file(".scalafix_2.conf"))
    case Some((3, _)) => Some(file(".scalafix_3.conf"))
  }),
)


lazy val root = (project in file("."))
  // run Tasks like test and compile on all projects
  .aggregate(
    PASSProcessModel,
    PASSProcessModelOperation,
    PASSProcessModelTUDarmstadt,
    PASSProcessModelParserAST,
    PASSProcessModelParserGraphML,
    PASSProcessModelWriterASM,
    PASSProcessModelInterface,
    PASSProcessModelInterfaceCLI,

    asm,

    CoreASMBase,
    CoreASMHelper,
    CoreASMBinding,
    CoreASMBindingAkka,

    AkkaStorageLib,
    AkkaStoragePlugin,

    ASMSemantic,
    PASSInterpreterConsole,
  )
  .settings(defaultSettings)
  .settings(
    crossScalaVersions := Nil, // crossScalaVersions must be set to Nil on the aggregating project
    publish / skip := true,    // also, the root project doesn't need to be published

    name := "asm-pass-interpreter",
    run := (PASSInterpreterConsole / Compile / run).evaluated, // overwrite run Task to use the run in PASSUI
  )

lazy val PASSProcessModel = (project in file("PASSProcessModel"))
  .enablePlugins(BuildInfoPlugin)
  .settings(defaultSettings)
  .settings(
    name := "PASS Process Model",
    libraryDependencies ++= Dependencies.PASSProcessModelDependencies,

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.athalis.pass.processmodel.info",
  )

lazy val PASSProcessModelOperation = (project in file("PASSProcessModelOperation"))
  .enablePlugins(BuildInfoPlugin)
  .settings(defaultSettings)
  .settings(
    name := "PASS Process Model Operation",
    libraryDependencies ++= Dependencies.PASSProcessModelOperationDependencies,

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.athalis.pass.processmodel.operation.info",
  )
  .dependsOn(PASSProcessModel)

lazy val PASSProcessModelTUDarmstadt = (project in file("PASSProcessModelTUDarmstadt"))
  .enablePlugins(BuildInfoPlugin)
  .settings(defaultSettings)
  .settings(
    name := "PASS Process Model TUDarmstadt",
    libraryDependencies ++= Dependencies.PASSProcessModelTUDarmstadtDependencies,

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.athalis.pass.processmodel.tudarmstadt.info",
  )
  .dependsOn(PASSProcessModel)


lazy val PASSProcessModelParserAST = (project in file("PASSProcessModelParserAST"))
  .settings(defaultSettings)
  .settings(
    name := "PASS Process Model Parser AST",
    libraryDependencies ++= Dependencies.PASSProcessModelParserASTDependencies(scalaVersion.value),
  )
  .dependsOn(PASSProcessModel)
  .dependsOn(PASSProcessModelOperation)
  .dependsOn(PASSProcessModelTUDarmstadt)

lazy val PASSProcessModelParserGraphML = (project in file("PASSProcessModelParserGraphML"))
  .settings(defaultSettings)
  .settings(
    name := "PASS Process Model Parser GraphML",
    libraryDependencies ++= Dependencies.PASSProcessModelParserGraphMLDependencies(scalaVersion.value),
  )
  .dependsOn(PASSProcessModel)
  .dependsOn(PASSProcessModelOperation)
  .dependsOn(PASSProcessModelTUDarmstadt)
  .dependsOn(PASSProcessModelParserAST)

lazy val PASSProcessModelWriterASM = (project in file("PASSProcessModelWriterASM"))
  .settings(defaultSettings)
  .settings(
    name := "PASS Process Model Writer ASM",
    libraryDependencies ++= Dependencies.PASSProcessModelWriterASMDependencies,
  )
  .dependsOn(PASSProcessModel)
  .dependsOn(PASSProcessModelOperation)
  .dependsOn(PASSProcessModelTUDarmstadt)


lazy val PASSProcessModelInterface = (project in file("PASSProcessModelInterface"))
  .settings(defaultSettings)
  .settings(
    name := "PASS Process Model Interface",
    libraryDependencies ++= Dependencies.PASSProcessModelInterfaceDependencies(scalaVersion.value),
  )
  .dependsOn(PASSProcessModel)
  .dependsOn(PASSProcessModelOperation)
  .dependsOn(PASSProcessModelTUDarmstadt % "test->test;compile->compile")
  .dependsOn(PASSProcessModelParserAST)
  .dependsOn(PASSProcessModelParserGraphML)
  .dependsOn(PASSProcessModelWriterASM)


lazy val PASSProcessModelInterfaceCLI = (project in file("PASSProcessModelInterfaceCLI"))
  .enablePlugins(BuildInfoPlugin)
  .settings(defaultSettings)
  .settings(
    name := "PASS Process Model Command-Line Interface",
    libraryDependencies ++= Dependencies.PASSProcessModelInterfaceCLIDependencies,

    parsePASSClass := "de.athalis.pass.processmodel.interface.cli.ConsoleConverter",

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.athalis.pass.processmodel.interface.cli",

    assembly / assemblyJarName := "pass-processmodel-cli.jar",

    Compile / mainClass := Some("de.athalis.pass.processmodel.interface.cli.ConsoleParser"),

    prepareRelease := {
      val assemblyFile: File = assembly.value
      FileUtils.copyFileToDirectory(assemblyFile, releaseFolder)

      val resources = (Compile / resourceDirectory).value
      FileUtils.copyDirectory(resources, releaseFolder)

      releaseFolder
    },
  )
  .enablePlugins(PASSProcessModelParserPlugin)
  .dependsOn(PASSProcessModel)
  .dependsOn(PASSProcessModelOperation)
  .dependsOn(PASSProcessModelInterface)



lazy val asm = (project in file("asm"))
  .settings(defaultSettings)
  .settings(
    name := "PASS ASM Semantic",
    libraryDependencies ++= Dependencies.asmDependencies,

    prepareRelease := {
      val asmFiles = (Compile / resourceDirectory).value
      val excludeNoReleaseFilter = new FileFilter {
        def accept(pathname: File): Boolean = !pathname.toString.contains("no-release")
      }
      FileUtils.copyDirectory(asmFiles, releaseFolder / "asm", excludeNoReleaseFilter)

      val carmaLibraryPath = update.value.select(module = moduleFilter(
        organization = Dependencies.coreASMCarma.organization,
        name = Dependencies.coreASMCarma.name,
        revision = Dependencies.coreASMCarma.revision)
      ).head
      FileUtils.copyFile(carmaLibraryPath, releaseFolder / "carma.jar")

      FileUtils.copyDirectory(scriptsFolder, releaseFolder)

      releaseFolder
    },

    // parse process files for test
    ParsePASS / parsePASS / fileInputExcludeFilter := (
        (ParsePASS / parsePASS / fileInputExcludeFilter).value ||
        "**/*.owl"// overwrite *.owl that is present by default
      ),
    ParsePASS / parsePASSFileType := "asm",
    ParsePASS / parsePASSProject := PASSProcessModelInterfaceCLI,
  )
  .enablePlugins(ParsePASSPlugin)


lazy val CoreASMBase = (project in file("CoreASMBase"))
  .settings(defaultSettings)
  .settings(
    name := "CoreASM Base",
    libraryDependencies ++= Dependencies.CoreASMBaseDependencies,
  )

lazy val CoreASMHelper = (project in file("CoreASMHelper"))
  .settings(defaultSettings)
  .settings(
    name := "CoreASM Helper",
    libraryDependencies ++= Dependencies.CoreASMHelperDependencies,
  )
  .dependsOn(CoreASMBase)

lazy val CoreASMBinding = (project in file("CoreASMBinding"))
  .settings(defaultSettings)
  .settings(
    name := "CoreASM Binding",
    libraryDependencies ++= Dependencies.CoreASMBindingDependencies,
  )
  .dependsOn(CoreASMBase)

lazy val CoreASMBindingAkka = (project in file("CoreASMBindingAkka"))
  .settings(defaultSettings)
  .settings(
    name := "CoreASM AkkaBinding",
    libraryDependencies ++= Dependencies.CoreASMBindingAkkaDependencies,
  )
  .dependsOn(CoreASMBinding)
  .dependsOn(AkkaStorageLib)


lazy val AkkaStorageLib = (project in file("AkkaStorage-lib"))
  .settings(defaultSettings)
  .settings(
    name := "CoreASM Akka Storage Library",
    libraryDependencies ++= Dependencies.AkkaStorageLibDependencies,
  )
  .dependsOn(CoreASMBase)

lazy val AkkaStoragePlugin = (project in file("AkkaStorage"))
  .enablePlugins(BuildInfoPlugin)
  .settings(defaultSettings)
  .settings(
    name := "CoreASM Akka Storage Plugin",
    libraryDependencies ++= Dependencies.AkkaStoragePluginDependencies,

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.athalis.coreasm.plugins.storage.info",

    assembly / assemblyJarName := "coreasm-akka-storage-plugin.jar",

    prepareRelease := {
      val file: File = assembly.value
      FileUtils.copyFileToDirectory(file, releaseFolder / "plugins")

      releaseFolder
    },
  )
  .dependsOn(CoreASMHelper)
  .dependsOn(AkkaStorageLib)


lazy val ASMSemantic = (project in file("ASMSemantic"))
  .settings(defaultSettings)
  .settings(
    name := "ASM Semantic",
    libraryDependencies ++= Dependencies.ASMSemanticDependencies,
  )
  .dependsOn(CoreASMBinding)
  .dependsOn(PASSProcessModelTUDarmstadt)

lazy val PASSInterpreterConsole = (project in file("PASSInterpreterConsole"))
  .enablePlugins(BuildInfoPlugin)
  .settings(defaultSettings)
  .settings(
    name := "PASS ASM Interpreter Console",
    libraryDependencies ++= Dependencies.PASSInterpreterConsoleDependencies(scalaVersion.value),

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.athalis.pass.ui.info",

    assembly / assemblyJarName := "pass-interpreter-console.jar",

    Compile / mainClass := Some("de.athalis.pass.ui.Boot"),

    prepareRelease := {
      val assemblyFile: File = assembly.value
      FileUtils.copyFileToDirectory(assemblyFile, releaseFolder)

      val resourcesDir = (Compile / resourceDirectory).value
      val applicationConf = resourcesDir / "application.conf"
      val logbackConf = resourcesDir / "logback.xml"
      FileUtils.copyFileToDirectory(applicationConf, releaseFolder)
      FileUtils.copyFileToDirectory(logbackConf, releaseFolder)

      releaseFolder
    },
  )
  .dependsOn(CoreASMBindingAkka)
  .dependsOn(ASMSemantic)
  .dependsOn(PASSProcessModel)
  .dependsOn(PASSProcessModelTUDarmstadt)
  .dependsOn(PASSProcessModelWriterASM)
  .dependsOn(PASSProcessModelInterface)


prepareRelease := {
  (asm / prepareRelease).value
  (AkkaStoragePlugin / prepareRelease).value
  (PASSInterpreterConsole / prepareRelease).value
  (PASSProcessModelInterfaceCLI / prepareRelease).value

  val readme = file("README.md")
  FileUtils.copyFileToDirectory(readme, releaseFolder)

  val todo = file("TODO.md")
  FileUtils.copyFileToDirectory(todo, releaseFolder)

  val excludeNoReleaseOrPDFFilter = new FileFilter {
    def accept(pathname: File): Boolean = (
        !pathname.toString.contains("no-release") &&
        !pathname.toString.contains("Skizzen") &&
        !pathname.toString.endsWith(".pdf")
      )
  }
  FileUtils.copyDirectory(processesFolder, releaseFolder / "processes", excludeNoReleaseOrPDFFilter)

  // FIXME: just to demonstrate the source
  val modelFiles = (PASSProcessModelTUDarmstadt / Compile / scalaSource).value / "de/athalis/pass/processmodel/tudarmstadt"
  FileUtils.copyDirectory(modelFiles, releaseFolder / "model-source")

  val shFilter = new org.apache.commons.io.filefilter.IOFileFilter {
    def accept(pathname: File): Boolean = pathname.toString.endsWith(".sh")
    def accept(dir: File, name: String): Boolean = accept(dir / name)
  }
  import scala.collection.JavaConverters._
  for (file <- FileUtils.iterateFiles(releaseFolder, shFilter, FalseFileFilter.INSTANCE).asScala) {
    file.setExecutable(true, false)
  }

  releaseFolder
}


cleanRelease := {
  if (releaseFolder.exists) FileUtils.cleanDirectory(releaseFolder)
}

prepareCleanRelease := (prepareRelease dependsOn cleanRelease).value

clean := (clean dependsOn cleanRelease).value


zipRelease := {
  val releaseFolderValue = prepareCleanRelease.value
  val releaseVersion = (ThisBuild / version).value

  IO.withTemporaryDirectory(tmp => {
    val name = "asm-pass-interpreter-release"
    val tmpFile = tmp / (name + "-" + releaseVersion + ".zip")
    val zipFile = file(name + "-" + releaseVersion + ".zip")

    // TODO: this doesn't preserve executable flag on *.sh files
    IO.zip(Path.allSubpaths(releaseFolderValue), tmpFile, Package.gitCommitDateTimestamp)

    IO.copyFile(tmpFile, zipFile)
    zipFile
  })
}
