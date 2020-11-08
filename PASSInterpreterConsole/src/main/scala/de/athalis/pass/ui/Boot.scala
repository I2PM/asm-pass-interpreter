package de.athalis.pass.ui

import com.typesafe.config.{Config, ConfigFactory}

import akka.actor._
import akka.event.{Logging, LoggingAdapter}
import akka.util.Timeout

import scala.concurrent._
import scala.concurrent.duration._

import org.fusesource.jansi.AnsiConsole
import org.jline.terminal.{Terminal, TerminalBuilder}

import de.athalis.coreasm.binding.akka.AkkaStorageBinding

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

        val storageHost = config.getString("coreasm-storage.hostname")
        val storagePort = config.getString("coreasm-storage.port").toInt
        val storageActor: ActorSelection = system.actorSelection("akka.tcp://coreasm-storage@" + storageHost + ":" + storagePort + "/user/AkkaStorageActor")

        logger.info("initialized Akka ActorSystem")

        implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global

        val bindingLogger: LoggingAdapter = Logging(system, classOf[AkkaStorageBinding].getName)
        implicit val binding: AkkaStorageBinding = new AkkaStorageBinding(storageActor, bindingLogger)

        implicit val uiLogger: LoggingAdapter = Logging(system, classOf[PASSInterpreterConsole].getName)
        val console: PASSInterpreterConsole = new PASSInterpreterConsole()

        console.run()
      }
      finally {
        Await.result(system.terminate, 10.seconds)
      }
    }
    finally {
      terminal.close()
      AnsiConsole.systemUninstall()
    }
  }
}
