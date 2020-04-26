package de.athalis.coreasm.binding.akka

import akka.actor.ActorSelection
import akka.event.LoggingAdapter
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.{Await, Future}

import de.athalis.coreasm.base.Typedefs._
import de.athalis.coreasm.binding._
import de.athalis.coreasm.plugins.storage.lib._

class AkkaStorageBinding(val storageActor: ActorSelection, val logger: LoggingAdapter)(implicit executor: scala.concurrent.ExecutionContext, timeout: Timeout) extends Binding {

  override def load[T](f: DerivedFunction[_]): Option[T] = {
    logger.warning("blocking on synchronous load! Please use loadAsync!")
    Await.result(loadAsync(f), timeout.duration)
  }

  override def loadAsync[T](f: DerivedFunction[_]): Future[Option[T]] = {
    val startTime = System.nanoTime
    val replyF = (storageActor ? ValueRequest(f.location)).mapTo[ValueReply[T]]
    replyF.map( x => {
      val time = ((System.nanoTime-startTime)/1e6)
      logger.debug("loaded: {} = {} ({}ms total)", f, x, time)
      if (x.duration > 20) {
        logger.warning("long read: " + x.duration + "ms for " + f.location.head)
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
