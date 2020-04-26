package de.athalis.coreasm.plugins.storage.lib

import de.athalis.coreasm.base.Typedefs._

sealed trait AkkaStorageJob

case class ValueRequest(location: ASMLocation) extends AkkaStorageJob
case class ValueReply[T](value: Option[T], startTime: Long, duration: Double)

case class ApplyUpdates(set: Seq[ASMUpdate]) extends AkkaStorageJob

case object AwaitASMStep extends AkkaStorageJob

case object ASMStep
