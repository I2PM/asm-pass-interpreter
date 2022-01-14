package de.athalis.pass.ui.loading

import de.athalis.coreasm.binding.Binding

import de.athalis.pass.processmodel.tudarmstadt.Types.VariableIdentifier
import de.athalis.pass.semantic.Semantic
import de.athalis.pass.ui.definitions.ActiveStateF

import de.athalis.util.matching.Implicits._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.matching.Regex

object VarLoader {
  val pattern = new Regex("""(?<!\\)\$[\w-_]+""")

  def apply(loader: (VariableIdentifier => Future[String]), text: Future[String])(implicit ctx: scala.concurrent.ExecutionContext): Future[String] = {
    text.flatMap(
      pattern.replaceAllInConcurrent(_, y => loader(y.toString().substring(1)) )
    ).map(_.replace("""\$""", """$"""))
  }


  private def varLoaderImpl(stateF: ActiveStateF)(implicit binding: Binding, executionContext: ExecutionContext): (VariableIdentifier => Future[String]) = {
    (varname: VariableIdentifier) => Semantic.LoadVarForChannel(stateF.ch, stateF.MI, varname).loadAndGetAsync().map(_.toString)
  }

  def transformLabel(state: ActiveStateF, label: Future[Option[String]])(implicit binding: Binding, executionContext: ExecutionContext): Future[String] = {
    VarLoader(varLoaderImpl(state), label.map(_.get))
  }
}
