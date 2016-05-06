package actors

import scala.io.Source
import scala.collection.mutable.HashMap

import java.io._
import java.nio.file.{Paths, Files}

import akka.actor.{Actor, ActorLogging, Props}


sealed trait IntentTaggerProtocol
case class TagIntent(id: String, intents: Seq[String]) extends IntentTaggerProtocol
case class GetIntent(id: String) extends IntentTaggerProtocol


class IntentTagger(outfile: String) extends Actor with ActorLogging {

  val intenttags = readIntentTagsToMap(outfile)

  def receive = {
    case TagIntent(id, intents) => addOrUpdate(id, intents)

    case GetIntent(id) =>
      val intents = intenttags.getOrElse(id, Seq.empty[String])
      sender() ! intents
  }

  private def addOrUpdate(id: String, intents: Seq[String]) {
    intenttags += ((id, intents))

    val oldfile = new File(outfile)
    val tmpfile = File.createTempFile("tmp", "", new File(oldfile.getParent))
    val bw      = new BufferedWriter(new FileWriter(tmpfile))

    intenttags foreach { t2 =>
      t2._2 foreach { intent =>
        bw.write(t2._1 + "," + intent + "\n")
      }
    }

    bw.close
    if(!oldfile.exists() || oldfile.delete()) tmpfile.renameTo(oldfile)
  }

  private def readIntentTagsToMap(file: String) =
    if(!Files.exists(Paths.get(file)))
      HashMap.empty[String, Seq[String]]
    else
      Source.fromFile(file).getLines.toList.foldLeft(HashMap.empty[String, Seq[String]])((map, line) => {
        val els = line.split(",")
        if(map.contains(els.head)) map += ((els.head, els.tail.mkString +: map.get(els.head).get))
        else map += ((els.head, els.tail.toList))
        map
      })

}


object IntentTagger {

  def props(outfile: String) = Props(classOf[IntentTagger], outfile)

}