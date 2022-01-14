package de.athalis.pass.ui

import de.athalis.coreasm.binding.akka.AkkaStorageBinding

import akka.actor._
import akka.event.Logging
import akka.event.LoggingAdapter
import akka.util.Timeout
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder

import org.fusesource.jansi.AnsiConsole

import scala.concurrent._
import scala.concurrent.duration._

object Boot {
  private implicit val timeout: Timeout = Timeout(10.seconds)

  def main(args: Array[String]): Unit = {
    println("starting [" + info.BuildInfo + "]")

    AnsiConsole.systemInstall()
    implicit val terminal: Terminal = TerminalBuilder.builder()
      .system(true)
      .build()

    try {
      println("initializing Akka ActorSystem")

      val config: Config = ConfigFactory.load(this.getClass.getClassLoader)

      val system = ActorSystem("coreasm-cli", config.getConfig("coreasm-cli").withFallback(config), this.getClass.getClassLoader)

      try {
        val logger: LoggingAdapter = Logging(system, getClass.getName)

        val storagePath = config.getString("coreasm-storage.remote-actor-selection-path")
        val storageActor: ActorSelection = system.actorSelection(storagePath)

        logger.info("initialized Akka ActorSystem")

        implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global

        val bindingLogger: LoggingAdapter = Logging(system, classOf[AkkaStorageBinding].getName)
        implicit val binding: AkkaStorageBinding = new AkkaStorageBinding(storageActor, bindingLogger)

        implicit val uiLogger: LoggingAdapter = Logging(system, classOf[PASSInterpreterConsole].getName)
        val console: PASSInterpreterConsole = new PASSInterpreterConsole()

        console.run()
      }
      finally {
        Await.result(system.terminate(), 10.seconds)
      }
    }
    finally {
      terminal.close()
      AnsiConsole.systemUninstall()
    }
  }
}
