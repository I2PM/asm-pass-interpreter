package de.athalis.coreasm.helper

import org.coreasm.engine.absstorage._
import org.coreasm.engine.plugins.number.NumberElement
import org.coreasm.engine.plugins.string.StringElement
import org.coreasm.engine.plugins.set.SetElement
import org.coreasm.engine.plugins.list.ListElement
import org.coreasm.engine.plugins.map.MapElement

import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.propspec.AnyPropSpec

class ImplicitsTableSpec extends AnyPropSpec with TableDrivenPropertyChecks with Matchers {
  import Implicits._

  val nestedScala: Seq[Any] = Seq[Any](Set("a", 1), Map(5 -> "x"))

  val nestedSet = new java.util.HashSet[Element]()
  nestedSet.add(E("a"))
  nestedSet.add(E(1))
  val nestedSetElement = new SetElement(nestedSet)

  val nestedMap = new java.util.HashMap[Element, Element]()
  nestedMap.put(E(5), E("x"))
  val nestedMapElement = new MapElement(nestedMap)

  val nestedList = new java.util.LinkedList[Element]()
  nestedList.add(nestedSetElement)
  nestedList.add(nestedMapElement)

  val nestedElement = new ListElement(nestedList)

  val scalaExamples =
    Table(
      ("Scala", "Element"),
      (None,    Element.UNDEF),
      ("x",     new StringElement("x")),
      (1.3,     NumberElement.getInstance(1.3)),
      (true,    BooleanElement.valueOf(true)),
      (Set(),   new SetElement()),
      (Seq(),   new ListElement()),
      (Map(),   new MapElement()),
      (nestedScala, nestedElement)
    )

  val javaExamples =
    Table(
      ("Java",                  "Element"),
      (new java.util.HashSet(), new SetElement()),
      (new java.util.HashSet(), new ListElement()), // TODO: why does this work?
      (new java.util.HashSet(), new MapElement())
    )

  property("E should convert Scala to Elements") {
    forAll(scalaExamples) { case (a, b) =>
      E(a) shouldBe b
    }
  }

  property("E should convert Java Collections to Elements") {
    forAll(javaExamples) { case (a, b) =>
      E(a) shouldBe b
    }
  }

  property("V should convert to Scala") {
    forAll(scalaExamples) { case (a, b) =>
      V(b) shouldBe a
    }
  }

  property("L should convert to Location") {
    val location = Seq("A", "a", 2)

    val args = new java.util.LinkedList[Element]()
    args.add(E("a"))
    args.add(E(2))

    L(location) shouldBe new Location("A", args)
  }
}
