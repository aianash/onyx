package onyx
package datasets
package generators

import org.rogach.scallop._

import core.repl._
import creed.query._, models._, datasets._
import creed.core.nlp.NLP


/** Starts RECL loop for adding activities, look and time/weather;
  * and for assigning relevancy to alts (chosen randomly)
  */
object IntentDatasetGenerator {

  class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {

    def lemmatizeVerbs(x: String) = {
      val posToLammetize = Seq("VB", "VBD", "VBG", "VBN", "VBP", "VPZ")
      NLP.lemmatize(x, posToLammetize, true).mkString(" ")
    }

    val datasetfile = opt[String]("datasetfile", descr = "source dataset file ", required = true)
    val recl = opt[String]("add", descr = "what task to perform", required = true) map {
      case "activity" | "activities" | "act" | "a" => AddIntentRECL("activity", x => Activity(lemmatizeVerbs(x)))
      case "look" | "looks" | "l"                  => AddIntentRECL("look", x => Look(lemmatizeVerbs(x)))
      case "weather" | "time" | "t" | "tw"         => AddIntentRECL("time/weather", x => TimeWeather(lemmatizeVerbs(x)))
      case "alt"                                   => ALTRelevanceRECL
      case _                                       => RECL.JustExit[IntentDataset]("Unidentified task to perform. Exiting")
    }

  }

  def main(args: Array[String]) {
    val conf = new Conf(args)
    val dataset = IntentDataset(conf.datasetfile())
    conf.recl().start(dataset)
  }


  /** RECL for adding intents like activites, look and time/weather to intent dataset
    *
    * @param name - name of the intent for command like prompt
    * @param instantiate - how to instantiate intent using input from command like
    */
  case class AddIntentRECL[I <: Intent[I]](name: String, instantiate: String => I) extends RECL[IntentDataset, I, Char, I] {
    val source  = RECL.ConsoleReadLineSource[I, IntentDataset](s"$shellPrompt add $name", instantiate)

    val eval = new RECL.Eval[IntentDataset, I, I] {
      def apply(dataset: IntentDataset) = (intent: I) => {
        val options = dataset.findSimilar(intent, 1)
        if(options.isEmpty || options.head.equals(intent)) None
        else {
          val newIntent = options.head.asInstanceOf[I]
          val ask = s"Is [${intent.toString}] similar to [${newIntent.toString}] ?"
          Some(ask -> newIntent)
        }
      }
    }

    val confirm = RECL.ynConfirm

    val sink = new RECL.Sink[IntentDataset, I, Char, I] {
      def apply(dataset: IntentDataset) = {
        case Left((_, intent, 'y')) => dataset += intent
        case Right(intent) => dataset += intent
        case _ =>
      }
    }
  }


  /** RECL for assigning relevance to ALTs chosen randomly from dataset
    */
  case object ALTRelevanceRECL extends RECL[IntentDataset, ALT, Char, ALT] {

    val source  = new RECL.Source[ALT, IntentDataset] {
      def apply(dataset: IntentDataset) = dataset.generateRandomALT
    }

    val eval = new RECL.Eval[IntentDataset, ALT, ALT] {
      def apply(dataset: IntentDataset) = (alt : ALT) =>
        Some(s"How relevant is to look ${alt.look.value.toUpperCase} for ${alt.activity.value.toUpperCase} on a ${alt.timeWeather.value.toUpperCase} time ?" -> alt)
    }

    val confirm = RECL.one23Confirm

    val sink = new RECL.Sink[IntentDataset, ALT, Char, ALT] {
      def apply(dataset: IntentDataset) = {
        case Left((_, alt, choice)) => dataset += (alt -> relevance(choice))
        case _ =>
      }
    }

    private def relevance(choice: Char) = choice match {
      case '1' => 0.1f
      case '2' => 0.5f
      case '3' => 0.9f
      case _   => 0.0f
    }
  }

}