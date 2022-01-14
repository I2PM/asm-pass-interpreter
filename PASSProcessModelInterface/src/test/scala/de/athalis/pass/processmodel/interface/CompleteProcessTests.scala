package de.athalis.pass.processmodel.interface

import de.athalis.pass.processmodel.interface.context.InvalidProcessException
import de.athalis.pass.processmodel.parser.ast.PASSProcessModelReaderAST

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.io.StringReader

class CompleteProcessTests extends AnyFunSuite with Matchers {

  private val process1 =
    """
      |Process Example_InvalidTransition {
      |    Subject AAAA {
      |        Macro Main {
      |            START: InternalAction "foo" -> DoesNotExist
      |        }
      |    }
      |}
      |
    """.stripMargin

  private val process2 =
    """
      |Process Example_InvalidTransition {
      |    Subject BBBB {
      |        Macro Main {
      |            START: Send ["foo" to * of B] -> TERMINATE
      |        }
      |    }
      |}
      |
    """.stripMargin


  test("StateConnectionsAnalysis1") {
    val caught = intercept[InvalidProcessException] {
      PASSProcessModelReaderInterface.readProcessModels(new StringReader(process1), "process1", PASSProcessModelReaderAST).getProcessModels
    }
    caught.getMessage should startWith ("StateConnectionsAnalysis:")
    caught.getMessage should include ("destination 'DoesNotExist'")
    caught.getMessage should include ("transition 'foo'")
    caught.getMessage should include ("state 'START'")
    caught.getMessage should include ("subject 'AAAA'")
    caught.getMessage should include ("process 'Example_InvalidTransition'")
  }

  test("MessageSubjectCountAnalysis1") {
    val caught = intercept[InvalidProcessException] {
      PASSProcessModelReaderInterface.readProcessModels(new StringReader(process2), "process2", PASSProcessModelReaderAST).getProcessModels
    }
    caught.getMessage should startWith ("MessageSubjectCountAnalysis:")
    caught.getMessage should include ("subjectVar of transition 'None (foo to/from B)' is not defined, although the `*` operator was used")
    caught.getMessage should include ("state 'START'")
    caught.getMessage should include ("subject 'BBBB'")
    caught.getMessage should include ("process 'Example_InvalidTransition'")
  }
}
