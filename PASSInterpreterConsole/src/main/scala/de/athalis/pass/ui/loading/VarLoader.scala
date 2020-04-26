package de.athalis.pass.ui.loading

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

import de.athalis.coreasm.binding.Binding
import de.athalis.pass.semantic.Semantic

import de.athalis.pass.ui.definitions.ActiveStateF
import de.athalis.util.matching.Implicits._

object VarLoader {
  val pattern = new Regex("""(?<!\\)\$[\w-_]+""")

  def apply(loader: (String => Future[String]), text: Future[String])(implicit ctx: scala.concurrent.ExecutionContext): Future[String] = {
    text.flatMap(
      pattern.replaceAllInConcurrent(_, y => loader(y.toString().substring(1)) )
    ).map(_.replace("""\$""", """$"""))
  }


  private def varLoaderImpl(stateF: ActiveStateF)(implicit binding: Binding, executionContext: ExecutionContext): (String => Future[String]) = {
    (varname: String) => Semantic.LoadVarForChannel(stateF.ch, stateF.MI, varname).loadAndGetAsync().map(_.toString)
  }

  def transformLabel(state: ActiveStateF, label: Future[Option[String]])(implicit binding: Binding, executionContext: ExecutionContext): Future[String] = {
    VarLoader(varLoaderImpl(state), label.map(_.get))
  }
}
