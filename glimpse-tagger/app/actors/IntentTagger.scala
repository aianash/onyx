package actors

import scala.io.Source
import scala.collection.mutable.HashMap

import java.io._
import java.nio.file.{Paths, Files}

import akka.actor.{Actor, ActorLogging, Props}


sealed trait IntentTaggerProtocol
case class TagIntent(id: String, intents: String) extends IntentTaggerProtocol


class IntentTagger(outfile: String) extends Actor with ActorLogging {

  val intenttags: HashMap[(String, String), Int] = readIntentTagsToMap(outfile)

  def receive = {
    case TagIntent(id, intent) => addOrUpdate(id, intent)
  }

  private def addOrUpdate(id: String, intent: String) {
    if(intenttags.contains((id, intent))) intenttags((id, intent)) += 1
    else intenttags += (((id, intent), 1))

    val oldfile = new File(outfile)
    val tmpfile = File.createTempFile("tmp", "", new File(oldfile.getParent))
    val bw      = new BufferedWriter(new FileWriter(tmpfile))

    intenttags foreach { t2 =>
      bw.write(t2._1._1 + "," + t2._1._2 + "," + t2._2 + "\n")
    }

    bw.close
    if(!oldfile.exists() || oldfile.delete()) tmpfile.renameTo(oldfile)
  }

  private def readIntentTagsToMap(file: String) =
    if(!Files.exists(Paths.get(file)))
      HashMap.empty[(String, String), Int]
    else
      Source.fromFile(file).getLines.toList.foldLeft(HashMap.empty[(String, String), Int])((map, line) => {
        val els = line.split(",")
        map += (((els(0), els(1)), els(2).toInt))
        map
      })

}


object IntentTagger {

  def props(outfile: String) = Props(classOf[IntentTagger], outfile)

}