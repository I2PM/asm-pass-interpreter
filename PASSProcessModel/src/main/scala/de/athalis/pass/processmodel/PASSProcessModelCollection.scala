package de.athalis.pass.processmodel

abstract class PASSProcessModelCollection[+T <: PASSProcessModel] {

  def getProcessModels[X >: T]: Set[X]

}

class PASSProcessModelCollectionImpl[+T <: PASSProcessModel](of: Set[T]) extends PASSProcessModelCollection[T] {

  override def getProcessModels[X >: T]: Set[X] = of.asInstanceOf[Set[X]]

  override def toString: String = "PASSProcessModelCollection" + of.mkString("(", ", ", ")")

}

object PASSProcessModelCollection {

  private val emptyInstance: PASSProcessModelCollection[PASSProcessModel] = new PASSProcessModelCollectionImpl(Set.empty[PASSProcessModel])
  def empty[T <: PASSProcessModel]: PASSProcessModelCollection[T] = emptyInstance.asInstanceOf[PASSProcessModelCollection[T]]

  def apply[T <: PASSProcessModel](args: T*): PASSProcessModelCollection[T] = {
    new PASSProcessModelCollectionImpl(args.toSet)
  }

  def of[T <: PASSProcessModel](x: Set[T]): PASSProcessModelCollection[T] = new PASSProcessModelCollectionImpl(x)

  def flatten[T <: PASSProcessModel](x: Set[PASSProcessModelCollection[T]]): PASSProcessModelCollection[T] = {
    if (x.size == 1) {
      x.head
    }
    else {
      val nested: Set[Set[T]] = x.map(_.getProcessModels)
      new PASSProcessModelCollectionImpl(nested.flatten)
    }
  }

}
