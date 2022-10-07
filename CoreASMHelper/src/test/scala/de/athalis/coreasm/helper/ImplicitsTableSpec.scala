package de.athalis.coreasm.helper

import org.coreasm.engine.absstorage._
import org.coreasm.engine.plugins.list.ListElement
import org.coreasm.engine.plugins.map.MapElement
import org.coreasm.engine.plugins.number.NumberElement
import org.coreasm.engine.plugins.set.SetElement
import org.coreasm.engine.plugins.string.StringElement
import org.coreasm.network.plugins.graph.ToGraphFunctionElement

import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleGraph

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

  // JGraphT
  val graph = new SimpleGraph[String, DefaultEdge](classOf[DefaultEdge])
  graph.addVertex("A")
  graph.addVertex("B")
  graph.addEdge("A", "B")


  val verticesElement = new ListElement(java.util.List.of(Seq(
    new StringElement("A"),
    new StringElement("B"),
  ): _*))

  val edgesElement = new ListElement(java.util.List.of(Seq(
    new ListElement(java.util.List.of(Seq(
      new StringElement("A"),
      new StringElement("B"),
    ): _*))
  ): _*))

  val graphElement = new ToGraphFunctionElement().getValue(java.util.List.of(verticesElement, edgesElement))


  val scalaExamples =
    Table(
      ("Scala", "Element"),
      (None,    Element.UNDEF),
      ("x",     new StringElement("x")),
      (1,       NumberElement.getInstance(1.0)),
      (1L,      NumberElement.getInstance(1.0)),
      (1.3,     NumberElement.getInstance(1.3)),
      (true,    BooleanElement.valueOf(true)),
      (Set(),   new SetElement()),
      (Seq(),   new ListElement()),
      (Map(),   new MapElement()),
      (nestedScala, nestedElement)
    )

  val javaExamplesPositive =
    Table(
      ("Java",                    "Element"),
      (new java.util.HashSet(),   new SetElement()),
      (new java.util.ArrayList(), new ListElement()),
      (new java.util.HashMap(),   new MapElement()),
    )

  // NOTE: some of these work, some not. That's just how the CoreASM collections are...
  val javaExamplesNegative =
    Table(
      ("Java",                  "Element"),
      (new java.util.HashSet(), new ListElement()),
      (new java.util.HashSet(), new MapElement()),

      (new java.util.ArrayList(), new SetElement()),
      (new java.util.ArrayList(), new MapElement()),

      (new java.util.HashMap(), new SetElement()),
      (new java.util.HashMap(), new ListElement()),
    )

  val negativeExamples =
    Table(
      ("Object", "is not"),
      (true,     Element.UNDEF),
    )

  val notImplemented =
    Table(
      ("Missing", "Element"),
      (graph,     graphElement),
    )

  property("E should convert Scala to Elements") {
    forAll(scalaExamples) { case (a, b) =>
      E(a) shouldBe b
    }
  }

  property("E should convert Java Collections to Elements") {
    forAll(javaExamplesPositive) { case (a, b) =>
      E(a) shouldBe b
    }
  }

  ignore("E should not convert Java Collections to different Elements") {
    forAll(javaExamplesNegative) { case (a, b) =>
      E(a) should not be (b)
    }
  }

  property("E should not convert Objects to different Elements") {
    forAll(negativeExamples) { case (a, b) =>
      E(a) should not be (b)
    }
  }

  ignore("E should convert more classes") {
    forAll(notImplemented) { case (a, b) =>
      E(a) shouldBe (b)
    }
  }

  property("E should convert more classes, but does not") {
    forAll(notImplemented) { case (a, b) =>
      an[NotImplementedError] should be thrownBy {
        E(a)
      }
    }
  }

  property("V should convert to Scala") {
    forAll(scalaExamples) { case (a, b) =>
      V(b) shouldBe a
    }
  }

  ignore("V should convert more classes") {
    forAll(notImplemented) { case (a, b) =>
      V(b) shouldBe a
    }
  }

  property("V should convert more classes, but does not") {
    forAll(notImplemented) { case (a, b) =>
      an[NotImplementedError] should be thrownBy {
        V(b)
      }
    }
  }

  property("L should convert to Location") {
    val location: Seq[Any] = Seq("A", "a", 2)

    val args = new java.util.LinkedList[Element]()
    args.add(E("a"))
    args.add(E(2))

    L(location) shouldBe new Location("A", args)
  }
}
