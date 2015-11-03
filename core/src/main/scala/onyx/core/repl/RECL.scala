package onyx
package core
package repl

import jline.console.ConsoleReader


/** Read Eval Confirm Loop
  * [NOTE] Soon to be part for Commons
  */
trait RECL[Context, From, Choice, To] {
  import RECL._

  protected implicit val reader = new ConsoleReader

  def shellPrompt: String = ">>"
  def source: Source[From, Context]
  def eval: Eval[Context, From, To]
  def confirm: Confirm[Choice]
  def sink: Sink[Context, From, Choice, To]

  def start(context: Context): Unit = {
    val eval = this.eval(context)
    val sink = this.sink(context)
    source(context).map { from =>
      eval(from) match {
        case Some((confirmPrompt, to)) => Left((from, to, runConfirmationLoop(confirmPrompt, confirm)))
        case None => Right(from)
      }
    } foreach(sink(_))
  }

  private def runConfirmationLoop(promptStr: Prompt, confirm: Confirm[Choice]): Choice = {
    var repeat = true
    var choice = null.asInstanceOf[Choice]
    while(repeat) {
      confirm(s"$shellPrompt $promptStr") match {
        case OK(ch) =>
          repeat = false
          choice = ch
        case ASKAGAIN() => repeat = true
      }
    }
    return choice
  }

}

object RECL {

  type Prompt = String

  case class JustExit[IgnoredContext](msg: String) extends RECL[IgnoredContext, Nothing, Nothing, Nothing] {

    val source  = EmptySource[Nothing, IgnoredContext]()
    val eval    = EvalNothing[IgnoredContext, Nothing, Nothing]()
    val confirm = Confirm.AlwaysOK[Nothing]
    val sink    = SinkNothing[IgnoredContext, Nothing, Nothing, Nothing]()

    override def start(ignored: IgnoredContext): Unit = println(msg)
  }

  //////////////////////////// Source //////////////////////////////

  trait Source[From, Context] {
    def apply(context: Context): Iterator[From]
  }

  case class EmptySource[From, Context]() extends Source[From, Context] {
    def apply(context: Context) = Iterator.empty
  }

  case class ConsoleReadLineSource[From, Context](prompt: String, instantiate: String => From, delimiter: String = ",")(implicit reader: ConsoleReader) extends Source[From, Context] {
    def apply(context: Context) =
      Iterator.continually(reader.readLine(s"$prompt : "))
        .takeWhile(i => !("quit".equals(i) || "exit".equals(i)))
        .flatMap(_.split(delimiter).map(_.trim).filter(!_.isEmpty).toList)
        .map(instantiate(_))
  }

  //////////////////////////// Eval //////////////////////////////

  trait Eval[Context, From, To] {
    def apply(context: Context): From => Option[(Prompt, To)]
  }

  case class EvalNothing[Context, From, To]() extends Eval[Context, From, To] {
    def apply(context: Context) = { _: From => None }
  }

  //////////////////////////// Confirm //////////////////////////////

  sealed trait CNF[Choice]
  case class ASKAGAIN[Choice]() extends CNF[Choice]
  case class OK[Choice](choice: Choice) extends CNF[Choice]

  trait Confirm[Choice] {
    def apply(promptStr: String)(implicit reader: ConsoleReader): CNF[Choice]
  }

  object Confirm {
    case class AlwaysOK[Choice]() extends Confirm[Choice] {
      def apply(any: String)(implicit reader: ConsoleReader) = OK(null.asInstanceOf[Choice])
    }
  }

  abstract class CharConfirm(val optionStr: String) extends Confirm[Char] {
    def apply(promptStr: String)(implicit reader: ConsoleReader) = {
      print(s"${promptStr} ${optionStr} ")
      val input = reader.readCharacter.toChar
      println("")
      test(input)
    }

    def test(selection: Char): CNF[Char]
  }

  object ynConfirm extends CharConfirm("(y/n)") {
    def test(yn: Char) = yn match {
      case 'y' | 'Y' => OK('y')
      case 'n' | 'N' => OK('n')
      case _   =>  ASKAGAIN[Char]
    }
  }

  object one23Confirm extends CharConfirm("(1/2/3)") {
    def test(selection: Char) = selection match {
      case '1' => OK('1')
      case '2' => OK('2')
      case '3' => OK('3')
      case _ => ASKAGAIN[Char]
    }
  }

  object sevenStarConfirm extends CharConfirm("(1/2/3/4/5/6/7)") {
    def test(selection: Char) = selection match {
      case '1' => OK('1')
      case '2' => OK('2')
      case '3' => OK('3')
      case '4' => OK('4')
      case '5' => OK('5')
      case '6' => OK('6')
      case '7' => OK('7')
      case _ => ASKAGAIN[Char]
    }
  }

  //////////////////////////// Sink //////////////////////////////

  trait Sink[Context, From, Choice, To] {
    def apply(context: Context): Either[(From, To, Choice), From] => Unit
  }

  case class SinkNothing[Context, From, Choice, To]() extends Sink[Context, From, Choice, To] {
    def apply(context: Context) = { _: Either[(From, To, Choice), From] => }
  }
}
