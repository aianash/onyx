package actors

import java.io._

import akka.actor.{Actor, ActorLogging, Props}


sealed trait IntentTaggerProtocol
case class TagIntent(id: String, intents: Seq[String]) extends IntentTaggerProtocol


class IntentTagger(outfile: String) extends Actor with ActorLogging {

  def receive = {
    case TagIntent(id: String, intents: Seq[String]) => addOrUpdate(id, intents)
  }

  private def addOrUpdate(id: String, intents: Seq[String]) {
    val tmpfile = File.createTempFile("tmp", "")
    val br = new BufferedReader(new FileReader(outfile))
    val bw = new BufferedWriter(new FileWriter(tmpfile))
    var line = br.readLine
    while(line != null) {
      if(line.split(",").head != id) bw.write(line + "\n")
      line = br.readLine
    }
    intents.foreach { intent =>
      bw.write(id + "," + intent + "\n")
    }
    br.close()
    bw.close()

    val oldfile = new File(outfile)
    if(oldfile.delete()) tmpfile.renameTo(oldfile)
  }

}


object IntentTagger {

  def props(outfile: String) = Props(classOf[IntentTagger], outfile)

}