package de.athalis.pass.processmodel.interface.cli

import de.athalis.pass.processmodel.PASSProcessModelCollection
import de.athalis.pass.processmodel.interface.PASSProcessModelReaderInterface
import de.athalis.pass.processmodel.interface.Repository
import de.athalis.pass.processmodel.operation.PASSProcessModelWriter
import de.athalis.pass.processmodel.tudarmstadt.Process

import com.typesafe.config.ConfigFactory

import org.fusesource.jansi.AnsiConsole

import java.io.File
import java.nio.file.Path

object ConsoleConverter {

  // usage example *nix: asm outDir file1:file2:file3
  // usage example *nix: asm outDir "file1:file 2:file3"
  // usage example Windows: asm outDir file1;file2;file3
  // usage example Windows: asm outDir "file1;file 2;file3"

  def main(args: Array[String]): Unit = try {
    val config = ConfigFactory.load()
    config.checkValid(ConfigFactory.defaultReference())

    AnsiConsole.systemInstall()

    if (args.length != 3) {
      System.err.println("args: " + args.mkString(" "))
      throw new IllegalArgumentException(s"usage: ${Repository.writers.keys.mkString("<", "|", ">")} outDir file1${File.pathSeparatorChar}file2")
    }

    val typ: String = args(0)
    val outDir: Path = Path.of(args(1))
    val paths: String = args(2)

    val writer: PASSProcessModelWriter[Process] = Repository.writers.getOrElse(typ, throw new IllegalArgumentException("unknown output type: " + typ))

    val processModels: PASSProcessModelCollection[Process] = PASSProcessModelReaderInterface.readProcessModels(paths)

    val writtenFiles: Set[Path] = writer.write(processModels, outDir)

    writtenFiles.foreach(f => {
      println(f.toRealPath())
    })
  } finally {
    AnsiConsole.systemUninstall()
  }

}
