package de.athalis.pass.processmodel.operation

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

class PASSProcessModelOperationSettings(
                                         val failOnWarning: Boolean,
                                       ) {

  def this(config: Config = ConfigFactory.load()) = {
    this(
      failOnWarning = config.getBoolean("pass.fail-on-warning"),
    )
  }

  def copyPASSProcessModelOperationSettings(
                                             failOnWarning: Boolean = this.failOnWarning,
                                           ): PASSProcessModelOperationSettings = {
    new PASSProcessModelOperationSettings(
      failOnWarning = failOnWarning,
    )
  }

}

class PASSProcessModelConverterSettings(
                                         override val failOnWarning: Boolean,
                                       ) extends PASSProcessModelOperationSettings(failOnWarning) {

  def this(config: Config = ConfigFactory.load()) = {
    this(
      failOnWarning = config.getBoolean("pass.converter.fail-on-warning"),
    )
  }

  def copyPASSProcessModelConverterSettings(
                                             failOnWarning: Boolean = failOnWarning,
                                           ): PASSProcessModelConverterSettings = {
    new PASSProcessModelConverterSettings(
      failOnWarning = failOnWarning,
    )
  }

}

class PASSProcessModelReaderSettings(
                                      override val failOnWarning: Boolean,
                                    ) extends PASSProcessModelOperationSettings(failOnWarning) {

  def this(config: Config = ConfigFactory.load()) = {
    this(
      failOnWarning = config.getBoolean("pass.parser.fail-on-warning"),
    )
  }

  def copyPASSProcessModelReaderSettings(
                                          failOnWarning: Boolean = this.failOnWarning,
                                        ): PASSProcessModelReaderSettings = {
    new PASSProcessModelReaderSettings(
      failOnWarning = failOnWarning,
    )
  }

}

class PASSProcessModelWriterSettings(
                                      override val failOnWarning: Boolean,
                                    ) extends PASSProcessModelOperationSettings(failOnWarning) {

  def this(config: Config = ConfigFactory.load()) = {
    this(
      failOnWarning = config.getBoolean("pass.writer.fail-on-warning"),
    )
  }

  def copyPASSProcessModelWriterSettings(
                                          failOnWarning: Boolean = failOnWarning,
                                        ): PASSProcessModelWriterSettings = {
    new PASSProcessModelWriterSettings(
      failOnWarning = failOnWarning,
    )
  }

}
