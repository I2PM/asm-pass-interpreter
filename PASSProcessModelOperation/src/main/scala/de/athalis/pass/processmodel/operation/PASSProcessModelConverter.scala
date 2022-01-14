package de.athalis.pass.processmodel.operation

import de.athalis.pass.processmodel.PASSProcessModel
import de.athalis.pass.processmodel.PASSProcessModelCollection

trait PASSProcessModelConverter[A <: PASSProcessModel, +B <: PASSProcessModel] {

  def convert(processModels: PASSProcessModelCollection[A]): PASSProcessModelCollection[B]

}

trait PASSProcessModelConverterSingle[A <: PASSProcessModel, +B <: PASSProcessModel] extends PASSProcessModelConverter[A, B] {

  def convertSingle(processModel: A): B

  override def convert(processModels: PASSProcessModelCollection[A]): PASSProcessModelCollection[B] = {
    PASSProcessModelCollection.of(processModels.getProcessModels.map(convertSingle))
  }

}
