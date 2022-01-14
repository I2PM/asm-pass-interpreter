package de.athalis.pass.processutil

import de.athalis.pass.processmodel.PASSProcessModelCollection
import de.athalis.pass.processmodel.tudarmstadt.Process

import java.io.Reader
import java.nio.file.Path
import java.nio.file.PathMatcher

package object base {

  trait PASSProcessModelReader {
    def getPathMatcher: PathMatcher
    def canReadPath(path: Path): Boolean

    def readProcessModels(paths: Set[Path]): PASSProcessModelCollection[Process]
    def readProcessModels(reader: Reader, sourceName: String): PASSProcessModelCollection[Process]
  }

  trait PASSProcessModelWriter {
    def write(processModels: PASSProcessModelCollection[Process], outDir: Path): Set[Path]
  }

}
