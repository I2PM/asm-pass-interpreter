package de.athalis.coreasm.binding

import scala.concurrent.Future

import de.athalis.coreasm.base.Typedefs._

trait Binding {
  def load[T](f: DerivedFunction[_]): Option[T]
  def loadAsync[T](f: DerivedFunction[_]): Future[Option[T]]

  def store(updates: Seq[ASMUpdate]): UpdateResult
  def storeAsync(updates: Seq[ASMUpdate]): Future[UpdateResult]
}


trait DerivedFunction[T] {
  def id: String
  def arguments: List[Any] = Nil
  def location: ASMLocation = id :: arguments

  def load()(implicit binding: Binding): Option[T] = binding.load[T](this)
  def loadAsync()(implicit binding: Binding, executor: scala.concurrent.ExecutionContext): Future[Option[T]] = binding.loadAsync[T](this)

  def loadAndGetOrElse(default: T)(implicit binding: Binding): T = this.load().getOrElse(default)
  def loadAndGetOrElseAsync(default: T)(implicit binding: Binding, executor: scala.concurrent.ExecutionContext): Future[T] = this.loadAsync().map(_.getOrElse(default))
}

trait DerivedFunctionMapped[A, B] extends DerivedFunction[B] {
  def mapper: (A => B)

  override def load()(implicit binding: Binding): Option[B] = binding.load[A](this).map(mapper)
  override def loadAsync()(implicit binding: Binding, executor: scala.concurrent.ExecutionContext): Future[Option[B]] = binding.loadAsync[A](this).map(_.map(mapper))
}

trait DerivedSetFunction[T] extends DerivedFunction[Set[T]] {
  def loadAndGet()(implicit binding: Binding): Set[T] = this.load().getOrElse(Set.empty[T])
  def loadAndGetAsync()(implicit binding: Binding, executor: scala.concurrent.ExecutionContext): Future[Set[T]] = this.loadAsync().map(_.getOrElse(Set.empty[T]))
}

trait DerivedSeqFunction[T] extends DerivedFunction[Seq[T]] {
  def loadAndGet()(implicit binding: Binding): Seq[T] = this.load().getOrElse(Seq.empty[T])
  def loadAndGetAsync()(implicit binding: Binding, executor: scala.concurrent.ExecutionContext): Future[Seq[T]] = this.loadAsync().map(_.getOrElse(Seq.empty[T]))
}

trait DerivedMapFunction[K, V] extends DerivedFunction[Map[K, V]] {
  def loadAndGet()(implicit binding: Binding): Map[K, V] = this.load().getOrElse(Map.empty[K, V])
  def loadAndGetAsyc()(implicit binding: Binding, executor: scala.concurrent.ExecutionContext): Future[Map[K, V]] = this.loadAsync().map(_.getOrElse(Map.empty[K, V]))
}
