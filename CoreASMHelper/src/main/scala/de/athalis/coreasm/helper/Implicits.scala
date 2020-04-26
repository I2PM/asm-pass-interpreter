package de.athalis.coreasm.helper

import de.athalis.coreasm.base.Typedefs._

import scala.collection.JavaConverters._

import org.coreasm.engine.interpreter.{ASTNode, Interpreter, ScannerInfo}
import org.coreasm.engine.absstorage._

import org.coreasm.engine.plugins.number.NumberElement
import org.coreasm.engine.plugins.string.StringElement
import org.coreasm.engine.plugins.set.{ SetPlugin, SetElement }
import org.coreasm.engine.plugins.list.ListElement
import org.coreasm.engine.plugins.map.MapElement

import java.util.{ Set => JavaSet, Map => JavaMap, List => JavaList }

object Implicits {


  def L(l: ASMLocation): Location = new Location(l.head.asInstanceOf[String], l.tail.map(E).asJava)



  def E(x: Any): Element = x match {
    case None         => Element.UNDEF
    case x: String    => E(x)
    case x: Double    => E(x)
    case x: Int       => E(x)
    case x: Long      => E(x)
    case x: Boolean   => E(x)
    case x: Set[_]    => E(x)
    case x: Seq[_]    => E(x)
    case x: Map[_, _] => E(x)
    case x: JavaSet[_]    => E(x)
    case x: JavaMap[_, _] => E(x)
    case x: JavaList[_]   => E(x)
    case x: Any       => throw new scala.NotImplementedError("an implementation is missing for '" + x + "' (" + x.getClass + ")")
  }

  def E(x: String): StringElement     = new StringElement(x)
  def E(x: Double): NumberElement     = NumberElement.getInstance(x)
  def E(x: Int): NumberElement        = NumberElement.getInstance(x.toDouble)
  def E(x: Long): NumberElement       = NumberElement.getInstance(x.toDouble)
  def E(x: Boolean): BooleanElement   = BooleanElement.valueOf(x)
  def E(x: Set[_]): SetElement        = new SetElement(x.map(E).asJava)
  def E(x: Seq[_]): ListElement       = new ListElement(x.map(E).asJava)
  def E(x: Map[_, _]): MapElement     = new MapElement(x.map({ case (k, v) => (E(k), E(v)) }).asJava)

  def E[T](x: JavaSet[T]): SetElement       = E(x.asScala.toSet)
  def E[A, B](x: JavaMap[A, B]): MapElement = E(x.asScala.toMap)
  def E[T](x: JavaList[T]): ListElement     = E(x.asScala.toSeq)


  def V(x: Element): Any = x match {
    case Element.UNDEF     => None
    case x: StringElement  => V(x)
    case x: NumberElement  => V(x)
    case x: BooleanElement => V(x)
    case x: SetElement     => V(x)
    case x: ListElement    => V(x)
    case x: MapElement     => V(x)
    case x: Any => throw new scala.NotImplementedError("an implementation is missing for '" + x + "' (" + x.getClass + ")")
  }

  def V(x: StringElement): String     = x.getValue
  def V(x: NumberElement): Double     = x.getValue
  def V(x: BooleanElement): Boolean   = x.getValue
  def V(x: SetElement): Set[Any]      = x.getSet.asScala.toSet[Element].map(V)
  def V(x: ListElement): Seq[Any]     = x.getList.asScala.toSeq.map(V)
  def V(x: MapElement): Map[Any, Any] = x.getMap.asScala.toMap[Element, Element].map({ case (k, v) => (V(k), V(v)) })


  def U(au: ASMUpdate)(implicit interpreter: Interpreter, node: ASTNode): Update = {
    U(au, interpreter.getSelf, node.getScannerInfo)
  }

  def U(au: ASMUpdate, self: Element, scannerInfo: ScannerInfo): Update = au match {
    case u: SetValue => U(u, self, scannerInfo)
    case u: AddToSet => U(u, self, scannerInfo)
    case u: RemoveFromSet => U(u, self, scannerInfo)
  }

  private def U(u: SetValue, self: Element, scannerInfo: ScannerInfo)
    = new Update(L(u.location), E(u.value), Update.UPDATE_ACTION, self, scannerInfo)

  private def U(u: AddToSet, self: Element, scannerInfo: ScannerInfo)
    = new Update(L(u.location), E(u.value), SetPlugin.SETADD_ACTION, self, scannerInfo)

  private def U(u: RemoveFromSet, self: Element, scannerInfo: ScannerInfo)
    = new Update(L(u.location), E(u.value), SetPlugin.SETREMOVE_ACTION, self, scannerInfo)
}
