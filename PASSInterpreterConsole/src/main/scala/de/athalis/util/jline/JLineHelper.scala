package de.athalis.util.jline

import java.util.{List => JList}

import org.jline.reader._
import org.jline.reader.impl.completer.{NullCompleter, StringsCompleter}
import org.jline.terminal.Terminal

import scala.collection.JavaConverters._

object JLineHelper {
  def readLine(promptMessage: String = "> ", args: Seq[String] = Seq())(implicit terminal: Terminal): String = {
    val completer = if (args.isEmpty) {
      NullCompleter.INSTANCE
    }
    else {
      new StringsCompleter(args.asJava)
    }

    val lineReader = LineReaderBuilder
      .builder()
      .terminal(terminal)
      .completer(completer)
      .build()

    readLine(promptMessage, lineReader)
  }

  def readLine(promptMessage: String, reader: LineReader): String = {
    Thread.`yield`() // give other threads a chance to print before we will print our prompt and wait for the user
    Console.out.flush()

    try {
      reader.readLine(promptMessage)
    }
    catch {
      case e: UserInterruptException => null // ^C
      case e: EndOfFileException => null // ^D
    }
  }
}

class DynamicStringsCompleter(fun: () => Seq[String]) extends Completer {
  override def complete(reader: LineReader, line: ParsedLine, candidates: JList[Candidate]): Unit = {
    val values: Seq[String] = fun()
    val completer = if (values.isEmpty) {
      NullCompleter.INSTANCE
    }
    else {
      new StringsCompleter(values.asJava)
    }
    completer.complete(reader, line, candidates)
  }
}
