package actors

import java.io._

import akka.actor.{Actor, ActorLogging, Props, ActorRef}

import javax.inject._

import com.google.inject.assistedinject.Assisted

sealed trait GlimpseTaggerMessage
case class GetImage(id: Int) extends GlimpseTaggerMessage
case class TagImage(id: Int, csv: Seq[Int]) extends GlimpseTaggerMessage


/**
 * GlimpseTagger is a class responsible for storing information whether a glimpse
 * is present or absent in an image. Glimpses are loaded from a config file.
 */
class GlimpseTagger(imgDir: String, tagsFile: String) extends Actor with ActorLogging {

  val dir   = new File(imgDir)
  val pw    = new PrintWriter(new FileWriter(tagsFile, true))
  val files = dir.listFiles.filter(_.isFile).toList

  def receive = {
    case GetImage(id) =>
      val file = files(id)
      sender() ! file.getName

    case TagImage(id, tags) =>
      val file = files(id)
      pw.append(file.getName + "," + tags.mkString(",") + "\n")
      pw.flush()
  }

  override def postStop = pw.close()

}


object GlimpseTagger {

  def props(imgDir: String, tagsFile: String) = Props(classOf[GlimpseTagger], imgDir, tagsFile)

}