import java.io.File

import sbt.Keys._
import sbt.{Def, _}

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

    includeFilter := "*.pass" | "*.graphml" | "*.owl",
    excludeFilter := HiddenFileFilter,

    sources := {
      val srcDir = sourceDirectory.value
      val incl = includeFilter.value
      val excl = excludeFilter.value
      (srcDir * (incl -- excl)).get
    },

    // have to be defined in actual project
    // parsePASSFileType
    // parsePASSProject

    parsePASS := (Def.taskDyn[Seq[File]] {
      lazy val runTask = (PASSProcessParserPlugin.autoImport.runParsePASSClassToFiles in parsePASSProject.value)

      val typ: String = parsePASSFileType.value

      val outDir: File = resourceManaged.value

      val sourcesValue: Seq[File] = sources.value

      if (sourcesValue.nonEmpty) {
        lazy val args: Seq[String] = PASSProcessParserPlugin.prepareArguments(typ, outDir, sourcesValue)
        runTask.toTask(" " + args.map(_.replace("\\", "\\\\")).mkString(" "))
      }
      else {
        Def.task {
          Seq.empty
        }
      }

      /*
       TODO: restore caching functionality.
         The approach below is no longer possible with the new structure.
         sbt 1.3 brings a new feature that should be looked at https://www.scala-sbt.org/1.0/docs/Howto-Track-File-Inputs-and-Outputs.html

      val cachedFun = FileFunction.cached(streams.value.cacheDirectory / "pass", FilesInfo.lastModified, FilesInfo.lastModified) {
        (in: Set[File]) => {
          val args: Seq[String] = ParsePASSProcesses.prepareArguments(typ, outDir, in)
          runTask.fullInput(args.mkString(" ")).evaluated
        }
      }
      cachedFun(sources.value.toSet).toSeq
      */
    }).value
  )

  // FIXME: `baseParsePASSSettings ++` shouldn't be needed due to `extend (ParsePASS)`?
  lazy val testPASSSettings: Seq[Setting[_]] = basePASSSettings ++ Def.settings(
    sourceDirectory := ((Test / sourceDirectory) { _ / "pass" }).value,
    resourceManaged := ((Test / resourceManaged) { _ / "pass" }).value,

    parsePASS := (parsePASS dependsOn (ParsePASS / parsePASS)).value,

    // default: use from parent in case it is overwritten there
    includeFilter := (ParsePASS / includeFilter).value,
    excludeFilter := (ParsePASS / excludeFilter).value,

    parsePASSFileType := (ParsePASS / parsePASSFileType).value,
    parsePASSProject := (ParsePASS / parsePASSProject).value
  )

  override lazy val projectSettings: Seq[Setting[_]] =
    inConfig(ParsePASS)(basePASSSettings) ++
    inConfig(ParsePASSTest)(testPASSSettings) ++ Def.settings(
      (Compile / resourceGenerators) += (ParsePASS / parsePASS).taskValue,
      (Compile / managedResourceDirectories) += (ParsePASS / resourceManaged).value,
      cleanFiles += (ParsePASS / resourceManaged).value,

      (resourceGenerators in Test) += (ParsePASSTest / parsePASS).taskValue,
      (managedResourceDirectories in Test) += (ParsePASSTest / resourceManaged).value,
      cleanFiles += (ParsePASSTest / resourceManaged).value
    )
  
}
