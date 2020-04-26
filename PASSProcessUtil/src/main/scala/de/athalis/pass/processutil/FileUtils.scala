package de.athalis.pass.processutil

import java.io.File

object FileUtils {

  def listFiles(fileIn: File): Set[File] = {
    if (fileIn.isDirectory) {
      fileIn.listFiles(processFileFilter).toSet
    }
    else {
      Set(fileIn)
    }
  }

  object processFileFilter extends java.io.FilenameFilter {
    override def accept(arg0: File, arg1: String): Boolean = {
      PASSProcessReaderUtil.fileExtensions.exists(arg1.endsWith)
    }
  }

}
