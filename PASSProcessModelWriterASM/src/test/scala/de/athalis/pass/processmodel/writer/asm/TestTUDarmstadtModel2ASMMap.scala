package de.athalis.pass.processmodel.writer.asm

import de.athalis.pass.processmodel.tudarmstadt.Tau

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class TestTUDarmstadtModel2ASMMap extends AnyFunSuite with Matchers {

  test("getFunctionName: Tau") {
    TUDarmstadtModel2ASMMap.getFunctionName(Tau) shouldBe Some("Tau")
  }

}
