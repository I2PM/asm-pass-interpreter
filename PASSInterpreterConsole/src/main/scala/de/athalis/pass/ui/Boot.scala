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

import scala.compat.java8.DurationConverters.DurationOps
import scala.concurrent._

object Boot {
  def main(args: Array[String]): Unit = {
    println("starting [" + info.BuildInfo + "]")

    AnsiConsole.systemInstall()
    implicit val terminal: Terminal = TerminalBuilder.builder()
      .system(true)
      .build()

    try {
      println("initializing Akka ActorSystem")

      val rootConfig: Config = ConfigFactory.load()
      rootConfig.checkValid(ConfigFactory.defaultReference())
      val config: Config = rootConfig.getConfig("coreasm-cli")

      implicit val timeout: Timeout = Timeout(config.getDuration("timeout").toScala)

      val system = ActorSystem("coreasm-cli", config.withFallback(rootConfig), this.getClass.getClassLoader)

      try {
        val logger: LoggingAdapter = Logging(system, getClass.getName)

        val storagePath = rootConfig.getString("coreasm-storage.remote-actor-selection-path")
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
        Await.result(system.terminate(), timeout.duration)
      }
    }
    finally {
      terminal.close()
      AnsiConsole.systemUninstall()
    }
  }
}
