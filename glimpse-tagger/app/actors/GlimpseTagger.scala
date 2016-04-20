package actors

import java.io._

import akka.actor.{Actor, ActorLogging, Props, ActorRef}

import javax.inject._

import com.google.inject.assistedinject.Assisted

sealed trait GlimpseTaggerMessage
case class GetImage(id: Int) extends GlimpseTaggerMessage
case class TagImage(id: Int, glimpses: Seq[Int], intents: Seq[String]) extends GlimpseTaggerMessage


/**
 * GlimpseTagger is a class responsible for storing information whether a glimpse
 * is present or absent in an image. Glimpses are loaded from a config file.
 */
class GlimpseTagger(imgDir: String, tagsOutFile: String, intentOutFile: String) extends Actor with ActorLogging {

  val dir   = new File(imgDir)
  val pwTags    = new PrintWriter(new FileWriter(tagsOutFile, true))
  val pwIntents = new PrintWriter(new FileWriter(intentOutFile, true))
  val files = dir.listFiles.filter(_.isFile).toList

  def receive = {
    case GetImage(id) =>
      val file = files(id)
      sender() ! file.getName

    case TagImage(id, glimpses, intents) =>
      val file = files(id)
      pwTags.append(file.getName + "," + glimpses.mkString(",") + "\n")
      pwTags.flush()
      pwIntents.append(file.getName + "," + intents.mkString(",") + "\n")
      pwIntents.flush()
  }

  override def postStop = {
    pwTags.close()
    pwIntents.close()
  }

}


object GlimpseTagger {

  def props(imgDir: String, tagsOutFile: String, intentOutFile: String) = Props(classOf[GlimpseTagger], imgDir, tagsOutFile, intentOutFile)

}