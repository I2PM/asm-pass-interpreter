package de.athalis.pass.processmodel.interface.context

import de.athalis.pass.processmodel.operation.PASSProcessModelConverterSingle
import de.athalis.pass.processmodel.tudarmstadt.Process

import scala.collection.parallel.mutable.ParArray


trait AnalysisException extends RuntimeException {
  def message: String
  def cause: Throwable
}

case class InvalidProcessException(override val message: String = null, override val cause: Throwable = null) extends RuntimeException(message, cause) with AnalysisException


trait AnalysisResult
object AnalysisDone extends AnalysisResult

trait Analysis[B <: AnalysisResult] {
  @throws(classOf[AnalysisException])
  def analyze(): B
}

trait Transformation {
  def transform(process: Process, analysisResults: Seq[AnalysisResult]): Process
}

object Analysis extends PASSProcessModelConverterSingle[Process, Process] {
  private def processAnalysis: java.util.function.Function[Process, (Process, Seq[AnalysisResult])] = (from) => {
    new StructuralSoundnessAnalysis(from).analyze() // not included in `analyses` as minimal structural soundness is always required

    val analyses: Seq[Analysis[_ <: AnalysisResult]] = Seq[Analysis[_ <: AnalysisResult]](new ModalJoinAnalysis(from))

    val analysisResults: Seq[AnalysisResult] = ParArray.handoff(analyses.toArray).map(_.analyze()).seq.toSeq

    (from, analysisResults)
  }

  private def processTransformation: java.util.function.Function[(Process, Seq[AnalysisResult]), Process] = (from) => {
    val (process, analysisResults) = from

    val transformations: Seq[Transformation] = Seq(ModalJoinTransformation)

    // FIXME: ugly.. works for one, but how will it work with conflicts?
    transformations.foldRight(process)((t, p) => t.transform(p, analysisResults))
  }

  def processAnalysisAndTransformation(from: Process): Process = {
    processTransformation(processAnalysis(from))
  }

  override def convertSingle(from: Process): Process = processAnalysisAndTransformation(from)
}
