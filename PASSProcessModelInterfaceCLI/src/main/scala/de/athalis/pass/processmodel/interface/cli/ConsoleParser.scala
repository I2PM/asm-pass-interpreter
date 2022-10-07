package de.athalis.pass.processmodel.interface.cli

import de.athalis.pass.processmodel.PASSProcessModelCollection
import de.athalis.pass.processmodel.interface.PASSProcessModelReaderInterface
import de.athalis.pass.processmodel.tudarmstadt.Process

import com.typesafe.config.ConfigFactory

import org.fusesource.jansi.AnsiConsole

import java.io.File

object ConsoleParser {

  def main(args: Array[String]): Unit = try {
    val config = ConfigFactory.load()
    config.checkValid(ConfigFactory.defaultReference())

    AnsiConsole.systemInstall()

    if (args.length != 1) {
      System.err.println("args: " + args.mkString(" "))
      throw new IllegalArgumentException(s"please specify file names, separated by '${File.pathSeparatorChar}'")
    }

    val processModels: PASSProcessModelCollection[Process] = PASSProcessModelReaderInterface.readProcessModels(args(0))

    println(processModels)
  } finally {
    AnsiConsole.systemUninstall()
  }

}
