package actors

import java.io.{FileWriter, PrintWriter}

import akka.actor.{Actor, ActorLogging, Props}


sealed trait IntentTaggerProtocol
case class TagIntent(id: String, intents: Seq[String]) extends IntentTaggerProtocol


class IntentTagger(outfile: String) extends Actor with ActorLogging {

  val pw = new PrintWriter(new FileWriter(outfile, true))

  def receive = {
    case TagIntent(id: String, intents: Seq[String]) =>
      intents foreach { intent =>
        pw.append(id + "," + intent + "\n")
        pw.flush()
      }
  }

  override def postStop = pw.close()

}


object IntentTagger {

  def props(outfile: String) = Props(classOf[IntentTagger], outfile)

}