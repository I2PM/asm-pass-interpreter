package de.athalis.pass.processmodel.interface

import de.athalis.pass.processmodel.interface.context.AnalysisException
import de.athalis.pass.processmodel.parser.ast.PASSProcessModelReaderAST
import de.athalis.pass.processmodel.tudarmstadt.Process
import de.athalis.pass.processmodel.writer.asm.TUDarmstadtModel2ASMMap

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.io.StringReader

class ModalJoinTests extends AnyFunSuite with Matchers {

  private val process1 =
    """
      |Process Example_Split {
      |    Subject A {
      |        StartSubject := true
      |
      |        Macro Main {
      |            START: InternalAction {
      |                "split" -> Split
      |                "terminate" -> TERMINATE
      |            }
      |
      |            Split: "ModalSplit" {
      |                -> a1
      |                -> a2
      |            }
      |
      |            a1: InternalAction "a1" -> Join
      |            a2: InternalAction "a2" -> Join
      |
      |            Join: "ModalJoin" -> b2
      |            b2: InternalAction "b2" -> START
      |        }
      |    }
      |}
      |
    """.stripMargin

  // join without split
  private val process2 =
    """
      |Process Example_Split {
      |    Subject A {
      |        StartSubject := true
      |
      |        Macro Main {
      |            START: InternalAction {
      |                "join" -> Join
      |            }
      |
      |            Join: "ModalJoin" -> TERMINATE
      |        }
      |    }
      |}
      |
    """.stripMargin

  // different splits to same join
  private val process3 =
    """
      |Process Example_Split {
      |    Subject A {
      |        StartSubject := true
      |
      |        Macro Main {
      |            START: "ModalSplit" {
      |                -> Join
      |                -> Split2
      |            }
      |
      |            Split2: "ModalSplit" {
      |                -> Join
      |                -> TERMINATE // Split without Join is fine
      |            }
      |
      |            Join: "ModalJoin" -> TERMINATE
      |        }
      |    }
      |}
      |
    """.stripMargin


  test("modal join without split") {
    val caught = intercept[AnalysisException] {
      PASSProcessModelReaderInterface.readProcessModels(new StringReader(process2), "process2", PASSProcessModelReaderAST).getProcessModels
    }
    caught.getMessage should startWith ("ModalJoin: join without split")
  }

  test("modal join from different splits") {
    val caught = intercept[AnalysisException] {
      PASSProcessModelReaderInterface.readProcessModels(new StringReader(process3), "process3", PASSProcessModelReaderAST).getProcessModels
    }
    caught.getMessage should startWith ("ModalJoin: different splits leading to state")
  }

  test("modal join has correct arguments injected") {
    val processModels: Set[Process] = PASSProcessModelReaderInterface.readProcessModels(new StringReader(process1), "process1", PASSProcessModelReaderAST).getProcessModels

    processModels should have size 1

    val p: Process = processModels.head

    val m = TUDarmstadtModel2ASMMap.toMap(p)

    val states = m("states").asInstanceOf[Map[Int, Map[String, Any]]]

    val nO: Option[Map[String, Any]] = states.values.find(_("ID") == "Join")

    nO shouldBe defined

    val n = nO.get

    n.keys should contain("function")
    n("function") shouldBe "ModalJoin"

    n.keys should contain("functionArguments")
    n("functionArguments") shouldBe Seq(2)
  }
}
