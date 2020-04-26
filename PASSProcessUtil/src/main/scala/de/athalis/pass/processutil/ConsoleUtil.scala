package de.athalis.pass.processutil

import java.io.File

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory

object ConsoleUtil {

  // usage example: asm outDir file1 "file 2" file3

  def main(args: Array[String]): Unit = {
    val root: Logger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
    root.setLevel(Level.ERROR)

    if (args.length < 3)
      throw new IllegalArgumentException("at least type, outDir and one source file needed as argument")

    val typ: String = args.head
    val files: Array[File] = args.tail.map(toFile)
    val outDir: File = files.head
    val sourceFiles: Set[File] = files.tail.toSet

    val writtenFiles: Set[File] = typ match {
      case "asm" => sourceFiles.par.flatMap(f => PASSProcessReaderUtil.readAndWriteASM(f, outDir)).seq
      case x => throw new IllegalArgumentException("unknown type: " + x)
    }

    writtenFiles.foreach(f => {
      println(f.getAbsolutePath)
    })
  }

  private def toFile(arg: String): File = {
    val filePath =
      if (arg.startsWith("\"")) {
        if (!arg.endsWith("\"")) {
          throw new IllegalArgumentException("started quote not ended")
        }
        arg.substring(1, arg.length - 2)
      }
      else arg

    new File(filePath)
  }

}
