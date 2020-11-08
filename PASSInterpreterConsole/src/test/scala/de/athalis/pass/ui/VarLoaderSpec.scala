package de.athalis.pass.ui

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.async.Async.async
import akka.util.Timeout
import de.athalis.pass.ui.loading.VarLoader

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers


class VarLoaderSpec extends AnyFunSuite with Matchers with ScalaFutures {
  implicit val timeout: Timeout = Timeout(5.seconds)

  def varLoaderMock(varname: String): Future[String] = async {
    "MOCK[" + varname + "]"
  }

  test("noop1") {
    val x = VarLoader(varLoaderMock, Future { "test" })
    whenReady (x) { y => {
      y shouldBe "test"
    }}
  }

  test("noop2") {
    val x = VarLoader(varLoaderMock, Future { "test foo bar" })
    whenReady (x) { y => {
      y shouldBe "test foo bar"
    }}
  }

  test("single") {
    val x = VarLoader(varLoaderMock, Future { "$test" })
    whenReady (x) { y => {
      y shouldBe "MOCK[test]"
    }}
  }

  test("special chars") {
    val x = VarLoader(varLoaderMock, Future { "$test-foo $test_bar" })
    whenReady (x) { y => {
      y shouldBe "MOCK[test-foo] MOCK[test_bar]"
    }}
  }

  test("double") {
    val x = VarLoader(varLoaderMock, Future { "$test $foo" })
    whenReady (x) { y => {
      y shouldBe "MOCK[test] MOCK[foo]"
    }}
  }

  test("complete") {
    val x = VarLoader(varLoaderMock, Future { "Hello $test $foo bar" })
    whenReady (x) { y => {
      y shouldBe "Hello MOCK[test] MOCK[foo] bar"
    }}
  }

  test("escape single") {
    val x = VarLoader(varLoaderMock, Future { """\$test""" })
    whenReady (x) { y => {
      y shouldBe "$test"
    }}
  }

  test("escape between") {
    val x = VarLoader(varLoaderMock, Future { """$test \$foo $bar""" })
    whenReady (x) { y => {
      y shouldBe "MOCK[test] $foo MOCK[bar]"
    }}
  }
}
