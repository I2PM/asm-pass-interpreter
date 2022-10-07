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

import de.athalis.asm.test.Util._

import org.coreasm.engine.Engine
import org.coreasm.engine.EngineProperties
import org.coreasm.engine.test.TestEngineDriver
import org.coreasm.util.Tools

import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.PrintStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import java.util.regex.Pattern

import scala.collection.JavaConverters._

object TestAllCasm {
  def getFilteredOutput(path: Path, filter: String): Seq[String] = {
    var filteredOutputList = Seq.empty[String]
    val pattern = Pattern.compile(filter + ".*")

    try {
      val input = Files.newBufferedReader(path)
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

      // TODO: ... finally ...
      input.close()
    }
    catch {
      case e: IOException => e.printStackTrace();
    }

    filteredOutputList
  }

  def getParameter(path: Path, name: String): Option[Int] = {
    var value: Option[Int] = None
    val pattern = Pattern.compile("@" + name + "\\s*(\\d+)")

    try {
      val input = Files.newBufferedReader(path)
      var line: String = null
      var abort = false
      while (!abort && {line = input.readLine(); line != null}) {
        val matcher = pattern.matcher(line)
        if (matcher.find()) {
          value = Some(Integer.parseInt(matcher.group(1)))
          abort = true
        }
      }

      // TODO: ... finally ...
      input.close()
    }
    catch {
      case e: IOException => e.printStackTrace();
    }

    value
  }

  def findTestFiles(base: Path, folder: String): Set[Path] = {
    if (base == null || !Files.exists(base) || !Files.isDirectory(base))
        throw new IllegalArgumentException("base does not exist or is no directory: " + base)

    val dir = base.resolve(folder)

    if (!Files.exists(dir) || !Files.isDirectory(dir))
        throw new IllegalArgumentException("dir does not exist or is no directory: " + dir)

    val ds = Files.newDirectoryStream(dir, "*.{casm,coreasm}")
    val r = ds.asScala.toSet
    ds.close()
    r
  }
}

//class TestAllCasm extends FunSuite with Matchers {
class TestAllCasm extends AnyFunSuite with Matchers with Checkpoints {
  import TestAllCasm._

  def outFilter(in: String): Boolean = false
  def logFilter(in: String): Boolean = false
  def errFilter(in: String): Boolean = false
  def failOnWarning: Boolean = true

  def getTestFiles: Set[Path] = {
    //setup the test by finding the test specifications
    val url: URL = this.getClass.getClassLoader.getResource(".")

    findTestFiles(Path.of(url.toURI), this.getClass.getSimpleName)
  }

  private val testFiles = getTestFiles

  test ("initialize") {
    ( testFiles should not be empty ) withMessage ("testFiles: ")
  }

  for (testFile <- testFiles) {
    test(testFile.getFileName.toString) {
      runSpecification(testFile)
    }
  }

  private def runSpecification(testFile: Path): Unit = synchronized {
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

  private def runSpecification(testFile: Path, outStream: ByteArrayOutputStream, logStream: ByteArrayOutputStream, errStream: ByteArrayOutputStream, origOutput: PrintStream): Unit = {
    var requiredOutputList = getFilteredOutput(testFile, "@require")
    val refusedOutputList = getFilteredOutput(testFile, "@refuse")
    val minSteps = getParameter(testFile, "minsteps").getOrElse(1)
    val maxSteps = getParameter(testFile, "maxsteps").getOrElse(minSteps)

    val properties: Properties = new Properties()
    properties.setProperty(EngineProperties.MAX_PROCESSORS, Runtime.getRuntime.availableProcessors().toString)

    val td = TestEngineDriver.newLaunch(testFile.toRealPath().toString, Tools.getRootFolder(classOf[Engine]) + "/plugins", properties)

    try {

      td.setOutputStream(new PrintStream(outStream))

      val startTime = System.nanoTime()

      // TODO: isn't this too much, as minSteps are executed each time?
      for (step <- 0 to maxSteps if (step < minSteps || requiredOutputList.nonEmpty)) {

        if (td.getStatus == TestEngineDriver.TestEngineDriverStatus.stopped) {
          (requiredOutputList shouldBe empty) withMessage (f"output:%n$outStream%s%n%nerrors:%n$errStream%s%n%nEngine terminated after $step%,d steps, but is missing required output: ")
          (step should be >= minSteps) withMessage (f"output:%n$outStream%s%n%nerrors:%n$errStream%s%n%nEngine terminated after $step%,d steps: ")
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
        val durationSecondsCP = (System.nanoTime() - startTime) / 1e9

        cp {
          //test if no unexpected error has occurred
          var errors: Iterator[String] = outputErr.linesIterator
          (errors.toSeq shouldBe empty) withMessage (f"log:%n$outputLog%s%n%noutput:%n$outputOut%s%n%nEngine had an error after $step%,d steps ($durationSecondsCP%,1.2f seconds): ")
        }

        if (failOnWarning) cp {
          //test if no unexpected warning has occurred
          var warnings = outputLog.linesIterator.filter(_.contains("WARN"))
          warnings = warnings.filterNot(msg => msg.contains("The update was not successful so it might not be added to the universe."))
          warnings = warnings.filterNot(msg => msg.contains("org.coreasm.util.Tools") && msg.toLowerCase.contains("root folder"))
          (warnings.toSeq shouldBe empty) withMessage (f"output:%n$outputOut%s%n%nEngine had an warning after $step%,d steps ($durationSecondsCP%,1.2f seconds): ")
        }

        for (refusedOutput <- refusedOutputList) {
          cp {
            outputOut.linesIterator.filter(_.contains(refusedOutput)).toSeq shouldBe empty withMessage (f"output of step $step%,d ($durationSecondsCP%,1.2f seconds):%n$outputOut%s")
          }
        }
        cp.reportAll()


        requiredOutputList = requiredOutputList.filterNot { x => outputOut.contains(x) }

        outStream.reset()
        logStream.reset()
        errStream.reset()
      }

      val durationSecondsTotal = (System.nanoTime() - startTime) / 1e9

      //check if no required output is missing
      (requiredOutputList shouldBe empty) withMessage (f"$outStream%s%n%nremaining required output after $maxSteps%,d maxSteps ($durationSecondsTotal%,1.2f seconds): ")
    }
    finally {
      td.stop()
    }
  }

}
