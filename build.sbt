import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.FalseFileFilter

val releaseFolder = file("./release/")
val scriptsFolder = file("./scripts/")
val processesFolder = file("./processes/").getAbsoluteFile

lazy val cleanRelease = taskKey[Unit]("cleans release folder")
lazy val prepareRelease = taskKey[File]("prepares all files for the release folder and returns it")
lazy val zipRelease = taskKey[File]("zips the release folder")

lazy val root = (project in file("."))
  // run Tasks like test and compile on all projects
  .aggregate(
    PASSProcessModel,
    PASSProcessParserAST,
    PASSProcessParserGraphML,
    PASSProcessWriterASM,
    PASSProcessUtil,

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
  .settings(Commons.settings)
  .settings(
    name := "asm-pass-interpreter",
    run := (PASSInterpreterConsole / Compile / run).evaluated, // overwrite run Task to use the run in PASSUI
  )

lazy val PASSProcessModel = (project in file("PASSProcessModel"))
  .enablePlugins(BuildInfoPlugin)
  .settings(Commons.settings)
  .settings(
    name := "PASS Process Model",
    libraryDependencies ++= Dependencies.PASSProcessModelDependencies,

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.athalis.pass.model.info",
  )


lazy val PASSProcessUtilBase = (project in file("PASSProcessUtilBase"))
  .settings(Commons.settings)
  .settings(
    name := "PASS Process Util Base",
    libraryDependencies ++= Dependencies.PASSProcessUtilBaseDependencies,
  )
  .dependsOn(PASSProcessModel)

lazy val PASSProcessParserAST = (project in file("PASSProcessParserAST"))
  .settings(Commons.settings)
  .settings(
    name := "PASS Process Parser AST",
    libraryDependencies ++= Dependencies.PASSProcessParserASTDependencies,
  )
  .dependsOn(PASSProcessUtilBase)

lazy val PASSProcessParserGraphML = (project in file("PASSProcessParserGraphML"))
  .settings(Commons.settings)
  .settings(
    name := "PASS Process Parser GraphML",
    libraryDependencies ++= Dependencies.PASSProcessParserGraphMLDependencies,
  )
  .dependsOn(PASSProcessUtilBase)
  .dependsOn(PASSProcessParserAST % "test->test;compile->compile")

lazy val PASSProcessWriterASM = (project in file("PASSProcessWriterASM"))
  .settings(Commons.settings)
  .settings(
    name := "PASS Process Writer ASM",
    libraryDependencies ++= Dependencies.PASSProcessWriterASMDependencies,
  )
  .dependsOn(PASSProcessUtilBase)
  .dependsOn(PASSProcessModel)


lazy val PASSProcessUtil = (project in file("PASSProcessUtil"))
  .settings(Commons.settings)
  .settings(
    name := "PASS Process Util",
    libraryDependencies ++= Dependencies.PASSProcessUtilDependencies,

    parsePASSClass := "de.athalis.pass.processutil.ConsoleUtil",

    prepareRelease := {
      val resources = (Compile / resourceDirectory).value
      FileUtils.copyDirectory(resources, releaseFolder)

      releaseFolder
    },
  )
  .enablePlugins(PASSProcessParserPlugin)
  .dependsOn(PASSProcessUtilBase)
  .dependsOn(PASSProcessParserAST % "test->test;compile->compile")
  .dependsOn(PASSProcessParserGraphML % "test->test;compile->compile")
  .dependsOn(PASSProcessWriterASM % "test->test;compile->compile")



lazy val asm = (project in file("asm"))
  .settings(Commons.settings)
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
    ParsePASS / parsePASSProject := PASSProcessUtil,

    // fork is necessary as the CoreASM Engine only writes to System.out
    Test / fork := true,
  )
  .enablePlugins(ParsePASSPlugin)


lazy val CoreASMBase = (project in file("CoreASMBase"))
  .settings(Commons.settings)
  .settings(
    name := "CoreASM Base",
    libraryDependencies ++= Dependencies.CoreASMBaseDependencies,
  )

lazy val CoreASMHelper = (project in file("CoreASMHelper"))
  .settings(Commons.settings)
  .settings(
    name := "CoreASM Helper",
    libraryDependencies ++= Dependencies.CoreASMHelperDependencies,
  )
  .dependsOn(CoreASMBase)

lazy val CoreASMBinding = (project in file("CoreASMBinding"))
  .settings(Commons.settings)
  .settings(
    name := "CoreASM Binding",
    libraryDependencies ++= Dependencies.CoreASMBindingDependencies,
  )
  .dependsOn(CoreASMBase)

lazy val CoreASMBindingAkka = (project in file("CoreASMBindingAkka"))
  .settings(Commons.settings)
  .settings(
    name := "CoreASM AkkaBinding",
    libraryDependencies ++= Dependencies.CoreASMBindingAkkaDependencies,
  )
  .dependsOn(CoreASMBinding)
  .dependsOn(AkkaStorageLib)


lazy val AkkaStorageLib = (project in file("AkkaStorage-lib"))
  .settings(Commons.settings)
  .settings(
    name := "CoreASM Akka Storage Library",
    libraryDependencies ++= Dependencies.AkkaStorageLibDependencies,
  )
  .dependsOn(CoreASMBase)

lazy val AkkaStoragePlugin = (project in file("AkkaStorage"))
  .enablePlugins(BuildInfoPlugin)
  .settings(Commons.settings)
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
  .settings(Commons.settings)
  .settings(
    name := "ASM Semantic",
    libraryDependencies ++= Dependencies.ASMSemanticDependencies,
  )
  .dependsOn(CoreASMBinding)

lazy val PASSInterpreterConsole = (project in file("PASSInterpreterConsole"))
  .enablePlugins(BuildInfoPlugin)
  .settings(Commons.settings)
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
  .dependsOn(PASSProcessUtil)


prepareRelease := {
  (asm / prepareRelease).value
  (AkkaStoragePlugin / prepareRelease).value
  (PASSInterpreterConsole / prepareRelease).value
  (PASSProcessUtil / prepareRelease).value

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
  val modelFiles = (PASSProcessModel / Compile / scalaSource).value / "de/athalis/pass/model/TUDarmstadtModel"
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

  IO.withTemporaryDirectory(tmp => {
    val name = "asm-pass-interpreter-release"
    val version = Commons.appVersion
    val tmpFile = tmp / (name + "-" + version + ".zip")
    val zipFile = file(name + "-" + version + ".zip")

    // TODO: this doesn't preserve executable flag on *.sh files
    IO.zip(Path.allSubpaths(releaseFolderValue), tmpFile)

    IO.copyFile(tmpFile, zipFile)
    zipFile
  })
}
