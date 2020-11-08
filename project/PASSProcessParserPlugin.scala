import java.io.{ByteArrayOutputStream, File}
import java.nio.charset.StandardCharsets
import java.nio.file.Path

import org.slf4j.LoggerFactory
import sbt.Keys._
import sbt.internal.util.ManagedLogger
import sbt.{Def, _}

object PASSProcessParserPlugin extends AutoPlugin {
  private val logger = LoggerFactory.getLogger(PASSProcessParserPlugin.getClass)

  def prepareArguments(typ: String, outDir: File, in: Seq[Path]): Seq[String] = {
    val outDirEscaped: String = "\"" + outDir.getAbsolutePath.replace("\\", "\\\\") + "\""
    val filePathsEscaped1: Seq[String] = in.map(path => path.toAbsolutePath.toString.replace("\\", "\\\\"))
    val filePathsEscaped2: String = filePathsEscaped1.mkString("\"", File.pathSeparator, "\"")
    Seq(typ, outDirEscaped, filePathsEscaped2)
  }

  override def requires: Plugins = plugins.JvmPlugin

  object autoImport {
    lazy val parsePASSClass = settingKey[String]("full main class name that parses PASS Process files")
    lazy val runParsePASSClassToFiles = inputKey[Seq[File]]("runs parsePASSClass with arguments as input, maps each line of the output to Set[Path]")
  }

  import autoImport._

  lazy val basePASSProcessParserSettings: Seq[Setting[_]] = Def.settings(
    // has to be defined in actual project: parsePASSClass

    runParsePASSClassToFiles := {
      val logger: ManagedLogger = streams.value.log

      val args: Seq[String] = complete.DefaultParsers.spaceDelimited("<arg>").parsed

      if (args.lengthCompare(3) != 0) {
        throw new IllegalArgumentException(s"usage: <asm|owl> outDir file1${File.pathSeparatorChar}file2")
      }
      else {
        logParsing(logger, args)

        val parsePASSClassValue: String = parsePASSClass.value
        val forkOptionsValue: ForkOptions = (Compile / run / forkOptions).value

        val x = Vector("-cp", (fullClasspath in Compile).value.map{_.data.getAbsolutePath }.mkString(File.pathSeparator))

        val generatedFiles: Seq[File] = ParsePASSProcesses.runMainToFiles(parsePASSClassValue, args, forkOptionsValue, x)

        logger.info(s"Done, ${generatedFiles.length} ${args.head} files generated.")

        generatedFiles
      }
    }
  )

  private def logParsing(logger: ManagedLogger, args: Seq[String]): Unit = {
    val typ: String = args(0)
    val outDir = args(1)
    val sources = args(2).split(File.pathSeparatorChar)

    logger.info(s"Parsing ${sources.length} PASS Process sources to $typ in $outDir ...")
  }

  override lazy val projectSettings: Seq[Setting[_]] = basePASSProcessParserSettings

  private object ParsePASSProcesses {
    def runMainToFiles(mainClass: String, arguments: Seq[String], forkOptions: ForkOptions, runJVMOptions: Vector[String]): Seq[File] = {
      val outputStream: ByteArrayOutputStream = new ByteArrayOutputStream()
      val options = forkOptions
        .withOutputStrategy(CustomOutput(outputStream))
        .withRunJVMOptions(runJVMOptions)

      // TODO: why is fullClasspath needed / why does Fork.scala not work out of the box?

      val exitCode: Int = Fork.java(options, mainClass +: arguments)

      val output = outputStream.toString(StandardCharsets.UTF_8)

      if (exitCode != 0) {
        logger.error(output)
        throw new Exception("failed to parse source(s)")
      }
      else {
        parseOutput(output)
      }
    }

    def parseOutput(output: String): Seq[File] = {
      output.linesIterator.map(line => new File(line)).toSeq
    }
  }
  
}
