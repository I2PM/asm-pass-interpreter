package de.athalis.coreasm.binding

import de.athalis.coreasm.base.Typedefs._

import scala.concurrent.Future

trait Binding {
  def load[T](l: ASMLocation): Option[T]
  def loadAsync[T](l: ASMLocation): Future[Option[T]]

  final def load[T](f: LoadableASMFunction[T, _]): Option[T] = this.load[T](f.location)
  final def loadAsync[T](f: LoadableASMFunction[T, _]): Future[Option[T]] = this.loadAsync[T](f.location)

  def store(updates: Seq[ASMUpdate]): UpdateResult
  def storeAsync(updates: Seq[ASMUpdate]): Future[UpdateResult]
}


object ASMFunction {

  def plain(id: String): ASMFunction = {
    val _id = id
    new ASMFunction {
      override def id: String = _id
    }
  }

  def loadable[T](id: String, arguments: List[Any] = Nil): LoadableASMFunction[T, T] = ASMFunction.loadable[T, T](id, arguments, x => x)
  def loadableInt[T <: Int](id: String, arguments: List[Any] = Nil): LoadableASMFunction[Double, Int] = ASMFunction.loadable[Double, Int](id, arguments, x => x.toInt)
  def loadable[A, B](id: String, arguments: List[Any], mapper: (A => B)): LoadableASMFunction[A, B] = {
    val _id = id
    val _arguments = arguments
    val _mapper = mapper
    new LoadableASMFunction[A, B] {
      override def id: String = _id
      override def arguments: List[Any] = _arguments
      override def mapper: (A => B) = _mapper
    }
  }

  def gettable[T](id: String, arguments: List[Any] = Nil, defaultValue: T): GettableASMFunction[T, T] = ASMFunction.gettable[T, T](id, arguments, defaultValue, x => x)
  def gettable[A, B](id: String, arguments: List[Any], defaultValue: B, mapper: (A => B)): GettableASMFunction[A, B] = {
    val _id = id
    val _arguments = arguments
    val _defaultValue = defaultValue
    val _mapper = mapper
    new GettableASMFunction[A, B] {
      override def id: String = _id
      override def arguments: List[Any] = _arguments
      override def defaultValue: B = _defaultValue
      override def mapper: (A => B) = _mapper
    }
  }

  def gettableSet[T](id: String, arguments: List[Any] = Nil): GettableASMFunction[Set[T], Set[T]] = ASMFunction.gettable(id, arguments, Set.empty[T], x => x)
  def gettableSet[A, B](id: String, arguments: List[Any], mapper: (Set[A] => Set[B])): GettableASMFunction[Set[A], Set[B]] = ASMFunction.gettable(id, arguments, Set.empty[B], mapper)

  def gettableSeq[T](id: String, arguments: List[Any] = Nil): GettableASMFunction[Seq[T], Seq[T]] = ASMFunction.gettable(id, arguments, Seq.empty[T], x => x)
  def gettableSeq[A, B](id: String, arguments: List[Any], mapper: (Seq[A] => Seq[B])): GettableASMFunction[Seq[A], Seq[B]] = ASMFunction.gettable(id, arguments, Seq.empty[B], mapper)

  def gettableMap[K, V](id: String, arguments: List[Any] = Nil): GettableASMFunction[Map[K, V], Map[K, V]] = ASMFunction.gettable(id, arguments, Map.empty[K, V], x => x)
  def gettableMap[A_K, A_V, B_K, B_V](id: String, arguments: List[Any], mapper: (Map[A_K, A_V] => Map[B_K, B_V])): GettableASMFunction[Map[A_K, A_V], Map[B_K, B_V]] = ASMFunction.gettable(id, arguments, Map.empty[B_K, B_V], mapper)

}

trait ASMFunction {
  def id: String
  def locationOf(arguments: List[Any]): ASMLocation = id :: arguments
}

trait LoadableASMFunction[A, B] extends ASMFunction {
  def arguments: List[Any]
  def location: ASMLocation = this.locationOf(arguments)
  def mapper: (A => B)

  def load()(implicit binding: Binding): Option[B] = binding.load[A](this).map(mapper)
  def loadAsync()(implicit binding: Binding, executor: scala.concurrent.ExecutionContext): Future[Option[B]] = binding.loadAsync[A](this).map(_.map(mapper))

  def loadAndGetOrElse(default: B)(implicit binding: Binding): B = this.load().getOrElse(default)
  def loadAndGetOrElseAsync(default: B)(implicit binding: Binding, executor: scala.concurrent.ExecutionContext): Future[B] = this.loadAsync().map(_.getOrElse(default))
}

trait GettableASMFunction[A, B] extends LoadableASMFunction[A, B] {
  def defaultValue: B

  def loadAndGet()(implicit binding: Binding): B = this.loadAndGetOrElse(defaultValue)
  def loadAndGetAsync()(implicit binding: Binding, executor: scala.concurrent.ExecutionContext): Future[B] = this.loadAndGetOrElseAsync(defaultValue)
}
