package de.athalis.pass.processutil

import de.athalis.pass.parser.PASSProcessReaderAST
import de.athalis.pass.processutil.context.InvalidProcessException
import org.scalatest.{FunSuite, Matchers}

class CompleteProcessTests extends FunSuite with Matchers {

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
      |            START: Send ["foo" to * of B] -> END
      |        }
      |    }
      |}
      |
    """.stripMargin


  test("StateConnectionsAnalysis1") {
    val caught = intercept[InvalidProcessException] {
      PASSProcessReaderUtil.readProcesses(process1, "process1", PASSProcessReaderAST)
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
      PASSProcessReaderUtil.readProcesses(process2, "process2", PASSProcessReaderAST)
    }
    caught.getMessage should startWith ("MessageSubjectCountAnalysis:")
    caught.getMessage should include ("subjectVar of transition '' is not defined, although the `*` operator was used")
    caught.getMessage should include ("state 'START'")
    caught.getMessage should include ("subject 'BBBB'")
    caught.getMessage should include ("process 'Example_InvalidTransition'")
  }
}
