import org.apache.commons.io.FileUtils
import java.nio.file.Path

import sbt.Keys._
import sbt.{Def, _}
import sbt.nio.Keys._
import sbt.nio.file.FileTreeView

object ParsePASSPlugin extends AutoPlugin {
  override def requires: Plugins = plugins.JvmPlugin

  object autoImport {
    lazy val ParsePASS = config("parse-pass") extend (Compile)
    lazy val ParsePASSTest = config("parse-pass-test") extend (ParsePASS)

    // executed in project of sources
    lazy val parsePASS = taskKey[Seq[File]]("parses PASS Process files of the sourceDirectory and stores the generated files in resourceManaged")
    lazy val parsePASSFileType = settingKey[String]("type of the generated files. either asm or owl")
    lazy val parsePASSProject = settingKey[Project]("project that has the PASSProcessParserPlugin enabled")
  }

  import autoImport._

  lazy val basePASSSettings: Seq[Setting[_]] = Def.settings(
    sourceDirectory := ((Compile / sourceDirectory) { _ / "pass" }).value,
    resourceManaged := ((Compile / resourceManaged) { _ / "pass" }).value,

    parsePASS / fileInputs += sourceDirectory.value.toGlob / "*.{pass,graphml,owl}",
    parsePASS / fileOutputs += resourceManaged.value.toGlob / "*.{casm,owl}",

    // have to be defined in actual project
    // parsePASSFileType
    // parsePASSProject

    parsePASS := (Def.taskDyn[Seq[File]] {
      val logger = streams.value.log

      lazy val runTask = (PASSProcessParserPlugin.autoImport.runParsePASSClassToFiles in parsePASSProject.value)

      val typ: String = parsePASSFileType.value

      val outDir: File = resourceManaged.value

      val changes: sbt.nio.FileChanges = parsePASS.inputFileChanges

      // unfortunately, we do not easily know which source file created which casm file(s)
      // therefore to be safe, re-parse everything
      if (changes.hasChanges) {
        logger.info("ParsePASS: changes detected, re-parsing all process files")

        if (outDir.exists()) {
          FileUtils.cleanDirectory(outDir)
        }

        val sourcesValue: Seq[Path] = changes.created ++ changes.modified ++ changes.unmodified

        val args: Seq[String] = PASSProcessParserPlugin.prepareArguments(typ, outDir, sourcesValue)

        // return task, that generates the files and returns them
        runTask.toTask(" " + args.mkString(" "))
      }
      else {
        // return task, that returns all existing files for resourceGenerator
        Def.task {
          FileTreeView.default.list((parsePASS / fileOutputs).value).map(_._1.toFile)
        }
      }
    }).value
  )

  // FIXME: `baseParsePASSSettings ++` shouldn't be needed due to `extend (ParsePASS)`?
  lazy val testPASSSettings: Seq[Setting[_]] = basePASSSettings ++ Def.settings(
    sourceDirectory := ((Test / sourceDirectory) { _ / "pass" }).value,
    resourceManaged := ((Test / resourceManaged) { _ / "pass" }).value,

    parsePASS := (parsePASS dependsOn (ParsePASS / parsePASS)).value,

    parsePASSFileType := (ParsePASS / parsePASSFileType).value,
    parsePASSProject := (ParsePASS / parsePASSProject).value
  )

  override lazy val projectSettings: Seq[Setting[_]] =
    inConfig(ParsePASS)(basePASSSettings) ++
    inConfig(ParsePASSTest)(testPASSSettings) ++ Def.settings(
      // FIXME: it looks like there is a bug in sbt regarding test:clean and test:copyResources. This works around that, as now test:clean depends on parse-pass:clean, which somehow magically cleans everything (altough it should just clean for itself)

      (Compile / resourceGenerators) += (ParsePASS / parsePASS).taskValue,
      (Compile / managedResourceDirectories) += (ParsePASS / resourceManaged).value,
      (Compile / clean) := (Compile / clean).dependsOn(clean in ParsePASS).value,

      (resourceGenerators in Test) += (ParsePASSTest / parsePASS).taskValue,
      (managedResourceDirectories in Test) += (ParsePASSTest / resourceManaged).value,
      (clean in Test) := (clean in Test).dependsOn(clean in ParsePASSTest).value,
    )
  
}
