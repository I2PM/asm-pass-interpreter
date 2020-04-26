package de.athalis.coreasm.base

object Typedefs {
  type ASMLocation = Seq[Any]

  sealed trait ASMUpdate {
    def location: ASMLocation
  }

  sealed trait UpdateResult
  case object UpdateStored extends UpdateResult
  case object UpdateFailed extends UpdateResult // TODO: catch failed update in Plugin


  case class SetValue(location: ASMLocation, value: Any) extends ASMUpdate

  case class AddToSet(location: ASMLocation, value: Any) extends ASMUpdate
  case class RemoveFromSet(location: ASMLocation, value: Any) extends ASMUpdate

  /* TODO: these Updates are not implemented as the corresponding CoreASM Updates are not incremental (implementation for Map will come some day, for List may not). Therefore it is safer to use SetValue
  case class AppendToList(location: Location, value: Any) extends ASMUpdate
  case class PrependToList(location: Location, value: Any) extends ASMUpdate
  case class RemoveFromList(location: Location, value: Any) extends ASMUpdate

  case class AddToMap(location: Location, key: Any, value: Any) extends ASMUpdate
  case class RemoveFromMap(location: Location, key: Any) extends ASMUpdate
  */
}
