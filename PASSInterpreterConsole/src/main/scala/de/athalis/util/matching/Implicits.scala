package de.athalis.util.matching

import scala.concurrent.Future
import scala.util.matching.Regex

/**
  * Created by locke on 28/02/16.
  */
object Implicits {
  implicit class ConcurrentRegex(private val regex: Regex) extends AnyVal {
    def replaceAllInConcurrent(target: CharSequence, replacer: Regex.Match => Future[String])(implicit ctx: scala.concurrent.ExecutionContext): Future[String] = {
      val f: Iterator[Future[String]] = regex.findAllMatchIn(target).map(replacer)

      Future.sequence(f).map( it2 => {
        regex.replaceAllIn(target, y => it2.next())
      })
    }

    def replaceSomeInConcurrent(target: CharSequence, replacer: Regex.Match => Future[Option[String]])(implicit ctx: scala.concurrent.ExecutionContext): Future[String] = {
      val f: Iterator[Future[Option[String]]] = regex.findAllMatchIn(target).map(replacer)

      Future.sequence(f).map( it2 => {
        regex.replaceSomeIn(target, y => it2.next())
      })
    }
  }
}
