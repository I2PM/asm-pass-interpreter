package de.athalis.pass.processmodel.operation

import de.athalis.pass.processmodel.PASSProcessModel
import de.athalis.pass.processmodel.PASSProcessModelCollection

import java.nio.file.Path

trait PASSProcessModelWriter[T <: PASSProcessModel] {
  def write(processModels: PASSProcessModelCollection[T], outDir: Path): Set[Path]
}
