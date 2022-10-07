package de.athalis

import akka.util.Timeout

import scala.async.Async.async
import scala.async.Async.await
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

package object util {
  implicit class NiceThrowable(private val ex: Throwable) extends AnyVal {
    def getNiceStackTraceString: String = {
      val sw = new java.io.StringWriter()
      val pw = new java.io.PrintWriter(sw)
      ex.printStackTrace(pw)
      sw.toString
    }
  }

  implicit class BlockingFuture[T](private val f: Future[T]) extends AnyVal {
    def blockingWait()(implicit t: Timeout): T = Await.result(f, t.duration)
  }

  def mapWithFuturesToFutureMap[A, B](m: Map[Future[A], Future[B]])(implicit executionContext: ExecutionContext): Future[Map[A, B]] = {
    val x: Future[Map[A, B]] = Future.traverse(m.toSeq)({
      case (k, v) => async {
        val k2 = await(k)
        val v2 = await(v)
        (k2 -> v2)
      }
    }).map(_.toMap)

    x
  }
}
