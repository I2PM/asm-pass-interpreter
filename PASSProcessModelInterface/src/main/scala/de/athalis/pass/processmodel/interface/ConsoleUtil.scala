package de.athalis.pass.processmodel.interface

import de.athalis.pass.processmodel.PASSProcessModelCollection
import de.athalis.pass.processmodel.tudarmstadt.Process
import de.athalis.pass.processmodel.writer.asm.PASSProcessModelWriterASM
import de.athalis.pass.processutil.base.PASSProcessModelWriter

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger

import org.slf4j.LoggerFactory

import java.io.File
import java.nio.file.Path

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
    val outDir: Path = Path.of(args(1))
    val paths: String = args(2)

    val writer: PASSProcessModelWriter = typ match {
      case "asm" => PASSProcessModelWriterASM
      case "owl" => throw new UnsupportedOperationException("OWL is not yet supported in public releases")
      case x => throw new IllegalArgumentException("unknown output type: " + x)
    }

    val processModels: PASSProcessModelCollection[Process] = PASSProcessModelReaderInterface.readProcessModels(paths)

    val writtenFiles: Set[Path] = writer.write(processModels, outDir)

    writtenFiles.foreach(f => {
      println(f.toRealPath())
    })
  }

}
