package de.athalis.coreasm.plugins.storage

import java.util.concurrent.BlockingQueue

import akka.actor._
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import de.athalis.coreasm.plugins.storage.lib._

object AkkaStorageActor {
  private val logger: Logger = LoggerFactory.getLogger(AkkaStorageActor.getClass)
}

class AkkaStorageActor(queue: BlockingQueue[(ActorRef, AkkaStorageJob)]) extends Actor {
  import AkkaStorageActor._

  logger.info("AkkaStorageActor starting")

  def receive = {
    case msg: AkkaStorageJob => queue.put((sender(), msg))
    case x => logger.error("received unknown message: {}", x)
  }
}
