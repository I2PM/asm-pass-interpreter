package de.athalis.asm.test

import scala.language.implicitConversions

import org.scalatest.Matchers._

object Util {
  class AssertionHolder(f: => Any) {
    def withMessage(s: String): Any = {
      withClue(s) { f }
    }
  }

  implicit def convertAssertion(f: => Any): AssertionHolder = new AssertionHolder(f)
}
