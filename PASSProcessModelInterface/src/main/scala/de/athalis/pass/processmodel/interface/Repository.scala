package de.athalis.pass.processmodel.interface

import de.athalis.pass.processmodel.operation.PASSProcessModelReader
import de.athalis.pass.processmodel.operation.PASSProcessModelWriter
import de.athalis.pass.processmodel.parser.ast.PASSProcessModelReaderAST
import de.athalis.pass.processmodel.parser.graphml.PASSProcessModelReaderGraphML
import de.athalis.pass.processmodel.tudarmstadt.Process
import de.athalis.pass.processmodel.writer.asm.PASSProcessModelWriterASM

import java.nio.file.PathMatcher

private[interface] object Repository {

  private[interface] val readers: Map[String, PASSProcessModelReader[Process]] = Map(
    "ast"     -> PASSProcessModelReaderAST,
    "graphml" -> PASSProcessModelReaderGraphML,
  )

  private[interface] val pathMatchers: Set[PathMatcher] = readers.values.map(_.getPathMatcher).toSet

  private[interface] val writers: Map[String, PASSProcessModelWriter[Process]] = Map(
    "asm" -> PASSProcessModelWriterASM,
  )

}
