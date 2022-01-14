import sbtassembly.AssemblyKeys.assemblyMergeStrategy
import sbtassembly.AssemblyPlugin.autoImport.{MergeStrategy, assembly}

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.FalseFileFilter

val releaseFolder = file("./release/")
val scriptsFolder = file("./scripts/")
val processesFolder = file("./processes/").getAbsoluteFile

lazy val cleanRelease = taskKey[Unit]("cleans release folder")
lazy val prepareRelease = taskKey[File]("prepares all files for the release folder and returns it")
lazy val zipRelease = taskKey[File]("zips the release folder")

ThisBuild / version := "2.0.0-M6-public"
ThisBuild / versionScheme := Some(VersionScheme.Strict) // NOTE: as of now, semver (or at least PVP) is aimed, but cannot be guaranteed as "the public API" needs to be defined

ThisBuild / scalaVersion := Dependencies.scalaVersion

ThisBuild / packageTimestamp := Package.gitCommitDateTimestamp

ThisBuild / resolvers ++= Seq(
  Resolver.mavenLocal
)

ThisBuild / semanticdbEnabled := true // enable SemanticDB
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision // only required for Scala 2.x
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % Dependencies.scalafixOrganizeImportsVersion

ThisBuild / scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-encoding", "utf8",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Xfuture",
  "-Xlint:-unused", // TODO: unused should be enabled
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused-import",
  //"-Ywarn-value-discard", // TODO: this should be enabled
  "-Xfatal-warnings",
  "-Xasync", // for scala-async 1.0.0
)

ThisBuild / assembly / assemblyMergeStrategy := {
  case "module-info.class" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

Global / cancelable := true

lazy val root = (project in file("."))
  // run Tasks like test and compile on all projects
  .aggregate(
    PASSProcessModel,
    PASSProcessModelOperation,
    PASSProcessModelTUDarmstadt,
    PASSProcessUtilBase,
    PASSProcessModelParserAST,
    PASSProcessModelParserGraphML,
    PASSProcessModelWriterASM,
    PASSProcessModelInterface,

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
  .settings(
    name := "asm-pass-interpreter",
    run := (PASSInterpreterConsole / Compile / run).evaluated, // overwrite run Task to use the run in PASSUI
  )

lazy val PASSProcessModel = (project in file("PASSProcessModel"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "PASS Process Model",
    libraryDependencies ++= Dependencies.PASSProcessModelDependencies,

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.athalis.pass.processmodel.info",
  )

lazy val PASSProcessModelOperation = (project in file("PASSProcessModelOperation"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "PASS Process Model Operation",
    libraryDependencies ++= Dependencies.PASSProcessModelOperationDependencies,

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.athalis.pass.processmodel.operation.info",
  )
  .dependsOn(PASSProcessModel)

lazy val PASSProcessModelTUDarmstadt = (project in file("PASSProcessModelTUDarmstadt"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "PASS Process Model TUDarmstadt",
    libraryDependencies ++= Dependencies.PASSProcessModelTUDarmstadtDependencies,

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.athalis.pass.processmodel.tudarmstadt.info",
  )
  .dependsOn(PASSProcessModel)


lazy val PASSProcessUtilBase = (project in file("PASSProcessUtilBase"))
  .settings(
    name := "PASS Process Util Base",
    libraryDependencies ++= Dependencies.PASSProcessUtilBaseDependencies,
  )
  .dependsOn(PASSProcessModel)
  .dependsOn(PASSProcessModelTUDarmstadt)

lazy val PASSProcessModelParserAST = (project in file("PASSProcessModelParserAST"))
  .settings(
    name := "PASS Process Model Parser AST",
    libraryDependencies ++= Dependencies.PASSProcessModelParserASTDependencies,
  )
  .dependsOn(PASSProcessModel)
  .dependsOn(PASSProcessModelOperation)
  .dependsOn(PASSProcessModelTUDarmstadt)
  .dependsOn(PASSProcessUtilBase)

lazy val PASSProcessModelParserGraphML = (project in file("PASSProcessModelParserGraphML"))
  .settings(
    name := "PASS Process Model Parser GraphML",
    libraryDependencies ++= Dependencies.PASSProcessModelParserGraphMLDependencies,
  )
  .dependsOn(PASSProcessModel)
  .dependsOn(PASSProcessModelTUDarmstadt)
  .dependsOn(PASSProcessUtilBase)
  .dependsOn(PASSProcessModelParserAST % "test->test;compile->compile")

lazy val PASSProcessModelWriterASM = (project in file("PASSProcessModelWriterASM"))
  .settings(
    name := "PASS Process Model Writer ASM",
    libraryDependencies ++= Dependencies.PASSProcessModelWriterASMDependencies,
  )
  .dependsOn(PASSProcessModel)
  .dependsOn(PASSProcessModelOperation)
  .dependsOn(PASSProcessModelTUDarmstadt)
  .dependsOn(PASSProcessUtilBase)


lazy val PASSProcessModelInterface = (project in file("PASSProcessModelInterface"))
  .settings(
    name := "PASS Process Model Interface",
    libraryDependencies ++= Dependencies.PASSProcessModelInterfaceDependencies,

    parsePASSClass := "de.athalis.pass.processmodel.interface.ConsoleUtil",

    prepareRelease := {
      val resources = (Compile / resourceDirectory).value
      FileUtils.copyDirectory(resources, releaseFolder)

      releaseFolder
    },
  )
  .enablePlugins(PASSProcessModelParserPlugin)
  .dependsOn(PASSProcessModel)
  .dependsOn(PASSProcessModelOperation)
  .dependsOn(PASSProcessModelTUDarmstadt)
  .dependsOn(PASSProcessUtilBase)
  .dependsOn(PASSProcessModelParserAST % "test->test;compile->compile")
  .dependsOn(PASSProcessModelParserGraphML % "test->test;compile->compile")
  .dependsOn(PASSProcessModelWriterASM % "test->test;compile->compile")



lazy val asm = (project in file("asm"))
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
    ParsePASS / parsePASSProject := PASSProcessModelInterface,

    // fork is necessary as the CoreASM Engine only writes to System.out
    Test / fork := true,
  )
  .enablePlugins(ParsePASSPlugin)


lazy val CoreASMBase = (project in file("CoreASMBase"))
  .settings(
    name := "CoreASM Base",
    libraryDependencies ++= Dependencies.CoreASMBaseDependencies,
  )

lazy val CoreASMHelper = (project in file("CoreASMHelper"))
  .settings(
    name := "CoreASM Helper",
    libraryDependencies ++= Dependencies.CoreASMHelperDependencies,
  )
  .dependsOn(CoreASMBase)

lazy val CoreASMBinding = (project in file("CoreASMBinding"))
  .settings(
    name := "CoreASM Binding",
    libraryDependencies ++= Dependencies.CoreASMBindingDependencies,
  )
  .dependsOn(CoreASMBase)

lazy val CoreASMBindingAkka = (project in file("CoreASMBindingAkka"))
  .settings(
    name := "CoreASM AkkaBinding",
    libraryDependencies ++= Dependencies.CoreASMBindingAkkaDependencies,
  )
  .dependsOn(CoreASMBinding)
  .dependsOn(AkkaStorageLib)


lazy val AkkaStorageLib = (project in file("AkkaStorage-lib"))
  .settings(
    name := "CoreASM Akka Storage Library",
    libraryDependencies ++= Dependencies.AkkaStorageLibDependencies,
  )
  .dependsOn(CoreASMBase)

lazy val AkkaStoragePlugin = (project in file("AkkaStorage"))
  .enablePlugins(BuildInfoPlugin)
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
  .settings(
    name := "ASM Semantic",
    libraryDependencies ++= Dependencies.ASMSemanticDependencies,
  )
  .dependsOn(CoreASMBinding)
  .dependsOn(PASSProcessModelTUDarmstadt)

lazy val PASSInterpreterConsole = (project in file("PASSInterpreterConsole"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "PASS ASM Interpreter Console",
    libraryDependencies ++= Dependencies.PASSInterpreterConsoleDependencies,

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.athalis.pass.ui.info",

    assembly / assemblyJarName := "pass-interpreter-console.jar",

    Compile / mainClass := Some("de.athalis.pass.ui.Boot"),
    run / baseDirectory := file("."), // important, otherwise it executed in the `PASSUI` folder and the `processes` folder cannot be accessed easily

    prepareRelease := {
      val assemblyFile: File = assembly.value
      FileUtils.copyFileToDirectory(assemblyFile, releaseFolder)

      val resources = (Compile / resourceDirectory).value
      FileUtils.copyDirectory(resources, releaseFolder)

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
  (PASSProcessModelInterface / prepareRelease).value

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

prepareRelease := (prepareRelease dependsOn cleanRelease).value

clean := (clean dependsOn cleanRelease).value


zipRelease := {
  val releaseFolderValue = prepareRelease.value
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
