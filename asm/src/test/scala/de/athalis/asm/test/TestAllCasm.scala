// SPDX-License-Identifier: AFL-3.0
/*
 * TestAllCasm.scala
 *
 * Licensed under the Academic Free License version 3.0
 *   http://www.opensource.org/licenses/afl-3.0.php
 *
 * Original source: https://github.com/CoreASM/coreasm.core/blob/3ad49542aa1776f92a14a35d6b7a8f0693e9c4eb/org.coreasm.engine/test/org/coreasm/engine/test/TestAllCasm.java
 *
 * Translated from Java/JUnit to Scala/ScalaTest by André Wolski
 */

package de.athalis.asm.test

import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileFilter
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.io.PrintStream
import java.net.URL
import java.util.Properties
import java.util.regex.Pattern

import org.scalatest._
import org.coreasm.util.Tools
import org.coreasm.engine.{Engine, EngineProperties}
import org.coreasm.engine.test.TestEngineDriver
import de.athalis.asm.test.Util._

object TestAllCasm {
  def getFilteredOutput(file: File, filter: String): Seq[String] = {
    var filteredOutputList = Seq.empty[String]
    val pattern = Pattern.compile(filter + ".*")

    try {
      val input = new BufferedReader(new FileReader(file))
      var line: String = null //not declared within while loop

      while ({line = input.readLine(); line != null}) {
        val matcher = pattern.matcher(line)
        if (matcher.find()) {
          val first = line.indexOf("\"", matcher.start) + 1
          val last = line.indexOf("\"", first)
          if (last > first) {
            filteredOutputList +:= Tools.convertFromEscapeSequence(line.substring(first, last))
          }
        }
      }

      input.close();
    }
    catch {
      case e: FileNotFoundException => e.printStackTrace();
      case e: IOException => e.printStackTrace();
    }

    filteredOutputList
  }

  def getParameter(file: File, name: String): Option[Int] = {
    var value: Option[Int] = None
    val pattern = Pattern.compile("@" + name + "\\s*(\\d+)")

    try {
      val input = new BufferedReader(new FileReader(file))
      var line: String = null
      var abort = false
      while (!abort && {line = input.readLine(); line != null}) {
        val matcher = pattern.matcher(line)
        if (matcher.find()) {
          value = Some(Integer.parseInt(matcher.group(1)))
          abort = true
        }
      }
      input.close();
    }
    catch {
      case e: FileNotFoundException => e.printStackTrace();
      case e: IOException => e.printStackTrace();
    }

    value
  }

  private val fileFilterCoreASM = new FileFilter() {
    override def accept(file: File): Boolean = {
      if (file.isDirectory) {
        false
      }
      else {
        val lowerName = file.getName.toLowerCase
        (lowerName.endsWith(".casm") || lowerName.endsWith(".coreasm"))
      }
    }
  }

  def findTestFiles(base: File, folder: String): Seq[File] = {
    if (base == null || !base.exists || !base.isDirectory)
        throw new IllegalArgumentException("base does not exist or is no directory: " + base)

    val dir = new File(base, folder)

    if (!dir.exists || !dir.isDirectory)
        throw new IllegalArgumentException("dir does not exist or is no directory: " + dir)

    dir.listFiles(fileFilterCoreASM)
  }
}

//class TestAllCasm extends FunSuite with Matchers {
class TestAllCasm extends FunSuite with Matchers with Checkpoints {
  import TestAllCasm._

  def outFilter(in: String): Boolean = false
  def logFilter(in: String): Boolean = false
  def errFilter(in: String): Boolean = false
  def failOnWarning: Boolean = true

  def getTestFiles: Seq[File] = {
    //setup the test by finding the test specifications
    val url: URL = this.getClass.getClassLoader.getResource(".")

    findTestFiles(new File(url.toURI), this.getClass.getSimpleName)
  }


  private val testFiles = getTestFiles

  test ("initialize") {
    ( testFiles should not be empty ) withMessage ("testFiles: ")
  }

  for (testFile <- testFiles) {
    test(testFile.getName) {
      runSpecification(testFile)
    }
  }

  private def runSpecification(testFile: File): Unit = synchronized {
    val origOutput: java.io.PrintStream = System.out
    val origError: java.io.PrintStream = System.err

    val logStream = new ByteArrayOutputStream()
    val outStream = new ByteArrayOutputStream()
    val errStream = new ByteArrayOutputStream()

    try {
      System.setOut(new PrintStream(logStream))
      System.setErr(new PrintStream(errStream))

      outStream.reset()
      logStream.reset()
      errStream.reset()

      runSpecification(testFile, outStream, logStream, errStream, origOutput)

      outStream.reset()
      logStream.reset()
      errStream.reset()
    }
    finally {
      System.setOut(origOutput)
      System.setErr(origError)
    }
  }

  private def runSpecification(testFile: File, outStream: ByteArrayOutputStream, logStream: ByteArrayOutputStream, errStream: ByteArrayOutputStream, origOutput: PrintStream): Unit = {
    var requiredOutputList = getFilteredOutput(testFile, "@require")
    val refusedOutputList = getFilteredOutput(testFile, "@refuse")
    val minSteps = getParameter(testFile, "minsteps").getOrElse(1)
    val maxSteps = getParameter(testFile, "maxsteps").getOrElse(minSteps)

    val path = testFile.getAbsolutePath


    val properties: Properties = new Properties()
    properties.setProperty(EngineProperties.MAX_PROCESSORS, Runtime.getRuntime.availableProcessors().toString)

    val td = TestEngineDriver.newLaunch(path, Tools.getRootFolder(classOf[Engine]) + "/plugins", properties)

    try {

      td.setOutputStream(new PrintStream(outStream))

      // TODO: isn't this too much, as minSteps are executed each time?
      for (step <- 0 to maxSteps if (step < minSteps || !requiredOutputList.isEmpty)) {

        if (td.getStatus == TestEngineDriver.TestEngineDriverStatus.stopped) {
          (requiredOutputList shouldBe empty) withMessage ("output:\n" + outStream.toString + "\n\nerrors:\n" + errStream.toString + "\n\nEngine terminated after " + step + " steps, but is missing required output: ")
          (step should be >= minSteps) withMessage ("output:\n" + outStream.toString + "\n\nerrors:\n" + errStream.toString + "\n\nEngine terminated after " + step + " steps: ")
        }

        td.executeSteps(1)

        val outputOut = outStream.toString
        val outputLog = logStream.toString
        val outputErr = errStream.toString

        for (line <- outputOut.linesIterator.filter(outFilter)) {
          origOutput.println("out: " + line)
        }

        for (line <- outputLog.linesIterator.filter(logFilter)) {
          origOutput.println("log" + line)
        }

        for (line <- outputErr.linesIterator.filter(errFilter)) {
          origOutput.println("err: " + line)
        }


        //check for refused output / errors. report all errors
        val cp = new Checkpoint

        cp {
          //test if no unexpected error has occurred
          var errors: Iterator[String] = outputErr.linesIterator
          errors = errors.filterNot(msg => msg.contains("SLF4J") && msg.contains("binding"))
          (errors.toSeq shouldBe empty) withMessage ("log:\n" + outputLog + "\n\noutput:\n" + outputOut + "\n\nEngine had an error after " + step + " steps: ")
        }

        if (failOnWarning) cp {
          //test if no unexpected warning has occurred
          var warnings = outputLog.linesIterator.filter(_.contains("WARN"))
          warnings = warnings.filterNot(msg => msg.contains("The update was not successful so it might not be added to the universe."))
          warnings = warnings.filterNot(msg => msg.contains("org.coreasm.util.Tools") && msg.toLowerCase.contains("root folder"))
          (warnings.toSeq shouldBe empty) withMessage ("output:\n" + outputOut + "\n\nEngine had an warning after " + step + " steps: ")
        }

        for (refusedOutput <- refusedOutputList) {
          cp {
            outputOut.linesIterator.filter(_.contains(refusedOutput)).toSeq shouldBe empty withMessage ("output: \n" + outputOut)
          }
        }
        cp.reportAll()


        requiredOutputList = requiredOutputList.filterNot { x => outputOut.contains(x) }

        outStream.reset()
        logStream.reset()
        errStream.reset()
      }

      //check if no required output is missing
      (requiredOutputList shouldBe empty) withMessage (outStream.toString + "\n\nremaining required output after " + maxSteps + " maxSteps: ")
    }
    finally {
      td.stop()
    }
  }

}