package de.athalis.pass.processutil

import java.io.File

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger

import de.athalis.pass.model.TUDarmstadtModel.Process
import de.athalis.pass.processutil.base.PASSProcessWriter
import de.athalis.pass.writer.asm.PASSProcessWriterASM

import org.slf4j.LoggerFactory

object ConsoleUtil {

  // usage example *nix: asm outDir file1:file2:file3
  // usage example *nix: asm outDir "file1:file 2:file3"
  // usage example Windows: asm outDir file1;file2;file3
  // usage example Windows: asm outDir "file1;file 2;file3"

  def main(args: Array[String]): Unit = {
    val root: Logger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
    root.setLevel(Level.ERROR)

    if (args.length != 3) {
      System.err.println("args: " + args.mkString(" "))
      throw new IllegalArgumentException(s"usage: <asm|owl> outDir file1${File.pathSeparatorChar}file2")
    }

    val typ: String = args(0)
    val outDir: File = new File(args(1))
    val paths: String = args(2)

    val writer: PASSProcessWriter = typ match {
      case "asm" => PASSProcessWriterASM
      case "owl" => throw new UnsupportedOperationException("OWL is not yet supported in public releases")
      case x => throw new IllegalArgumentException("unknown output type: " + x)
    }

    val processes: Set[Process] = PASSProcessReaderUtil.readProcesses(paths)

    val writtenFiles = writer.write(processes, outDir)

    writtenFiles.foreach(f => {
      println(f.getAbsolutePath)
    })
  }

}
