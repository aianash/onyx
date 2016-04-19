package actors

import java.io.File

import akka.actor.{Actor, ActorLogging, Props, ActorRef}

import javax.inject._

import com.google.inject.assistedinject.Assisted

sealed trait GlimpseTaggerMessage
case class GetImage(id: Int) extends GlimpseTaggerMessage
case class TagImage(id: Int, csv: Seq[Int]) extends GlimpseTaggerMessage


class GlimpseTagger(imgDir: String) extends Actor with ActorLogging {

  val dir = new File(imgDir)
  val files = dir.listFiles.filter(_.isFile).toList

  def receive = {
    case GetImage(id) =>
      val file = files(id)
      sender() ! file.getName

    case TagImage(id, csv) =>

  }

}


object GlimpseTagger {

  def props(imgDir: String) = Props(classOf[GlimpseTagger], imgDir)

}