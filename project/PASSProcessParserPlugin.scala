import java.io.{ByteArrayOutputStream, File}
import java.nio.charset.StandardCharsets

import org.slf4j.LoggerFactory
import sbt.Keys._
import sbt.internal.util.ManagedLogger
import sbt.{Def, _}

object PASSProcessParserPlugin extends AutoPlugin {
  private val logger = LoggerFactory.getLogger(PASSProcessParserPlugin.getClass)

  def prepareArguments(typ: String, outDir: File, in: Seq[File]): Seq[String] = {
    typ +: ("\"" + outDir.getAbsolutePath + "\"") +: in.map(f => "\"" + f.getAbsolutePath + "\"")
  }

  override def requires: Plugins = plugins.JvmPlugin

  object autoImport {
    lazy val parsePASSClass = settingKey[String]("full main class name that parses PASS Process files")
    lazy val runParsePASSClassToFiles = inputKey[Seq[File]]("runs parsePASSClass with arguments as input, maps each line of the output to Set[File]")
  }

  import autoImport._

  lazy val basePASSProcessParserSettings: Seq[Setting[_]] = Def.settings(
    // has to be defined in actual project: parsePASSClass

    runParsePASSClassToFiles := {
      val logger: ManagedLogger = streams.value.log

      val args: Seq[String] = complete.DefaultParsers.spaceDelimited("<arg>").parsed

      if (args.lengthCompare(3) < 0) {
        throw new IllegalArgumentException("at least type, outDir and one source file needed as argument")
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
    val typ: String = args.head
    val files = args.tail
    val outDir = files.head
    val sources = files.tail

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
