package onyx
package datasets
package generators

import org.rogach.scallop._

import core.repl._
import core.datasets.alt._


/** Starts RECL loop for adding activities, look and time/weather;
  * and for assigning relevancy to alts (chosen randomly)
  */
object IntentDatasetGenerator {

  class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
    val datasetfile = opt[String]("datasetfile", descr = "source dataset file ", required = true)
    val recl = opt[String]("add", descr = "what task to perform", required = true) map {
      case "activity" | "activities" | "act" | "a" => AddIntentRECL("activity", Activity(_))
      case "look" | "looks" | "l"                  => AddIntentRECL("look", Look(_))
      case "weather" | "time" | "t"                => AddIntentRECL("time/weather", TimeWeather(_))
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
  case class AddIntentRECL[I <: Intent](name: String, instantiate: String => I) extends RECL[IntentDataset, I, Char, I] {
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
      def apply(dataset: IntentDataset) = (alt : ALT) => Some(s"How relevant is ALT = [$alt] ?" -> alt)
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