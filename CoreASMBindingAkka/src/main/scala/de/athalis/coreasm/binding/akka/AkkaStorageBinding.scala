package de.athalis.coreasm.binding.akka

import de.athalis.coreasm.base.Typedefs._
import de.athalis.coreasm.binding.Binding
import de.athalis.coreasm.plugins.storage.lib._

import akka.actor.ActorSelection
import akka.event.LoggingAdapter
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.Future

class AkkaStorageBinding(val storageActor: ActorSelection, val logger: LoggingAdapter)(implicit executor: scala.concurrent.ExecutionContext, timeout: Timeout) extends Binding {

  override def load[T](l: ASMLocation): Option[T] = {
    logger.warning("blocking on synchronous load! Please use loadAsync!")
    Await.result(loadAsync(l), timeout.duration)
  }

  override def loadAsync[T](l: ASMLocation): Future[Option[T]] = {
    val startTime = System.nanoTime
    val replyF = (storageActor ? ValueRequest(l)).mapTo[ValueReply[T]]
    replyF.map( x => {
      val time = ((System.nanoTime-startTime)/1e6)
      logger.debug("loaded: {} = {} ({}ms total)", l, x, time)
      if (x.duration > 20) {
        logger.warning("long read: " + x.duration + "ms for " + l.head)
      }
    })
    replyF.map(_.value)
  }


  override def store(updates: Seq[ASMUpdate]): UpdateResult = {
    logger.warning("blocking on synchronous store! Please use storeAsync!")
    Await.result(storeAsync(updates), timeout.duration)
  }

  override def storeAsync(updates: Seq[ASMUpdate]): Future[UpdateResult] = {
    logger.debug("store: {}", updates)

    val startTime = System.nanoTime()

    val f = (storageActor ? ApplyUpdates(updates)).mapTo[UpdateResult]

    f.map(_ => {
      logger.debug("stored after " + (System.nanoTime() - startTime)/1e6 + "ms: {}", updates)
    })

    f.recover {
      case x => {
        logger.error(x, "Failed to apply updates")
        UpdateFailed
      }
    }
  }


  def waitForASMStep(): Future[Any] = {
    (storageActor ? AwaitASMStep)
  }

}
